# URL Parser Feature Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add ability to paste a product URL and automatically fill product form fields by parsing the URL on the backend.

**Architecture:** 
- Backend: New FastAPI endpoint `/api/parse-product-url` that accepts a URL, fetches the page, extracts product data using site-specific rules, and returns structured data.
- Frontend: Add URL input field in product form, call backend parse endpoint on blur or button click, auto-populate form fields with parsed data.

**Tech Stack:** 
- Backend: Python, FastAPI, beautifulsoup4, requests
- Frontend: React, Axios (via existing fetch wrapper)

---
### Task 1: Backend - Add parsing service

**Files:**
- Create: `backend/api/app/services/url_parser.py`
- Modify: `backend/api/app/main.py:34-38` (import and add route)
- Modify: `backend/api/requirements.txt`

- [ ] **Step 1: Write the failing test for URL parser service**

```python
# tests/test_url_parser.py
import pytest
from app.services.url_parser import parse_product_url

def test_parse_ozon_url():
    # This will fail because function doesn't exist
    result = parse_product_url("https://www.ozon.ru/product/test")
    assert result["name"] == "Test Product"
```

- [ ] **Step 2: Run test to verify it fails**

Run: `pytest backend/api/tests/test_url_parser.py::test_parse_ozon_url -v`
Expected: FAIL with "ModuleNotFoundError: No module named 'app.services.url_parser'"

- [ ] **Step 3: Create url_parser service with stub implementation**

```python
# backend/api/app/services/url_parser.py
import re
from typing import Dict, Any

def parse_product_url(url: str) -> Dict[str, Any]:
    """
    Parse product URL and extract product data.
    Currently supports Ozon.ru (stub implementation)
    """
    # Stub implementation for testing
    if "ozon.ru" in url:
        return {
            "name": "Test Product from Ozon",
            "brand": "TestBrand",
            "category": "Уход",
            "description": "Test description",
            "images": ["https://example.com/image.jpg"],
            "volume": "50мл",
            "segment": "Бюджетная"
        }
    # Return empty dict for unsupported URLs
    return {}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `pytest backend/api/tests/test_url_parser.py::test_parse_ozon_url -v`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add backend/api/app/services/url_parser.py backend/api/tests/test_url_parser.py
git commit -m "feat: add url parser service stub"
```

### Task 2: Backend - Add API endpoint for URL parsing

**Files:**
- Modify: `backend/api/app/main.py` (add import and route)
- Modify: `backend/api/app/services/__init__.py` (if needed to expose parser)

- [ ] **Step 1: Write the failing test for parse endpoint**

```python
# tests/test_parse_endpoint.py
from fastapi.testclient import TestClient
from app.main import app

client = TestClient(app)

def test_parse_product_url_endpoint():
    response = client.post("/api/parse-product-url", json={"url": "https://www.ozon.ru/product/test"})
    assert response.status_code == 200
    data = response.json()
    assert data["name"] == "Test Product from Ozon"
```

- [ ] **Step 2: Run test to verify it fails**

Run: `pytest backend/api/tests/test_parse_endpoint.py::test_parse_product_url_endpoint -v`
Expected: FAIL with "404 Not Found" or similar

- [ ] **Step 3: Add endpoint to main.py**

```python
# backend/api/app/main.py (add after other imports)
from app.services.url_parser import parse_product_url

# Add after route definitions, before if __name__ == "__main__":
@app.post("/api/parse-product-url")
async def parse_product_url_endpoint(payload: dict):
    url = payload.get("url")
    if not url:
        raise HTTPException(status_code=400, detail="URL is required")
    try:
        product_data = parse_product_url(url)
        if not product_data:
            raise HTTPException(status_code=400, detail="Could not parse product from URL")
        return product_data
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))
```

- [ ] **Step 4: Run test to verify it passes**

