from __future__ import annotations

import json
import re
import argparse
import asyncio
from datetime import datetime, timezone
from pathlib import Path
from typing import Any, Awaitable, Callable

import aiohttp
from bs4 import BeautifulSoup


FIELD_MAP = {
    "title": "name",
    "brandName": "brand",
    "categoryName": "category",
    "description": "description",
    "imageUrls": "images",
    "volume": "volume",
    "productType": "product_type",
    "gender": "for_whom",
    "purpose": "purpose",
    "skinType": "skin_type",
    "applicationTime": "application_time",
    "area": "area",
    "activeIngredient": "active_ingredient",
    "ingredients": "composition",
    "usage": "application_info",
    "country": "country",
}

SPEC_LABELS = {
    "объем": "volume",
    "объём": "volume",
    "тип продукта": "productType",
    "тип товара": "productType",
    "для кого": "gender",
    "назначение": "purpose",
    "тип кожи": "skinType",
    "время применения": "applicationTime",
    "область применения": "area",
    "зона применения": "area",
    "активный ингредиент": "activeIngredient",
    "активные ингредиенты": "activeIngredient",
    "состав": "ingredients",
    "ингредиенты": "ingredients",
    "способ применения": "usage",
    "применение": "usage",
    "страна": "country",
    "страна производства": "country",
}

BLOCK_PATTERNS = (
    ("captcha", re.compile(r"captcha|капча|подтвердите,? что вы не робот|i'?m not a robot", re.I)),
    ("access_denied", re.compile(r"access denied|доступ запрещен|forbidden|temporarily blocked", re.I)),
)

Fetcher = Callable[[str], Awaitable[str] | str]


def normalize_product_record(source_site: str, source_url: str, payload: dict[str, Any]) -> dict[str, Any]:
    record: dict[str, Any] = {
        "status": "ok",
        "source_site": source_site,
        "source_url": source_url,
        "scraped_at": datetime.now(timezone.utc).isoformat(),
    }

    for source_key, target_key in FIELD_MAP.items():
        value = payload.get(source_key)
        if value is not None:
            record[target_key] = value

    price = _parse_rub_amount(payload.get("price"))
    old_price = _parse_rub_amount(payload.get("oldPrice"))
    rating = _parse_float(payload.get("rating"))
    reviews_count = _parse_int(payload.get("reviewsCount"))

    if price is not None:
        record["price"] = price
        record["currency"] = "RUB"
    if old_price is not None:
        record["old_price"] = old_price
        record.setdefault("currency", "RUB")
    if rating is not None:
        record["rating"] = rating
    if reviews_count is not None:
        record["reviews_count"] = reviews_count

    return record


def build_blocked_record(source_site: str, source_url: str, block_reason: str) -> dict[str, Any]:
    return {
        "status": "blocked",
        "source_site": source_site,
        "source_url": source_url,
        "block_reason": block_reason,
        "scraped_at": datetime.now(timezone.utc).isoformat(),
    }


