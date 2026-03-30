from typing import List, Optional
import re
import aiohttp
from bs4 import BeautifulSoup
from fastapi import HTTPException
from app.database import get_pool
from app.models.product import Product, ProductCreate


DICT_TABLE_MAP = {
    "brands": "brands",
    "categories": "categories",
    "segments": "segments",
    "volumes": "volumes",
    "procedureCategories": "procedure_categories",
    "contentCategories": "content_categories",
    "userRoles": "user_roles",
    "skinTypes": "skin_types"
}


class ProductService:
    @staticmethod
    async def get_all() -> List[dict]:
        async with get_pool().acquire() as conn:
            rows = await conn.fetch("SELECT * FROM products ORDER BY id DESC")
            return [dict(row) for row in rows]

    @staticmethod
    async def create(data: ProductCreate) -> dict:
        async with get_pool().acquire() as conn:
            row = await conn.fetchrow(
                """INSERT INTO products (
                    name, what_is_it, brand, product_type, for_whom, purpose,
                    skin_type, application_time, area, active_ingredient,
                    volume, segment, composition, application_info,
                    country, manufacturer, description, photos, has_video
                ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13, $14, $15, $16, $17, $18, $19) RETURNING *""",
                data.name, data.what_is_it, data.brand, data.product_type, data.for_whom,
                data.purpose, data.skin_type, data.application_time, data.area,
                data.active_ingredient, data.volume, data.segment, data.composition,
                data.application_info, data.country, data.manufacturer,
                data.description, data.photos, data.has_video
            )
            return dict(row)

    @staticmethod
    async def update(product_id: int, data: ProductCreate) -> Optional[dict]:
        async with get_pool().acquire() as conn:
            row = await conn.fetchrow(
                """UPDATE products SET 
                    name=$1, what_is_it=$2, brand=$3, product_type=$4, for_whom=$5,
                    purpose=$6, skin_type=$7, application_time=$8, area=$9,
                    active_ingredient=$10, volume=$11, segment=$12, composition=$13,
                    application_info=$14, country=$15, manufacturer=$16,
                    description=$17, photos=$18, has_video=$19
                WHERE id=$20 RETURNING *""",
                data.name, data.what_is_it, data.brand, data.product_type, data.for_whom,
                data.purpose, data.skin_type, data.application_time, data.area,
                data.active_ingredient, data.volume, data.segment, data.composition,
                data.application_info, data.country, data.manufacturer,
                data.description, data.photos, data.has_video, product_id
            )
            return dict(row) if row else None

    @staticmethod
    async def get_by_id(product_id: int) -> Optional[dict]:
        async with get_pool().acquire() as conn:
            row = await conn.fetchrow("SELECT * FROM products WHERE id=$1", product_id)
            return dict(row) if row else None

    @staticmethod
    async def delete(product_id: int) -> None:
        async with get_pool().acquire() as conn:
            await conn.execute("DELETE FROM products WHERE id=$1", product_id)

    @staticmethod
    async def parse_url(url: str) -> dict:
        result = {
            "name": None,
            "brand": None,
            "category": None,
            "description": None,
            "images": [],
            "volume": None
        }
        
        wb_match = re.search(r'wildberries\.ru/catalog/(\d+)', url)
        if wb_match:
            product_id = wb_match.group(1)
            wb_result = await ProductService._parse_wildberries(product_id)
            if wb_result.get("name"):
                return wb_result
        
        ozon_match = re.search(r'ozon\.ru/product/([^/?]+)', url)
        if ozon_match:
            ozon_result = await ProductService._parse_ozon(url)
            if ozon_result.get("name"):
                return ozon_result
        
        return await ProductService._parse_generic(url)
    
    @staticmethod
    async def _parse_wildberries(product_id: str) -> dict:
        result = {
            "name": None,
            "brand": None,
            "category": None,
            "description": None,
            "images": [],
            "volume": None
        }
        
        try:
            from playwright.async_api import async_playwright
            
            async with async_playwright() as p:
                browser = await p.chromium.launch(headless=True)
                page = await browser.new_page()
                
                try:
                    await page.goto(f"https://www.wildberries.ru/catalog/{product_id}/detail.aspx", timeout=15000)
                    await page.wait_for_load_state("networkidle", timeout=10000)
                    
                    result["name"] = await page.title()
                    if result["name"] and " — " in result["name"]:
                        result["name"] = result["name"].split(" — ")[0].split(" | ")[0].strip()
                    elif result["name"] and " | " in result["name"]:
                        result["name"] = result["name"].split(" | ")[0].strip()
                    
                    brand_el = await page.query_selector('[data-link="text__brandName"]')
                    if not brand_el:
                        brand_el = await page.query_selector('.brand-name')
                    if brand_el:
                        result["brand"] = await brand_el.inner_text()
                    
                    desc_el = await page.query_selector('meta[name="description"]')
                    if desc_el:
                        result["description"] = await desc_el.get_attribute("content")
                    
                    images = await page.query_selector_all('img[src*="wb"]')
                    for img in images[:5]:
                        src = await img.get_attribute("src")
                        if src and src not in result["images"]:
                            result["images"].append(src)
                    
                    price_el = await page.query_selector('[class*="price"]')
                    if price_el:
                        price_text = await price_el.inner_text()
                        import re
                        vol_match = re.search(r'(\d+\s*(?:мл|ml|г|g|л|l))', price_text, re.IGNORECASE)
                        if vol_match:
                            result["volume"] = vol_match.group(1)
                            
                except Exception as e:
                    print(f"Playwright error: {e}")
                finally:
                    await browser.close()
                    
        except Exception as e:
            print(f"Playwright init error: {e}")
            return await ProductService._parse_generic(f"https://www.wildberries.ru/catalog/{product_id}/detail.aspx")
        
        return result
    
    @staticmethod
    async def _parse_ozon(url: str) -> dict:
        result = {
            "name": None,
            "brand": None,
            "category": None,
            "description": None,
            "images": [],
            "volume": None
        }
        
        headers = {
            "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            "Accept": "application/json",
        }
        
        ozon_match = re.search(r'/product/(\d+)', url)
        if ozon_match:
            product_id = ozon_match.group(1)
            try:
                api_url = f"https://www.ozon.ru/api/composer-api.bx/page/json/v2?url={url}"
                async with aiohttp.ClientSession() as session:
                    async with session.get(api_url, headers=headers, timeout=aiohttp.ClientTimeout(total=10)) as response:
                        if response.status == 200:
                            data = await response.json()
                            if data.get("widgetState"):
                                for widget in data["widgetState"]:
                                    if widget.get("id") == "productMain":
                                        product_data = widget.get("json", {})
                                        result["name"] = product_data.get("title", {}).get("text")
                                        result["brand"] = product_data.get("brand")
                                        result["description"] = product_data.get("description")
                                        if product_data.get("media"):
                                            for media in product_data["media"]:
                                                if media.get("type") == "image":
                                                    result["images"].append(media.get("link", {}).get("url", ""))
            except Exception as e:
                pass
        
        if not result["name"]:
            try:
                return await ProductService._parse_generic(url)
            except:
                pass
        
        return result
    
    @staticmethod
    async def _parse_generic(url: str) -> dict:
        parsed_url = url.split('?')[0]
        domain = parsed_url.split('/')[2] if len(parsed_url.split('/')) > 2 else ""
        
        headers = {
            "User-Agent": "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8",
            "Accept-Language": "ru-RU,ru;q=0.9,en-US;q=0.8,en;q=0.7",
            "Accept-Encoding": "gzip, deflate, br",
            "Connection": "keep-alive",
            "Upgrade-Insecure-Requests": "1",
            "Sec-Fetch-Dest": "document",
            "Sec-Fetch-Mode": "navigate",
            "Sec-Fetch-Site": "same-origin",
            "Sec-Fetch-User": "?1",
            "Cache-Control": "max-age=0",
            "sec-ch-ua": '"Not_A Brand";v="8", "Chromium";v="120", "Google Chrome";v="120"',
            "sec-ch-ua-mobile": "?0",
            "sec-ch-ua-platform": '"macOS"',
            "Referer": f"https://{domain}/",
        }
        
        cookie_jar = aiohttp.CookieJar()
        
        try:
            async with aiohttp.ClientSession(cookie_jar=cookie_jar) as session:
                async with session.get(url, headers=headers, timeout=aiohttp.ClientTimeout(total=15)) as response:
                    if response.status != 200:
                        raise HTTPException(status_code=400, detail=f"Failed to fetch URL: status {response.status}")
                    html = await response.text()
        except Exception as e:
            raise HTTPException(status_code=400, detail=f"Error fetching URL: {str(e)}")
        
        soup = BeautifulSoup(html, 'html.parser')
        
        result = {
            "name": None,
            "brand": None,
            "category": None,
            "description": None,
            "images": [],
            "volume": None
        }
        
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
        
        brand_meta = soup.find("meta", property="product:brand")
        if brand_meta:
            result["brand"] = brand_meta.get("content", "").strip()
        
        if not result["brand"]:
            known_brands = ['La Roche-Posay', 'Vichy', 'Bioderma', 'CeraVe', 'The Ordinary', 
                           "Paula's Choice", 'Cosrx', 'Eucerin', 'Nivea', 'Aura', 'A-Derma',
                           'Uriage', 'Filorga', 'Nuxe', 'Darphin', 'Clarins', 'Estee Lauder',
                           'Lancome', 'Shiseido', 'Clinique', 'Origins', 'Decathlon', 'Bio']
            for brand in known_brands:
                if brand.lower() in html.lower():
                    result["brand"] = brand
                    break
        
        desc_meta = soup.find("meta", property="og:description")
        if desc_meta:
            result["description"] = desc_meta.get("content", "").strip()
        
        if not result["description"]:
            desc_tag = soup.find("meta", attrs={"name": "description"})
            if desc_tag:
                result["description"] = desc_tag.get("content", "").strip()
        
        img_tags = soup.find_all("img")
        for img in img_tags[:5]:
            src = img.get("src") or img.get("data-src") or img.get("data-lazy") or img.get("data-srcset", "").split()[0]
            if src and src.startswith("http"):
                if src not in result["images"]:
                    result["images"].append(src)
        
        og_image = soup.find("meta", property="og:image")
        if og_image:
            img_url = og_image.get("content", "")
            if img_url and img_url not in result["images"]:
                result["images"].insert(0, img_url)
        
        json_ld = soup.find("script", type="application/ld+json")
        if json_ld and json_ld.string:
            try:
                import json
                data = json.loads(json_ld.string)
                if isinstance(data, dict):
                    if not result["name"] and data.get("name"):
                        result["name"] = data["name"]
                    if not result["brand"] and data.get("brand"):
                        brand_val = data["brand"]
                        result["brand"] = brand_val if isinstance(brand_val, str) else brand_val.get("name")
                    if not result["description"] and data.get("description"):
                        result["description"] = data["description"][:500] if len(data.get("description", "")) > 500 else data.get("description")
                    if not result["images"] and data.get("image"):
                        img_data = data["image"]
                        if isinstance(img_data, list):
                            result["images"] = [i for i in img_data if isinstance(i, str)][:5]
                        elif isinstance(img_data, str):
                            result["images"] = [img_data]
                        elif isinstance(img_data, dict) and img_data.get("url"):
                            result["images"] = [img_data["url"]]
            except:
                pass
        
        volume_patterns = [
            r'(\d+\s*ml)',
            r'(\d+\s*мл)',
            r'(\d+\s*г)',
            r'(\d+\s*ml)',
            r'(\d+\s*l)',
        ]
        for pattern in volume_patterns:
            if not result["volume"]:
                match = re.search(pattern, html, re.IGNORECASE)
                if match:
                    result["volume"] = match.group(1).lower()
        
        text_content = soup.get_text()
        category_keywords = {
            'Очищение': ['очищающий', 'гель для умывания', 'мицеллярная вода', 'пенка', 'mousse', 'cleanser', 'wash'],
            'Увлажнение': ['увлажняющий', 'крем для лица', 'hydration', 'moisturizer', 'cream'],
            'Сыворотка': ['сыворотка', 'serum', 'эссенция', 'essence'],
            'SPF': ['spf', 'sun protection', 'защита от солнца', 'sunscreen', 'uv'],
            'Маска': ['маска для лица', 'mask', 'sheet mask'],
            'Тоник': ['тоник', 'toner'],
            'Масло': ['масло для лица', 'oil', 'huile'],
        }
        
        for cat, keywords in category_keywords.items():
            for kw in keywords:
                if kw.lower() in text_content.lower():
                    result["category"] = cat
                    break
            if result["category"]:
                break
        
        return result


