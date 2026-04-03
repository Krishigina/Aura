# Расширенная сущность Продукт - План реализации

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Расширить модель Product с новыми полями, добавить загрузку фото/видео в БД

**Architecture:** PostgreSQL с BYTEA для видео, JSONB для фото. FastAPI для бэкенда, React для фронтенда.

**Tech Stack:** Python (FastAPI), PostgreSQL, React, pydantic

---

## Структура файлов

### Бэкенд
- `backend/api/app/models/product.py` - Модель Pydantic
- `backend/api/app/database.py` - Миграция таблицы
- `backend/api/app/services/product_service.py` - Бизнес-логика
- `backend/api/app/api/routes/products.py` - Роуты + загрузка файлов

### Фронтенд  
- `web-admin/src/pages/Products.jsx` - Форма с новыми полями
- `web-admin/src/pages/Products.css` - Стили
- `web-admin/src/api/index.js` - API методы загрузки
- `web-admin/src/components/FileUpload.jsx` - Компонент загрузки (создать)

---

## Задачи

### Task 1: Обновить модель Product (бэкенд)

**Files:**
- Modify: `backend/api/app/models/product.py`

- [ ] **Шаг 1: Прочитать текущую модель**

```python
# Текущая структура:
class ProductBase(BaseModel):
    name: str
    brand: Optional[str] = None
    category: Optional[str] = None
    description: Optional[str] = None
    images: Optional[List[str]] = None
    volume: Optional[str] = None
    segment: Optional[str] = None
```

- [ ] **Шаг 2: Заменить на новую модель**

```python
from pydantic import BaseModel
from typing import Optional, List
from enum import Enum

class ProductTypeEnum(str, Enum):
    Крем = "Крем"
    Сыворотка = "Сыворотка"
    Лосьон = "Лосьон"
    Тоник = "Тоник"
    Эмульсия = "Эмульсия"
    Масло = "Масло"
    Гель = "Гель"
    Пилинг = "Пилинг"
    Маска = "Маска"
    Бальзам = "Бальзам"
    Спрей = "Спрей"
    Мист = "Мист"

class ForWhomEnum(str, Enum):
    Универсальный = "Универсальный"
    Мужчинам = "Мужчинам"
    Женщинам = "Женщинам"

class PurposeEnum(str, Enum):
    Увлажнение = "Увлажнение"
    Очищение = "Очищение"
    Питание = "Питание"
    Антивозрастной = "Антивозрастной"
    Отбеливание = "Отбеливание"
    Защита_от_солнца = "Защита от солнца"
    Проблемная_кожа = "Проблемная кожа"
    Восстановление = "Восстановление"
    Матирование = "Матирование"
    Тонирование = "Тонирование"

class SkinTypeEnum(str, Enum):
    Сухая = "Сухая"
    Жирная = "Жирная"
    Комбинированная = "Комбинированная"
    Нормальная = "Нормальная"
    Чувствительная = "Чувствительная"
    Проблемная = "Проблемная"

class ApplicationTimeEnum(str, Enum):
    Утро = "Утро"
    Вечер = "Вечер"
    Утро_Вечер = "Утро/Вечер"

class AreaEnum(str, Enum):
    Лицо = "Лицо"
    Тело = "Тело"
    Волосы = "Волосы"
    Губы = "Губы"
    Руки = "Руки"
    Веки = "Веки"
    Зона_вокруг_глаз = "Зона вокруг глаз"

class SegmentEnum(str, Enum):
    Бюджетная = "Бюджетная"
    Люкс = "Люкс"
    Профессиональная = "Профессиональная"
    Космецевтика = "Космецевтика"

class PhotoItem(BaseModel):
    id: str
    filename: str
    data: str  # base64
    content_type: str

class ProductBase(BaseModel):
    name: str
    what_is_it: Optional[str] = None
    brand: Optional[str] = None
    product_type: Optional[ProductTypeEnum] = None
    for_whom: Optional[ForWhomEnum] = None
    purpose: Optional[PurposeEnum] = None
    skin_type: Optional[SkinTypeEnum] = None
    application_time: Optional[ApplicationTimeEnum] = None
    area: Optional[AreaEnum] = None
    active_ingredient: Optional[str] = None
    volume: Optional[str] = None
    segment: Optional[SegmentEnum] = None
    composition: Optional[str] = None
    application_info: Optional[str] = None
    country: Optional[str] = None
    manufacturer: Optional[str] = None
    description: Optional[str] = None
    photos: Optional[List[PhotoItem]] = None
    has_video: Optional[bool] = False

class ProductCreate(ProductBase):
    pass

class Product(ProductBase):
    id: int
    created_at: Optional[str] = None

    class Config:
        from_attributes = True
```