def write_products_json(path: str | Path, records: list[dict[str, Any]]) -> None:
    output_path = Path(path)
    output_path.parent.mkdir(parents=True, exist_ok=True)
    output_path.write_text(
        json.dumps(records, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )


def extract_product_payload(html: str) -> dict[str, Any]:
    soup = BeautifulSoup(html, "html.parser")
    payload: dict[str, Any] = {}

    product = _find_json_ld_product(soup)
    if product:
        _merge_json_ld_product(payload, product)

    payload.setdefault("title", _meta_content(soup, "og:title") or _text_or_none(soup.find("h1")))
    payload.setdefault("description", _meta_content(soup, "description") or _meta_content(soup, "og:description"))
    image = _meta_content(soup, "og:image")
    if image and not payload.get("imageUrls"):
        payload["imageUrls"] = [image]

    _merge_specs(payload, soup)
    return {key: value for key, value in payload.items() if value not in (None, "", [])}


def detect_block_reason(html: str) -> str | None:
    for reason, pattern in BLOCK_PATTERNS:
        if pattern.search(html):
            return reason
    return None


def load_url_list(path: str | Path) -> list[str]:
    lines = Path(path).read_text(encoding="utf-8").splitlines()
    return [line.strip() for line in lines if line.strip() and not line.strip().startswith("#")]


def collect_products(
    urls: list[str],
    output_path: str | Path,
    *,
    fetcher: Fetcher | None = None,
    delay_seconds: float = 2.0,
) -> list[dict[str, Any]]:
    records = asyncio.run(_collect_products_async(urls, fetcher=fetcher, delay_seconds=delay_seconds))
    write_products_json(output_path, records)
    return records


async def _collect_products_async(
    urls: list[str],
    *,
    fetcher: Fetcher | None,
    delay_seconds: float,
) -> list[dict[str, Any]]:
    records: list[dict[str, Any]] = []
    actual_fetcher = fetcher or fetch_html
    for index, url in enumerate(urls):
        if index and delay_seconds > 0:
            await asyncio.sleep(delay_seconds)

        source_site = detect_source_site(url)
        try:
            html = await _maybe_await(actual_fetcher(url))
        except Exception as exc:  # Network errors are data quality, not a reason to stop the batch.
            records.append(build_blocked_record(source_site, url, f"fetch_error:{type(exc).__name__}"))
            continue

        block_reason = detect_block_reason(html)
        if block_reason:
            records.append(build_blocked_record(source_site, url, block_reason))
            continue

        payload = extract_product_payload(html)
        if not payload.get("title"):
            records.append(build_blocked_record(source_site, url, "no_product_data"))
            continue
        records.append(normalize_product_record(source_site, url, payload))
    return records


async def fetch_html(url: str) -> str:
    headers = {
        "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
        "Accept-Language": "ru-RU,ru;q=0.9,en;q=0.8",
        "User-Agent": "AuraResearchCollector/1.0 (+local academic dataset; contact: local)",
    }
    timeout = aiohttp.ClientTimeout(total=30)
    async with aiohttp.ClientSession(headers=headers, timeout=timeout) as session:
        async with session.get(url, allow_redirects=True) as response:
            if response.status in (401, 403, 429):
                return f"access denied status {response.status}"
            response.raise_for_status()
            return await response.text()


def detect_source_site(url: str) -> str:
    host = re.sub(r"^www\.", "", re.sub(r"^https?://", "", url).split("/")[0].lower())
    if "goldapple" in host:
        return "goldapple"
    if "letu" in host:
        return "letu"
    if "rivegauche" in host:
        return "rivegauche"
    if "podrygka" in host:
        return "podrygka"
    if "ozon" in host:
        return "ozon"
    if "wildberries" in host or "wb.ru" in host:
        return "wildberries"
    return host or "unknown"


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Collect public cosmetics product pages into JSON.")
    parser.add_argument("--urls", required=True, help="UTF-8 text file with one product URL per line.")
    parser.add_argument("--output", required=True, help="Output JSON file path.")
    parser.add_argument("--delay", type=float, default=2.0, help="Delay between requests in seconds.")
    args = parser.parse_args(argv)

    urls = load_url_list(args.urls)
    collect_products(urls, args.output, delay_seconds=args.delay)
    return 0


def _parse_rub_amount(value: Any) -> int | None:
    if value is None:
        return None
    digits = re.sub(r"\D", "", str(value))
    return int(digits) if digits else None


def _parse_float(value: Any) -> float | None:
    if value is None or value == "":
        return None
    match = re.search(r"\d+(?:[,.]\d+)?", str(value))
    return float(match.group(0).replace(",", ".")) if match else None


def _parse_int(value: Any) -> int | None:
    if value is None:
        return None
    digits = re.sub(r"\D", "", str(value))
    return int(digits) if digits else None


def _find_json_ld_product(soup: BeautifulSoup) -> dict[str, Any] | None:
    for script in soup.find_all("script", {"type": "application/ld+json"}):
        raw = script.string or script.get_text(strip=True)
        if not raw:
            continue
        try:
            data = json.loads(raw)
        except json.JSONDecodeError:
            continue
        product = _find_product_node(data)
        if product:
            return product
    return None


def _find_product_node(data: Any) -> dict[str, Any] | None:
    if isinstance(data, dict):
        node_type = data.get("@type")
        if node_type == "Product" or (isinstance(node_type, list) and "Product" in node_type):
            return data
        graph = data.get("@graph")
        if graph:
            return _find_product_node(graph)
    if isinstance(data, list):
        for item in data:
            product = _find_product_node(item)
            if product:
                return product
    return None


def _merge_json_ld_product(payload: dict[str, Any], product: dict[str, Any]) -> None:
    payload["title"] = product.get("name")
    payload["description"] = product.get("description")

    brand = product.get("brand")
    if isinstance(brand, dict):
        payload["brandName"] = brand.get("name")
    elif isinstance(brand, str):
        payload["brandName"] = brand

    image = product.get("image")
    if isinstance(image, list):
        payload["imageUrls"] = image
    elif isinstance(image, str):
        payload["imageUrls"] = [image]

    offers = product.get("offers")
    if isinstance(offers, list):
        offers = offers[0] if offers else None
    if isinstance(offers, dict):
        payload["price"] = offers.get("price") or offers.get("lowPrice")

    rating = product.get("aggregateRating")
    if isinstance(rating, dict):
        payload["rating"] = rating.get("ratingValue")
        payload["reviewsCount"] = rating.get("reviewCount") or rating.get("ratingCount")


def _merge_specs(payload: dict[str, Any], soup: BeautifulSoup) -> None:
    for dt in soup.find_all("dt"):
        dd = dt.find_next_sibling("dd")
        if dd:
            _set_spec_value(payload, dt.get_text(" ", strip=True), dd.get_text(" ", strip=True))

    for row in soup.find_all(["tr", "li", "div"]):
        text = row.get_text(" ", strip=True)
        if ":" not in text or len(text) > 500:
            continue
        label, value = text.split(":", 1)
        _set_spec_value(payload, label, value)


def _set_spec_value(payload: dict[str, Any], label: str, value: str) -> None:
    normalized_label = re.sub(r"\s+", " ", label).strip().lower()
    target = SPEC_LABELS.get(normalized_label)
    value = value.strip()
    if target and value and target not in payload:
        payload[target] = _split_list_value(value) if target in {"purpose", "skinType"} else value


def _split_list_value(value: str) -> list[str] | str:
    parts = [part.strip() for part in re.split(r"[,;]", value) if part.strip()]
    return parts if len(parts) > 1 else value


def _meta_content(soup: BeautifulSoup, name: str) -> str | None:
    tag = soup.find("meta", attrs={"property": name}) or soup.find("meta", attrs={"name": name})
    return tag.get("content") if tag and tag.get("content") else None


def _text_or_none(node: Any) -> str | None:
    return node.get_text(" ", strip=True) if node else None


async def _maybe_await(value: Awaitable[str] | str) -> str:
    if asyncio.iscoroutine(value) or isinstance(value, Awaitable):
        return await value
    return value


if __name__ == "__main__":
    raise SystemExit(main())
