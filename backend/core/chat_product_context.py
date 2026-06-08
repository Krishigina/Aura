import re
from typing import Any, Dict, List, Optional

from backend.core.products import normalize_product_response


PRODUCT_REFERENCE_STOP_TERMS = {
    "что",
    "это",
    "этот",
    "эта",
    "его",
    "ее",
    "её",
    "какой",
    "какая",
    "какие",
    "состав",
    "составе",
    "ингредиенты",
    "формула",
    "подходит",
    "подойдет",
    "подойдёт",
    "посоветуешь",
    "посоветуй",
    "сыворотка",
    "крем",
    "тоник",
    "средство",
    "продукт",
}


def _normalize_text(value: Any) -> str:
    return re.sub(r"\s+", " ", str(value or "")).strip().lower()


def _tokenize(value: Any) -> List[str]:
    tokens = []
    for raw in re.findall(r"[A-Za-zА-Яа-яЁё0-9-]+", _normalize_text(value)):
        token = raw.strip("-")
        if len(token) < 3 or token in PRODUCT_REFERENCE_STOP_TERMS:
            continue
        if token not in tokens:
            tokens.append(token)
    return tokens


def _build_history_reference_text(message: str, chat_history: Optional[List[Dict[str, Any]]] = None) -> str:
    parts = [str(message or "")]
    for item in reversed(chat_history or []):
        if not isinstance(item, dict):
            continue
        content = str(item.get("content") or "").strip()
        if not content:
            continue
        parts.append(content)
        if len(parts) >= 5:
            break
    return " ".join(parts)


def _candidate_score(reference_text: str, row: Any) -> float:
    product = normalize_product_response(row)
    name = str(product.get("name") or "").strip()
    brand = str(product.get("brand") or "").strip()
    if not name:
        return 0.0

    lowered_reference = _normalize_text(reference_text)
    lowered_name = _normalize_text(name)
    lowered_brand = _normalize_text(brand)
    score = 0.0

    if lowered_name and lowered_name in lowered_reference:
        score += 100.0
    if lowered_name and lowered_brand and f"{lowered_name} {lowered_brand}" in lowered_reference:
        score += 30.0
    if lowered_name and lowered_brand and f"{lowered_brand} {lowered_name}" in lowered_reference:
        score += 40.0
    if lowered_brand and lowered_brand in lowered_reference:
        score += 20.0

    reference_tokens = set(_tokenize(reference_text))
    candidate_tokens = set(_tokenize(f"{name} {brand}"))
    score += sum(12.0 for token in candidate_tokens if token in reference_tokens)

    return score


def _build_compact_product_context(row: Any) -> Dict[str, Any]:
    product = normalize_product_response(row)
    compact_product = {
        "id": int(row["id"]),
        "name": str(product.get("name") or ""),
        "brand": str(product.get("brand") or ""),
        "product_type": str(product.get("product_type") or ""),
        "what_is_it": str(product.get("what_is_it") or ""),
        "active_ingredient": str(product.get("active_ingredient") or ""),
        "composition": str(product.get("composition") or ""),
        "application_info": str(product.get("application_info") or ""),
        "purpose": product.get("purpose") or [],
        "skin_type": product.get("skin_type") or [],
    }
    return {"product": compact_product}


async def resolve_catalog_product_context(conn, *, message: str, chat_history: Optional[List[Dict[str, Any]]] = None) -> Optional[Dict[str, Any]]:
    reference_text = _build_history_reference_text(message, chat_history)
    reference_tokens = _tokenize(reference_text)
    if not reference_tokens and len(_normalize_text(message)) < 6:
        return None

    rows = await conn.fetch(
        """
        SELECT id, name, brand, product_type, what_is_it, active_ingredient, composition, application_info, purpose, skin_type
        FROM products
        WHERE COALESCE(name, '') <> ''
        ORDER BY id DESC
        LIMIT 300
        """
    )
    best_row = None
    best_score = 0.0
    for row in rows:
        score = _candidate_score(reference_text, row)
        if score > best_score:
            best_score = score
            best_row = row

    if best_row is None or best_score < 24.0:
        return None
    return _build_compact_product_context(best_row)