- [ ] **Шаг 3: Запустить проверку типов**

Run: `cd backend/api && python -c "from app.models.product import Product, ProductCreate; print('OK')"`
Expected: OK

- [ ] **Шаг 4: Commit**

```bash
git add backend/api/app/models/product.py
git commit -m "feat: расширить модель Product с новыми полями"
```

---

### Task 2: Миграция базы данных

**Files:**
- Modify: `backend/api/app/database.py`

- [ ] **Шаг 1: Прочитать текущую схему products (строки 28-38)**

- [ ] **Шаг 2: Обновить CREATE TABLE**

```sql
CREATE TABLE IF NOT EXISTS products (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    what_is_it TEXT,
    brand VARCHAR(255),
    product_type VARCHAR(50),
    for_whom VARCHAR(50),
    purpose VARCHAR(255),
    skin_type VARCHAR(50),
    application_time VARCHAR(50),
    area VARCHAR(50),
    active_ingredient TEXT,
    volume VARCHAR(50),
    segment VARCHAR(50),
    composition TEXT,
    application_info TEXT,
    country VARCHAR(100),
    manufacturer VARCHAR(255),
    description TEXT,
    photos JSONB,
    has_video BOOLEAN DEFAULT FALSE,
    video BYTEA,
    created_at TIMESTAMP DEFAULT NOW()
);
```

- [ ] **Шаг 3: Commit**

```bash
git add backend/api/app/database.py
git commit -m "feat: добавить новые поля в таблицу products"
```

---

### Task 3: Обновить ProductService

**Files:**
- Modify: `backend/api/app/services/product_service.py`

- [ ] **Шаг 1: Прочитать текущий service**

- [ ] **Шаг 2: Обновить методы create, update, get_all**

```python
# В методе create - добавить все новые поля в INSERT
INSERT INTO products (
    name, what_is_it, brand, product_type, for_whom, purpose, 
    skin_type, application_time, area, active_ingredient, 
    volume, segment, composition, application_info, country, 
    manufacturer, description, photos, has_video
) VALUES (...)

# В методе update - добавить все новые поля в UPDATE
UPDATE products SET 
    name=$1, what_is_it=$2, brand=$3, product_type=$4, for_whom=$5,
    purpose=$6, skin_type=$7, application_time=$8, area=$9,
    active_ingredient=$10, volume=$11, segment=$12, composition=$13,
    application_info=$14, country=$15, manufacturer=$16,
    description=$17, photos=$18, has_video=$19
WHERE id=$20
```

- [ ] **Шаг 3: Commit**

```bash
git add backend/api/app/services/product_service.py
git commit -m "feat: обновить ProductService для новых полей"
```

---

### Task 4: Добавить эндпоинты для файлов

**Files:**
- Modify: `backend/api/app/api/routes/products.py`

- [ ] **Шаг 1: Добавить импорты**

```python
from fastapi import UploadFile, File, HTTPException
from fastapi.responses import StreamingResponse
import io
import base64
import uuid
import json
```

- [ ] **Шаг 2: Добавить роуты для фото**

```python
@router.post("/{product_id}/photos")
async def upload_photo(product_id: int, file: UploadFile = File(...)):
    if not file.content_type.startswith('image/'):
        raise HTTPException(400, "Only images allowed")
    
    contents = await file.read()
    photo_data = base64.b64encode(contents).decode()
    
    product = await ProductService.get_by_id(product_id)
    if not product:
        raise HTTPException(404, "Product not found")
    
    photos = product.get('photos') or []
    photo_item = {
        "id": str(uuid.uuid4()),
        "filename": file.filename,
        "data": photo_data,
        "content_type": file.content_type
    }
    photos.append(photo_item)
    
    await ProductService.update(product_id, {"photos": photos})
    return {"id": photo_item["id"], "filename": file.filename}

@router.delete("/{product_id}/photos/{photo_id}")
async def delete_photo(product_id: int, photo_id: str):
    product = await ProductService.get_by_id(product_id)
    if not product:
        raise HTTPException(404, "Product not found")
    
    photos = [p for p in (product.get('photos') or []) if p.get('id') != photo_id]
    await ProductService.update(product_id, {"photos": photos})
    return {"success": True}
```

