import json

from backend.cosmetics_collection import (
    build_blocked_record,
    collect_products,
    detect_block_reason,
    extract_product_payload,
    load_url_list,
    normalize_product_record,
    write_products_json,
)


def test_normalize_product_record_maps_site_payload_to_database_json_fields():
    payload = {
        "title": "Mineral 89",
        "brandName": "Vichy",
        "categoryName": "Уход за лицом",
        "description": "Увлажняющий крем для ежедневного ухода.",
        "imageUrls": ["https://example.test/image.jpg"],
        "volume": "50 мл",
        "productType": "Крем для лица",
        "gender": "Универсальный",
        "purpose": ["Увлажнение", "Питание"],
        "skinType": ["Для всех типов кожи"],
        "applicationTime": "Утро и вечер",
        "area": "Лицо",
        "activeIngredient": "Niacinamide",
        "ingredients": "Aqua, Glycerin, Niacinamide",
        "usage": "Нанести на очищенную кожу.",
        "country": "Франция",
        "price": "2499 ₽",
        "oldPrice": "2999 ₽",
        "rating": "4.8",
        "reviewsCount": "123 отзыва",
    }

    record = normalize_product_record("goldapple", "https://goldapple.ru/product/1", payload)

    assert record["status"] == "ok"
    assert record["source_site"] == "goldapple"
    assert record["source_url"] == "https://goldapple.ru/product/1"
    assert record["name"] == "Mineral 89"
    assert record["brand"] == "Vichy"
    assert record["category"] == "Уход за лицом"
    assert record["images"] == ["https://example.test/image.jpg"]
    assert record["volume"] == "50 мл"
    assert record["product_type"] == "Крем для лица"
    assert record["for_whom"] == "Универсальный"
    assert record["purpose"] == ["Увлажнение", "Питание"]
    assert record["skin_type"] == ["Для всех типов кожи"]
    assert record["application_time"] == "Утро и вечер"
    assert record["area"] == "Лицо"
    assert record["active_ingredient"] == "Niacinamide"
    assert record["composition"] == "Aqua, Glycerin, Niacinamide"
    assert record["application_info"] == "Нанести на очищенную кожу."
    assert record["country"] == "Франция"
    assert record["price"] == 2499
    assert record["old_price"] == 2999
    assert record["currency"] == "RUB"
    assert record["rating"] == 4.8
    assert record["reviews_count"] == 123
    assert record["scraped_at"]


def test_build_blocked_record_preserves_source_without_bypassing_protection():
    record = build_blocked_record("letu", "https://www.letu.ru/product/1", "captcha")

    assert record["status"] == "blocked"
    assert record["source_site"] == "letu"
    assert record["source_url"] == "https://www.letu.ru/product/1"
    assert record["block_reason"] == "captcha"
    assert "name" not in record


def test_write_products_json_creates_utf8_json_array(tmp_path):
    output = tmp_path / "products.json"
    records = [
        normalize_product_record(
            "goldapple",
            "https://goldapple.ru/product/1",
            {"title": "Крем", "brandName": "Aura", "price": "100 ₽"},
        )
    ]

    write_products_json(output, records)

    saved = json.loads(output.read_text(encoding="utf-8"))
    assert saved[0]["name"] == "Крем"
    assert saved[0]["price"] == 100


def test_extract_product_payload_reads_json_ld_and_meta_fields():
    html = """
    <html>
      <head>
        <meta property="og:image" content="https://example.test/cream.jpg">
        <meta name="description" content="Описание из meta">
        <script type="application/ld+json">
        {
          "@type": "Product",
          "name": "Крем Mineral 89",
          "brand": {"name": "Vichy"},
          "description": "Описание из JSON-LD",
          "image": ["https://example.test/cream-json.jpg"],
          "offers": {"price": "2499", "priceCurrency": "RUB"},
          "aggregateRating": {"ratingValue": "4.7", "reviewCount": "77"}
        }
        </script>
      </head>
      <body>
        <dl>
          <dt>Объем</dt><dd>50 мл</dd>
          <dt>Тип продукта</dt><dd>Крем для лица</dd>
          <dt>Состав</dt><dd>Aqua, Glycerin</dd>
          <dt>Способ применения</dt><dd>Нанести утром.</dd>
        </dl>
      </body>
    </html>
    """

    payload = extract_product_payload(html)

    assert payload["title"] == "Крем Mineral 89"
    assert payload["brandName"] == "Vichy"
    assert payload["description"] == "Описание из JSON-LD"
    assert payload["imageUrls"] == ["https://example.test/cream-json.jpg"]
    assert payload["price"] == "2499"
    assert payload["rating"] == "4.7"
    assert payload["reviewsCount"] == "77"
    assert payload["volume"] == "50 мл"
    assert payload["productType"] == "Крем для лица"
    assert payload["ingredients"] == "Aqua, Glycerin"
    assert payload["usage"] == "Нанести утром."


def test_detect_block_reason_identifies_captcha_or_access_denied_pages():
    assert detect_block_reason("<html>captcha required</html>") == "captcha"
    assert detect_block_reason("<h1>Access denied</h1>") == "access_denied"
    assert detect_block_reason("<html><h1>Product</h1></html>") is None


def test_load_url_list_ignores_blank_lines_and_comments(tmp_path):
    urls = tmp_path / "urls.txt"
    urls.write_text("\n# comment\nhttps://goldapple.ru/p/1\n\nhttps://www.letu.ru/p/2\n", encoding="utf-8")

    assert load_url_list(urls) == ["https://goldapple.ru/p/1", "https://www.letu.ru/p/2"]


def test_collect_products_saves_ok_and_blocked_records(tmp_path):
    async def fetcher(url):
        if "blocked" in url:
            return "<html>captcha required</html>"
        return """
        <script type="application/ld+json">
        {"@type":"Product","name":"Крем","brand":{"name":"Aura"},"offers":{"price":"100"}}
        </script>
        """

    output = tmp_path / "products.json"

    records = collect_products(
        ["https://goldapple.ru/product/ok", "https://www.letu.ru/product/blocked"],
        output,
        fetcher=fetcher,
        delay_seconds=0,
    )

    saved = json.loads(output.read_text(encoding="utf-8"))
    assert [item["status"] for item in records] == ["ok", "blocked"]
    assert saved[0]["source_site"] == "goldapple"
    assert saved[0]["name"] == "Крем"
    assert saved[0]["brand"] == "Aura"
    assert saved[0]["price"] == 100
    assert saved[1]["source_site"] == "letu"
    assert saved[1]["block_reason"] == "captcha"