Run: `pytest backend/api/tests/test_parse_endpoint.py::test_parse_product_url_endpoint -v`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add backend/api/app/main.py backend/api/tests/test_parse_endpoint.py
git commit -m "feat: add parse product url endpoint"
```

### Task 3: Backend - Update dependencies

**Files:**
- Modify: `backend/api/requirements.txt`

- [ ] **Step 1: Add required packages**

Update requirements.txt:
```
beautifulsoup4==4.12.2
requests==2.31.0
```

- [ ] **Step 2: Commit**

```bash
git add backend/api/requirements.txt
git commit -m "feat: add beautifulsoup4 and requests dependencies"
```

### Task 4: Frontend - Add URL input field to product form

**Files:**
- Modify: `web-admin/src/pages/Products.jsx`
- Modify: `web-admin/src/api/index.js` (add parseUrl function)

- [ ] **Step 1: Write failing test (manual verification) - we'll do manual check**

We'll skip automated tests for frontend for simplicity, but we can add a simple test later.

- [ ] **Step 2: Add parseUrl function to API**

```javascript
// web-admin/src/api/index.js
export const productsApi = {
  // ... existing functions
  parseUrl: (url) => request('/parse-product-url', { method: 'POST', body: JSON.stringify({ url }) }),
};
```

- [ ] **Step 3: Modify Products.jsx to add URL field and handling**

Add state for URL input and parsing status:
```javascript
const [url, setUrl] = useState('');
const [parsing, setParsing] = useState(false);
```

Add to formData initially (or handle separately):
We'll add a separate URL field that when submitted triggers parsing.

In the form, add:
```javascript
<div className="form-group"><label>Ссылка на продукт</label><input name="url" value={url} onChange={e => setUrl(e.target.value)} className="input" placeholder="Вставьте URL продукта" /></div>
{parsing && <div className="form-group">Идет обработка ссылки...</div>}
<button type="button" className="btn btn-ghost" onClick={handleParseUrl} disabled={!url || parsing}>Парсить ссылку</button>
```

Add handler:
```javascript
const handleParseUrl = async () => {
  if (!url) return;
  setParsing(true);
  try {
    const parsedData = await productsApi.parseUrl(url);
    // Update formData with parsed values
    setFormData(prev => ({
      ...prev,
      name: parsedData.name || '',
      brand: parsedData.brand || '',
      category: parsedData.category || '',
      description: parsedData.description || '',
      images: parsedData.images?.join('\n') || '',
      volume: parsedData.volume || '',
      segment: parsedData.segment || '',
    }));
    success('Данные продукта из ссылки загружены');
  } catch (err) {
    error('Не удалось распарсить ссылку: ' + err.message);
  } finally {
    setParsing(false);
  }
};
```

- [ ] **Step 4: Run the dev server to verify manually**

Run: `cd web-admin && npm run dev` (or whatever dev command)
Check that the URL field appears and parsing works.

- [ ] **Step 5: Commit**

```bash
git add web-admin/src/pages/Products.jsx web-admin/src/api/index.js
git commit -m "feat: add URL parsing to product form"
```

### Task 5: Improve parser with real site support (optional extension)

**Files:**
- Modify: `backend/api/app/services/url_parser.py`

- [ ] **Step 1: Implement real parsing for Ozon using requests and BeautifulSoup**

```python
# backend/api/app/services/url_parser.py
import requests
from bs4 import BeautifulSoup
import re

def parse_product_url(url: str) -> Dict[str, Any]:
    """
    Parse product URL and extract product data.
    Supports Ozon.ru
    """
    headers = {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36'
    }
    try:
        response = requests.get(url, headers=headers, timeout=10)
        response.raise_for_status()
    except Exception as e:
        raise Exception(f"Failed to fetch URL: {str(e)}")
    
    soup = BeautifulSoup(response.content, 'html.parser')
    
    if "ozon.ru" in url:
        # Extract title
        title_elem = soup.find('h1')
        name = title_elem.get_text(strip=True) if title_elem else ""
        
        # Extract brand (often in breadcrumbs or specific block)
        brand = ""
        # Try to find brand
        brand_elem = soup.find('a', {'data-widget': 'brand'})
        if brand_elem:
            brand = brand_elem.get_text(strip=True)
        else:
            # fallback: look for brand in text
            brand_match = re.search(r'Бренд[:\s]+([^\n]+)', soup.get_text())
            if brand_match:
                brand = brand_match.group(1).strip()
        
        # Extract images
        images = []
        img_elements = soup.find_all('img')
        for img in img_elements[:5]:  # limit to first 5
            src = img.get('src') or img.get('data-src')
            if src and ('http' in src or '//' in src):
                if src.startswith('//'):
                    src = 'https:' + src
                images.append(src)
        
        # Extract description
        description = ""
        desc_elem = soup.find('div', {'data-widget': 'description'})
        if desc_elem:
            description = desc_elem.get_text(strip=True)
        else:
            # fallback
            desc_elem = soup.find('div', string=re.compile('Описание', re.I))
            if desc_elem:
                description = desc_elem.find_next('div').get_text(strip=True) if desc_elem.find_next('div') else ""
        
        # For volume, segment, category we might need more specific parsing
        # For now, we'll leave them empty or try to guess from text
        volume = ""
        segment = ""
        category = ""
        
        return {
            "name": name,
            "brand": brand,
            "category": category,
            "description": description,
            "images": images,
            "volume": volume,
            "segment": segment,
        }
    else:
        # Unsupported site
        return {}
```

- [ ] **Step 2: Test with a real Ozon URL (manual)**

- [ ] **Step 3: Commit**

```bash
git add backend/api/app/services/url_parser.py
git commit -m "feat: implement real Ozon parsing with BeautifulSoup"
```

### Task 6: Add support for additional sites (Wildberries, etc.) - optional

We can extend the parser with more site-specific logic.

But for MVP, we stop after Task 5.

---
**Note:** After completing these steps, run the backend and frontend to verify end-to-end functionality.

Run backend: `cd backend/api && uvicorn app.main:app --reload`
Run frontend: `cd web-admin && npm run dev`

Test by visiting http://localhost:5173, navigating to Products, adding a product, pasting an Ozon URL, and clicking "Парсить ссылку".

All tasks should be committed frequently.