class DictionaryService:
    @staticmethod
    def get_table(key: str) -> Optional[str]:
        return DICT_TABLE_MAP.get(key)

    @staticmethod
    async def get_values(key: str) -> List[str]:
        table = DictionaryService.get_table(key)
        if not table:
            return []
        async with get_pool().acquire() as conn:
            rows = await conn.fetch(f"SELECT value FROM {table} ORDER BY id")
            return [row["value"] for row in rows]

    @staticmethod
    async def create_value(key: str, value: str) -> dict:
        table = DictionaryService.get_table(key)
        if not table:
            raise ValueError(f"Unknown dictionary key: {key}")
        async with get_pool().acquire() as conn:
            row = await conn.fetchrow(
                f"INSERT INTO {table} (value) VALUES ($1) RETURNING *",
                value
            )
            return dict(row)

    @staticmethod
    async def update_value(key: str, old_value: str, new_value: str) -> dict:
        table = DictionaryService.get_table(key)
        if not table:
            raise ValueError(f"Unknown dictionary key: {key}")
        async with get_pool().acquire() as conn:
            row = await conn.fetchrow(
                f"UPDATE {table} SET value=$1 WHERE value=$2 RETURNING *",
                new_value, old_value
            )
            return dict(row) if row else {}

    @staticmethod
    async def delete_value(key: str, value: str) -> None:
        table = DictionaryService.get_table(key)
        if not table:
            raise ValueError(f"Unknown dictionary key: {key}")
        async with get_pool().acquire() as conn:
            await conn.execute(f"DELETE FROM {table} WHERE value=$1", value)