- [ ] **Шаг 3: Добавить роуты для видео**

```python
@router.post("/{product_id}/video")
async def upload_video(product_id: int, file: UploadFile = File(...)):
    if file.content_type != "video/mp4":
        raise HTTPException(400, "Only MP4 videos allowed")
    
    contents = await file.read()
    
    product = await ProductService.get_by_id(product_id)
    if not product:
        raise HTTPException(404, "Product not found")
    
    await ProductService.update(product_id, {"video": contents, "has_video": True})
    return {"success": True, "filename": file.filename}

@router.delete("/{product_id}/video")
async def delete_video(product_id: int):
    await ProductService.update(product_id, {"video": None, "has_video": False})
    return {"success": True}

@router.get("/{product_id}/video")
async def get_video(product_id: int):
    product = await ProductService.get_by_id(product_id)
    if not product or not product.get('video'):
        raise HTTPException(404, "Video not found")
    
    video_data = product['video']
    return StreamingResponse(
        io.BytesIO(video_data),
        media_type="video/mp4",
        headers={"Content-Disposition": f"inline; filename=video.mp4"}
    )
```

- [ ] **Шаг 4: Commit**

```bash
git add backend/api/app/api/routes/products.py
git commit -m "feat: добавить эндпоинты загрузки фото и видео"
```

---

### Task 5: Обновить фронтенд API

**Files:**
- Modify: `web-admin/src/api/index.js`

- [ ] **Шаг 1: Найти productsApi**

- [ ] **Шаг 2: Добавить методы**

```javascript
export const productsApi = {
  // ... existing methods
  
  uploadPhoto: async (productId, file) => {
    const formData = new FormData()
    formData.append('file', file)
    const response = await fetch(`${BASE_URL}/products/${productId}/photos`, {
      method: 'POST',
      headers: { ...authHeaders() },
      body: formData
    })
    return response.json()
  },
  
  deletePhoto: async (productId, photoId) => {
    const response = await fetch(`${BASE_URL}/products/${productId}/photos/${photoId}`, {
      method: 'DELETE',
      headers: authHeaders()
    })
    return response.json()
  },
  
  uploadVideo: async (productId, file) => {
    const formData = new FormData()
    formData.append('file', file)
    const response = await fetch(`${BASE_URL}/products/${productId}/video`, {
      method: 'POST',
      headers: { ...authHeaders() },
      body: formData
    })
    return response.json()
  },
  
  deleteVideo: async (productId) => {
    const response = await fetch(`${BASE_URL}/products/${productId}/video`, {
      method: 'DELETE',
      headers: authHeaders()
    })
    return response.json()
  },
  
  getVideoUrl: (productId) => `${BASE_URL}/products/${productId}/video`
}
```

- [ ] **Шаг 3: Commit**

```bash
git add web-admin/src/api/index.js
git commit -m "feat: добавить API методы для работы с фото и видео"
```

---

### Task 6: Создать компонент FileUpload

**Files:**
- Create: `web-admin/src/components/FileUpload.jsx`

- [ ] **Шаг 1: Создать компонент**

