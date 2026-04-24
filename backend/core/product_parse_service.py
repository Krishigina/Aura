import json
import re

import aiohttp
from bs4 import BeautifulSoup
from fastapi import HTTPException


async def parse_product_page(url: str):
    html = await fetch_product_page_html(url)
    soup = BeautifulSoup(html, "html.parser")
    result = {"name": None, "brand": None, "category": None, "description": None, "images": [], "volume": None}

    populate_product_name(result, soup)
    populate_product_brand(result, html, soup)
    populate_product_description(result, soup)
    populate_product_images(result, soup)
    populate_json_ld_product_data(result, soup)
    populate_product_volume(result, html)
    populate_product_category(result, soup)
    return result


async def fetch_product_page_html(url: str):
    headers = {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    }
    try:
        async with aiohttp.ClientSession() as session:
            async with session.get(url, headers=headers, timeout=aiohttp.ClientTimeout(total=15)) as response:
                if response.status != 200:
                    raise HTTPException(status_code=400, detail=f"Failed to fetch URL: status {response.status}")
                return await response.text()
    except Exception as error:
        raise HTTPException(status_code=400, detail=f"Error fetching URL: {str(error)}") from error


def populate_product_name(result, soup):
    og_title = soup.find("meta", property="og:title")
    if og_title:
        result["name"] = og_title.get("content", "").strip()
    if not result["name"]:
        title_tag = soup.find("title")
        if title_tag:
            result["name"] = title_tag.string.strip() if title_tag.string else None
    h1_tag = soup.find("h1")
    if h1_tag and h1_tag.string and not result["name"]:
        result["name"] = h1_tag.string.strip()


def populate_product_brand(result, html: str, soup):
    brand_meta = soup.find("meta", property="product:brand")
    if brand_meta:
        result["brand"] = brand_meta.get("content", "").strip()
    if not result["brand"]:
        for pattern, group in [
            (r"(?:Brand|Marque|Marca):\s*([^<\n]+)", 1),
            (r'"brand"\s*:\s*"([^"]+)"', 1),
            (r'<span[^>]*class="[^"]*brand[^"]*"[^>]*>([^<]+)</span>', 1),
            (r'"name"\s*:\s*"([^"]+)"', 1),
        ]:
            match = re.search(pattern, html, re.IGNORECASE)
            if match:
                result["brand"] = match.group(group).strip()
                break
    if not result["brand"]:
        for brand in [
            "La Roche-Posay", "Vichy", "Bioderma", "CeraVe", "The Ordinary",
            "Paula's Choice", "Cosrx", "Eucerin", "Nivea", "Aura", "A-Derma",
            "Uriage", "Filorga", "Nuxe", "Darphin", "Clarins", "Estee Lauder",
            "Lancome", "Shiseido", "Clinique", "Origins", "Decathlon",
        ]:
            if brand.lower() in html.lower():
                result["brand"] = brand
                break


def populate_product_description(result, soup):
    desc_meta = soup.find("meta", property="og:description")
    if desc_meta:
        result["description"] = desc_meta.get("content", "").strip()
    if not result["description"]:
        desc_tag = soup.find("meta", attrs={"name": "description"})
        if desc_tag:
            result["description"] = desc_tag.get("content", "").strip()
    if not result["description"]:
        desc_div = soup.find("div", class_=re.compile(r"description", re.I))
        if desc_div:
            result["description"] = desc_div.get_text(strip=True)[:500]


def populate_product_images(result, soup):
    for img in soup.find_all("img")[:5]:
        src = img.get("src") or img.get("data-src") or img.get("data-lazy") or img.get("data-srcset", "").split()[0]
        if src and src.startswith("http") and src not in result["images"]:
            result["images"].append(src)
    og_image = soup.find("meta", property="og:image")
    if og_image:
        img_url = og_image.get("content", "")
        if img_url and img_url not in result["images"]:
            result["images"].insert(0, img_url)


def populate_json_ld_product_data(result, soup):
    json_ld = soup.find("script", type="application/ld+json")
    if not (json_ld and json_ld.string):
        return
    try:
        data = json.loads(json_ld.string)
    except Exception:
        return
    if not isinstance(data, dict):
        return
    if not result["name"] and data.get("name"):
        result["name"] = data["name"]
    if not result["brand"] and data.get("brand"):
        brand_val = data["brand"]
        result["brand"] = brand_val if isinstance(brand_val, str) else brand_val.get("name")
    if not result["description"] and data.get("description"):
        description = data.get("description", "")
        result["description"] = description[:500] if len(description) > 500 else description
    if not result["images"] and data.get("image"):
        img_data = data["image"]
        if isinstance(img_data, list):
            result["images"] = [item for item in img_data if isinstance(item, str)][:5]
        elif isinstance(img_data, str):
            result["images"] = [img_data]
        elif isinstance(img_data, dict) and img_data.get("url"):
            result["images"] = [img_data["url"]]
    if not result["volume"] and data.get("offers"):
        offers = data["offers"]
        if isinstance(offers, dict) and offers.get("sku"):
            volume_match = re.search(r"(\d+\s*ml|\d+\s*мл|\d+\s*г|\d+\s*ml)", offers.get("sku", ""), re.I)
            if volume_match:
                result["volume"] = volume_match.group(1)


def populate_product_volume(result, html: str):
    for pattern in [r"(\d+\s*ml)", r"(\d+\s*мл)", r"(\d+\s*г)", r"(\d+\s*ml)", r"(\d+\s*l)"]:
        if not result["volume"]:
            match = re.search(pattern, html, re.IGNORECASE)
            if match:
                result["volume"] = match.group(1).lower()


def populate_product_category(result, soup):
    text_content = soup.get_text()
    category_keywords = {
        "Очищение": ["очищающий", "гель для умывания", "мицеллярная вода", "пенка", "mousse", "cleanser", "wash"],
        "Увлажнение": ["увлажняющий", "крем для лица", "hydration", "moisturizer", "cream"],
        "Сыворотка": ["сыворотка", "serum", "эссенция", "essence"],
        "SPF": ["spf", "sun protection", "защита от солнца", "sunscreen", "uv"],
        "Маска": ["маска для лица", "mask", "sheet mask"],
        "Тоник": ["тоник", "toner"],
        "Масло": ["масло для лица", "oil", "huile"],
    }
    for category, keywords in category_keywords.items():
        for keyword in keywords:
            if keyword.lower() in text_content.lower():
                result["category"] = category
                break
        if result["category"]:
            break
    if not result["category"]:
        category_tag = soup.find("span", class_=re.compile(r"category|cat", re.I))
        if category_tag:
            category_text = category_tag.get_text(strip=True)
            for category, keywords in category_keywords.items():
                for keyword in keywords:
                    if keyword.lower() in category_text.lower():
                        result["category"] = category
                        break
                if result["category"]:
                    break
