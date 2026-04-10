from typing import List, Optional, Union
import re
import json
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
    "skin_types": "skin_types",
    "product_types": "product_types",
    "for_whom": "for_whom",
    "purposes": "purposes",
    "application_times": "application_times",
    "areas": "areas",
    "countries": "countries",
    # Procedure dictionaries
    "methodTypes": "procedure_method_types",
    "procedureDurations": "procedure_durations",
    "procedureEquipment": "procedure_equipment",
    "procedureZones": "procedure_zones",
    "procedureEffects": "procedure_effects",
    "procedureProblems": "procedure_problems",
}


def serialize_purpose(purpose):
    if purpose is None:
        return None
    if isinstance(purpose, list):
        return json.dumps(purpose, ensure_ascii=False)
    return purpose


def deserialize_purpose(purpose):
    # Return None for null/empty values
    if purpose is None:
        return None
    # Already a list - return as is
    if isinstance(purpose, list):
        return purpose
    # If it's not a string, return None
    if not isinstance(purpose, str):
        return None
    # Empty string
    if not purpose.strip():
        return None
    # Try to parse JSON array
    if purpose.strip().startswith('['):
        try:
            return json.loads(purpose)
        except:
            return None
    # Not a JSON array - could be corrupted data, return None
    return None


class ProductService:
    @staticmethod
    def get_all() -> List[dict]:
        pool = get_pool()
        conn = pool.getconn()
        try:
            cursor = conn.cursor()
            cursor.execute("SELECT * FROM products ORDER BY id DESC")
            rows = cursor.fetchall()
            columns = [desc[0] for desc in cursor.description]
            results = []
            for row in rows:
                product = dict(zip(columns, row))
                # Always deserialize - the function now handles null/invalid safely
                product['purpose'] = deserialize_purpose(product.get('purpose'))
                product['skin_type'] = deserialize_purpose(product.get('skin_type'))
                if product.get('photos') and isinstance(product['photos'], str):
                    product['photos'] = json.loads(product['photos'])
                results.append(product)
            return results
        finally:
            pool.putconn(conn)

    @staticmethod
    def create(data: ProductCreate) -> dict:
        pool = get_pool()
        conn = pool.getconn()
        try:
            cursor = conn.cursor()
            cursor.execute(
                """INSERT INTO products (
                    name, what_is_it, brand, product_type, for_whom, purpose,
                    skin_type, application_time, area, active_ingredient,
                    volume, segment, composition, application_info,
                    country, country_origin, manufacturer, description, photos, has_video
                ) VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s) RETURNING *""",
                (data.name, data.what_is_it, data.brand, data.product_type, data.for_whom,
                 serialize_purpose(data.purpose), serialize_purpose(data.skin_type), data.application_time, data.area,
                 data.active_ingredient, data.volume, data.segment, data.composition,
                 data.application_info, data.country, getattr(data, 'country_origin', None), data.manufacturer,
                 data.description, json.dumps(data.photos) if data.photos else None, data.has_video)
            )
            row = cursor.fetchone()
            conn.commit()
            columns = [desc[0] for desc in cursor.description]
            return dict(zip(columns, row))
        finally:
            pool.putconn(conn)

    @staticmethod
    def update(product_id: int, data: ProductCreate) -> Optional[dict]:
        pool = get_pool()
        conn = pool.getconn()
        try:
            cursor = conn.cursor()
            
            # Get existing product to merge with new data
            cursor.execute("SELECT * FROM products WHERE id=%s", (product_id,))
            existing_row = cursor.fetchone()
            if not existing_row:
                return None
            
            columns = [desc[0] for desc in cursor.description]
            existing = dict(zip(columns, existing_row))
            
            # Merge: use new value if provided, otherwise keep existing
            def merge(new_val, existing_val):
                if new_val is None:
                    return existing_val
                if isinstance(new_val, list) and len(new_val) == 0:
                    return existing_val
                if isinstance(new_val, str) and new_val.strip() == '':
                    return existing_val
                if isinstance(new_val, bool):
                    return new_val
                return new_val
            
            name = merge(data.name, existing.get('name'))
            what_is_it = merge(data.what_is_it, existing.get('what_is_it'))
            brand = merge(data.brand, existing.get('brand'))
            product_type = merge(data.product_type, existing.get('product_type'))
            for_whom = merge(data.for_whom, existing.get('for_whom'))
            purpose = merge(serialize_purpose(data.purpose), existing.get('purpose'))
            skin_type = merge(serialize_purpose(data.skin_type), existing.get('skin_type'))
            application_time = merge(data.application_time, existing.get('application_time'))
            area = merge(data.area, existing.get('area'))
            active_ingredient = merge(data.active_ingredient, existing.get('active_ingredient'))
            volume = merge(data.volume, existing.get('volume'))
            segment = merge(data.segment, existing.get('segment'))
            composition = merge(data.composition, existing.get('composition'))
            application_info = merge(data.application_info, existing.get('application_info'))
            country = merge(data.country, existing.get('country'))
            country_origin = merge(getattr(data, 'country_origin', None), existing.get('country_origin'))
            manufacturer = merge(data.manufacturer, existing.get('manufacturer'))
            description = merge(data.description, existing.get('description'))
            photos = merge(json.dumps(data.photos) if data.photos else None, existing.get('photos'))
            has_video = data.has_video if data.has_video is not None else existing.get('has_video')
            video = merge(data.video, existing.get('video'))
            
            cursor.execute(
                """UPDATE products SET 
                    name=%s, what_is_it=%s, brand=%s, product_type=%s, for_whom=%s,
                    purpose=%s, skin_type=%s, application_time=%s, area=%s,
                    active_ingredient=%s, volume=%s, segment=%s, composition=%s,
                    application_info=%s, country=%s, country_origin=%s, manufacturer=%s,
                    description=%s, photos=%s, has_video=%s, video=%s
                WHERE id=%s RETURNING *""",
                (name, what_is_it, brand, product_type, for_whom,
                 purpose, skin_type, application_time, area,
                 active_ingredient, volume, segment, composition,
                 application_info, country, country_origin, manufacturer,
                 description, photos, has_video, video, product_id)
            )
            row = cursor.fetchone()
            conn.commit()
            if row:
                columns = [desc[0] for desc in cursor.description]
                return dict(zip(columns, row))
            return None
        finally:
            pool.putconn(conn)

    @staticmethod
    def get_by_id(product_id: int) -> Optional[dict]:
        pool = get_pool()
        conn = pool.getconn()
        try:
            cursor = conn.cursor()
            cursor.execute("SELECT * FROM products WHERE id=%s", (product_id,))
            row = cursor.fetchone()
            if row:
                columns = [desc[0] for desc in cursor.description]
                product = dict(zip(columns, row))
                if product.get('purpose'):
                    product['purpose'] = deserialize_purpose(product['purpose'])
                if product.get('skin_type'):
                    product['skin_type'] = deserialize_purpose(product['skin_type'])
                if product.get('photos') and isinstance(product['photos'], str):
                    product['photos'] = json.loads(product['photos'])
                return product
            return None
        finally:
            pool.putconn(conn)

    @staticmethod
    def delete(product_id: int) -> None:
        pool = get_pool()
        conn = pool.getconn()
        try:
            cursor = conn.cursor()
            cursor.execute("DELETE FROM products WHERE id=%s", (product_id,))
            conn.commit()
        finally:
            pool.putconn(conn)

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
    def get_brands() -> List[dict]:
        pool = get_pool()
        conn = pool.getconn()
        try:
            cursor = conn.cursor()
            cursor.execute("SELECT id, value, description, country, country_origin, manufacturer FROM brands ORDER BY id")
            rows = cursor.fetchall()
            return [{'id': row[0], 'value': row[1], 'description': row[2], 'country': row[3], 'country_origin': row[4], 'manufacturer': row[5]} for row in rows]
        finally:
            pool.putconn(conn)

    @staticmethod
    def create_brand(value: str, description: str = None, country: str = None, country_origin: str = None, manufacturer: str = None) -> dict:
        pool = get_pool()
        conn = pool.getconn()
        try:
            cursor = conn.cursor()
            cursor.execute("INSERT INTO brands (value, description, country, country_origin, manufacturer) VALUES (%s, %s, %s, %s, %s) RETURNING *", (value, description, country, country_origin, manufacturer))
            row = cursor.fetchone()
            conn.commit()
            columns = [desc[0] for desc in cursor.description]
            return dict(zip(columns, row))
        finally:
            pool.putconn(conn)

    @staticmethod
    def update_brand(value: str, description: str = None, country: str = None, country_origin: str = None, manufacturer: str = None) -> dict:
        pool = get_pool()
        conn = pool.getconn()
        try:
            cursor = conn.cursor()
            cursor.execute(
                "UPDATE brands SET description=%s, country=%s, country_origin=%s, manufacturer=%s WHERE value=%s RETURNING *",
                (description, country, country_origin, manufacturer, value)
            )
            row = cursor.fetchone()
            conn.commit()
            if row:
                columns = [desc[0] for desc in cursor.description]
                return dict(zip(columns, row))
            return {}
        finally:
            pool.putconn(conn)

    @staticmethod
    def get_values(key: str) -> List[str]:
        table = DictionaryService.get_table(key)
        if not table:
            return []
        pool = get_pool()
        conn = pool.getconn()
        try:
            cursor = conn.cursor()
            cursor.execute(f"SELECT value FROM {table} ORDER BY id")
            rows = cursor.fetchall()
            return [row[0] for row in rows]
        finally:
            pool.putconn(conn)

    @staticmethod
    def create_value(key: str, value: str) -> dict:
        table = DictionaryService.get_table(key)
        if not table:
            raise ValueError(f"Unknown dictionary key: {key}")
        pool = get_pool()
        conn = pool.getconn()
        try:
            cursor = conn.cursor()
            cursor.execute(f"INSERT INTO {table} (value) VALUES (%s) RETURNING *", (value,))
            row = cursor.fetchone()
            conn.commit()
            columns = [desc[0] for desc in cursor.description]
            return dict(zip(columns, row))
        finally:
            pool.putconn(conn)

    @staticmethod
    def update_value(key: str, old_value: str, new_value: str) -> dict:
        table = DictionaryService.get_table(key)
        if not table:
            raise ValueError(f"Unknown dictionary key: {key}")
        pool = get_pool()
        conn = pool.getconn()
        try:
            cursor = conn.cursor()
            cursor.execute(f"UPDATE {table} SET value=%s WHERE value=%s RETURNING *", (new_value, old_value))
            row = cursor.fetchone()
            conn.commit()
            if row:
                columns = [desc[0] for desc in cursor.description]
                return dict(zip(columns, row))
            return {}
        finally:
            pool.putconn(conn)

    @staticmethod
    def delete_value(key: str, value: str) -> None:
        table = DictionaryService.get_table(key)
        if not table:
            raise ValueError(f"Unknown dictionary key: {key}")
        pool = get_pool()
        conn = pool.getconn()
        try:
            cursor = conn.cursor()
            cursor.execute(f"DELETE FROM {table} WHERE value=%s", (value,))
            conn.commit()
        finally:
            pool.putconn(conn)