```jsx
import { useState, useRef } from 'react'
import { Upload, X, Image, Video } from 'lucide-react'
import './FileUpload.css'

export default function FileUpload({ 
  type = 'image', // 'image' or 'video'
  onUpload, 
  onDelete,
  files = [],
  maxFiles = 5
}) {
  const [uploading, setUploading] = useState(false)
  const inputRef = useRef(null)

  const handleDrop = async (e) => {
    e.preventDefault()
    const file = e.dataTransfer.files[0]
    if (file) await uploadFile(file)
  }

  const handleFileSelect = async (e) => {
    const file = e.target.files[0]
    if (file) await uploadFile(file)
  }

  const uploadFile = async (file) => {
    if (type === 'image' && !file.type.startsWith('image/')) return
    if (type === 'video' && file.type !== 'video/mp4') return
    
    setUploading(true)
    try {
      await onUpload(file)
    } finally {
      setUploading(false)
    }
  }

  const handleDelete = async (fileId) => {
    await onDelete(fileId)
  }

  return (
    <div className="file-upload">
      <div 
        className="file-upload-zone"
        onDrop={handleDrop}
        onDragOver={(e) => e.preventDefault()}
        onClick={() => inputRef.current?.click()}
      >
        <input 
          ref={inputRef}
          type="file"
          accept={type === 'image' ? 'image/*' : 'video/mp4'}
          onChange={handleFileSelect}
          hidden
        />
        {uploading ? (
          <span className="spinner"></span>
        ) : (
          <>
            {type === 'image' ? <Image size={24} /> : <Video size={24} />}
            <span>{type === 'image' ? 'Перетащите фото' : 'Перетащите видео (MP4)'}</span>
          </>
        )}
      </div>
      
      {files.length > 0 && (
        <div className="file-list">
          {files.map((file, idx) => (
            <div key={file.id || idx} className="file-item">
              {type === 'image' ? (
                <img src={`data:${file.content_type};base64,${file.data}`} alt={file.filename} />
              ) : (
                <video src={file.url} />
              )}
              <button onClick={() => handleDelete(file.id)} className="file-remove">
                <X size={14} />
              </button>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
```

- [ ] **Шаг 2: Создать CSS**

```css
.file-upload {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.file-upload-zone {
  border: 2px dashed var(--border-color);
  border-radius: 8px;
  padding: 24px;
  text-align: center;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  color: var(--text-secondary);
}

.file-upload-zone:hover {
  border-color: var(--primary);
  background: rgba(var(--primary-rgb), 0.05);
}

.file-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, 80px);
  gap: 8px;
}

.file-item {
  position: relative;
  aspect-ratio: 1;
  border-radius: 8px;
  overflow: hidden;
}

.file-item img, .file-item video {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.file-remove {
  position: absolute;
  top: 4px;
  right: 4px;
  background: rgba(0,0,0,0.6);
  border: none;
  border-radius: 50%;
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: white;
}
```

- [ ] **Шаг 3: Commit**

```bash
git add web-admin/src/components/FileUpload.jsx web-admin/src/components/FileUpload.css
git commit -m "feat: создать компонент FileUpload"
```

---

### Task 7: Обновить Products.jsx

**Files:**
- Modify: `web-admin/src/pages/Products.jsx`

- [ ] **Шаг 1: Обновить состояние формы**

```javascript
const [formData, setFormData] = useState({
  name: '',
  what_is_it: '',
  brand: '',
  product_type: '',
  for_whom: '',
  purpose: '',
  skin_type: '',
  application_time: '',
  area: '',
  active_ingredient: '',
  volume: '',
  segment: '',
  composition: '',
  application_info: '',
  country: '',
  manufacturer: '',
  description: '',
  photos: [],
  has_video: false
})
```

- [ ] **Шаг 2: Добавить enums**

```javascript
const productEnums = {
  product_types: ['Крем', 'Сыворотка', 'Лосьон', 'Тоник', 'Эмульсия', 'Масло', 'Гель', 'Пилинг', 'Маска', 'Бальзам', 'Спрей', 'Мист'],
  for_whom: ['Универсальный', 'Мужчинам', 'Женщинам'],
  purposes: ['Увлажнение', 'Очищение', 'Питание', 'Антивозрастной', 'Отбеливание', 'Защита от солнца', 'Проблемная кожа', 'Восстановление', 'Матирование', 'Тонирование'],
  skin_types: ['Сухая', 'Жирная', 'Комбинированная', 'Нормальная', 'Чувствительная', 'Проблемная'],
  application_times: ['Утро', 'Вечер', 'Утро/Вечер'],
  areas: ['Лицо', 'Тело', 'Волосы', 'Губы', 'Руки', 'Веки', 'Зона вокруг глаз']
}
```

- [ ] **Шаг 3: Обновить handleSave**

```javascript
const handleSave = async (e) => {
  e.preventDefault()
  const product = {
    name: formData.name,
    what_is_it: formData.what_is_it,
    brand: formData.brand,
    product_type: formData.product_type,
    for_whom: formData.for_whom,
    purpose: formData.purpose,
    skin_type: formData.skin_type,
    application_time: formData.application_time,
    area: formData.area,
    active_ingredient: formData.active_ingredient,
    volume: formData.volume,
    segment: formData.segment,
    composition: formData.composition,
    application_info: formData.application_info,
    country: formData.country,
    manufacturer: formData.manufacturer,
    description: formData.description,
    photos: formData.photos,
    has_video: formData.has_video
  }
  // ... existing save logic
}
```

- [ ] **Шаг 4: Обновить форму в модале** - добавить все новые поля

```jsx
<div className="form-grid">
  <div className="form-group"><label>Название *</label><input name="name" value={formData.name} onChange={handleInputChange} className="input" required /></div>
  <div className="form-group"><label>Что это</label><input name="what_is_it" value={formData.what_is_it} onChange={handleInputChange} className="input" placeholder="Крем для глаз с маслом..." /></div>
  <Select label="Бренд" name="brand" value={formData.brand} onChange={handleSelectChange} options={enums.brands} />
  <Select label="Тип продукта" name="product_type" value={formData.product_type} onChange={handleSelectChange} options={productEnums.product_types} />
  <Select label="Для кого" name="for_whom" value={formData.for_whom} onChange={handleSelectChange} options={productEnums.for_whom} />
  <Select label="Назначение" name="purpose" value={formData.purpose} onChange={handleSelectChange} options={productEnums.purposes} />
  <Select label="Тип кожи" name="skin_type" value={formData.skin_type} onChange={handleSelectChange} options={productEnums.skin_types} />
  <Select label="Время нанесения" name="application_time" value={formData.application_time} onChange={handleSelectChange} options={productEnums.application_times} />
  <Select label="Область применения" name="area" value={formData.area} onChange={handleSelectChange} options={productEnums.areas} />
  <div className="form-group"><label>Действующий компонент</label><input name="active_ingredient" value={formData.active_ingredient} onChange={handleInputChange} className="input" /></div>
  <Select label="Объём" name="volume" value={formData.volume} onChange={handleSelectChange} options={enums.volumes} />
  <Select label="Сегмент" name="segment" value={formData.segment} onChange={handleSelectChange} options={enums.segments} />
  <div className="form-group" style={{ gridColumn: 'span 2' }}><label>Состав</label><textarea name="composition" value={formData.composition} onChange={handleInputChange} className="input textarea" rows="4" /></div>
  <div className="form-group" style={{ gridColumn: 'span 2' }}><label>Применение</label><textarea name="application_info" value={formData.application_info} onChange={handleInputChange} className="input textarea" rows="3" /></div>
  <div className="form-group"><label>Страна происхождения</label><input name="country" value={formData.country} onChange={handleInputChange} className="input" /></div>
  <div className="form-group"><label>Изготовитель</label><input name="manufacturer" value={formData.manufacturer} onChange={handleInputChange} className="input" /></div>
  <div className="form-group" style={{ gridColumn: 'span 2' }}><label>Описание</label><textarea name="description" value={formData.description} onChange={handleInputChange} className="input textarea" rows="3" /></div>
</div>
```

- [ ] **Шаг 5: Commit**

```bash
git add web-admin/src/pages/Products.jsx
git commit -m "feat: обновить форму Products с новыми полями"
```

---

### Task 8: Тестирование

- [ ] **Шаг 1: Запустить бэкенд**

```bash
cd backend/api && python -m uvicorn main:app --reload
```

- [ ] **Шаг 2: Запустить фронтенд**

```bash
cd web-admin && npm run dev
```

- [ ] **Шаг 3: Проверить создание продукта**

- [ ] **Шаг 4: Проверить загрузку фото**

- [ ] **Шаг 5: Проверить загрузку видео**

---

## Итог

**Коммитов: 7**
1. Расширенная модель Product
2. Миграция БД
3. Обновленный ProductService
4. Роуты для файлов
5. Фронтенд API
6. Компонент FileUpload
7. Форма Products.jsx
