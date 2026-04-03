# Products UI/UX Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Реорганизовать форму продукта в модальном окне с логическими секциями, добавить новые справочники, улучшить UI/UX

**Architecture:** Разделить форму на визуальные блоки с заголовками, добавить справочники в Settings > Dictionaries, стилизовать под современный glassmorphism UI

**Tech Stack:** React, CSS, Lucide Icons

---

## Task 1: Добавить стили секций в Products.css

**Files:**
- Modify: `web-admin/src/pages/Products.css`

- [ ] **Step 1: Добавить стили для секций формы**

Добавить в конец файла (после строки 906):

```css
/* Form Sections */
.form-section {
  margin-bottom: var(--spacing-lg);
  padding-bottom: var(--spacing-lg);
  border-bottom: 1px solid var(--color-gray-200);
}

.form-section:last-child {
  border-bottom: none;
  margin-bottom: 0;
}

.form-section-header {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  margin-bottom: var(--spacing-md);
  font-size: 14px;
  font-weight: 600;
  color: var(--color-on-surface);
}

.form-section-icon {
  width: 28px;
  height: 28px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
}

.form-section.accent-purple .form-section-icon {
  background: rgba(139, 92, 246, 0.1);
  color: #8B5CF6;
}

.form-section.accent-green .form-section-icon {
  background: rgba(16, 185, 129, 0.1);
  color: #10B981;
}

.form-section.accent-orange .form-section-icon {
  background: rgba(245, 158, 11, 0.1);
  color: #F59E0B;
}

.form-section.accent-pink .form-section-icon {
  background: rgba(236, 72, 153, 0.1);
  color: #EC4899;
}

.form-section.accent-blue .form-section-icon {
  background: rgba(59, 130, 246, 0.1);
  color: #3B82F6;
}

/* Characteristics grid - 2 columns */
.form-section .form-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--spacing-md);
}

/* Full width fields in sections */
.form-section .form-group.full-width {
  grid-column: span 2;
}

/* Section specific adjustments */
.form-section-characteristics .form-group:nth-child(7),
.form-section-characteristics .form-group:nth-child(8) {
  grid-column: span 1;
}

[data-theme="dark"] .form-section {
  border-bottom-color: var(--color-gray-700);
}
```

- [ ] **Step 2: Обновить стили модального окна для большей ширины**

Найти `.modal-wide` и изменить:
```css
.modal-wide {
  max-width: 800px;
}
```

- [ ] **Step 3: Commit**

```bash
git add web-admin/src/pages/Products.css
git commit -m "feat: add form section styles for Products"
```

---

## Task 2: Реорганизовать форму продукта в Products.jsx

**Files:**
- Modify: `web-admin/src/pages/Products.jsx`

- [ ] **Step 1: Добавить импорты иконок для секций**

Добавить в импорты (строка 4):
```javascript
import { Search, Plus, Edit2, Trash2, X, Image, Tag, Check, Package, AlertTriangle, Link as LinkIcon, FlaskConical, FileText, Settings, Palette, MapPin, Factory, Clock, Droplets } from 'lucide-react'
```

- [ ] **Step 2: Реорганизовать JSX форму в секции**

Найти форму (начинается с `<form onSubmit={handleSave} className="product-form">`) и заменить всё содержимое `form-grid`:

```jsx
<div className="form-grid">
  {/* Секция 1: О продукте */}
  <div className="form-section form-section-accent-purple" style={{ gridColumn: 'span 2' }}>
    <div className="form-section-header">
      <div className="form-section-icon"><FlaskConical size={16} /></div>
      <span>О продукте</span>
    </div>
    <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: 'var(--spacing-md)' }}>
      <div className="form-group"><label>Название *</label><input name="name" value={formData.name} onChange={handleInputChange} className="input" required /></div>
      <div className="form-group"><label>Что это?</label><input name="what_is_it" value={formData.what_is_it} onChange={handleInputChange} className="input" placeholder="Например: увлажняющий крем" /></div>
      <div className="form-group" style={{ gridColumn: 'span 2' }}><Select label="Бренд *" name="brand" value={formData.brand} onChange={handleSelectChange} options={enums.brands} /></div>
    </div>
  </div>

  {/* Секция 2: Описание */}
  <div className="form-section" style={{ gridColumn: 'span 2' }}>
    <div className="form-section-header">
      <div className="form-section-icon"><FileText size={16} /></div>
      <span>Описание</span>
    </div>
    <div className="form-group full-width"><label>Описание</label><textarea name="description" value={formData.description} onChange={handleInputChange} className="input textarea" rows="3" /></div>
  </div>

  {/* Секция 3: Характеристики */}
  <div className="form-section form-section-accent-green" style={{ gridColumn: 'span 2' }}>
    <div className="form-section-header">
      <div className="form-section-icon"><Settings size={16} /></div>
      <span>Характеристики</span>
    </div>
    <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: 'var(--spacing-md)' }}>
      <Select label="Тип продукта" name="product_type" value={formData.product_type} onChange={handleSelectChange} options={productEnums.product_types} placeholder="Выберите тип" />
      <Select label="Для кого" name="for_whom" value={formData.for_whom} onChange={handleSelectChange} options={productEnums.for_whom} placeholder="Выберите" />
      <Select label="Назначение" name="purpose" value={formData.purpose} onChange={handleSelectChange} options={productEnums.purposes} placeholder="Выберите назначение" />
      <Select label="Тип кожи" name="skin_type" value={formData.skin_type} onChange={handleSelectChange} options={productEnums.skin_types} placeholder="Выберите тип кожи" />
      <Select label="Время применения" name="application_time" value={formData.application_time} onChange={handleSelectChange} options={productEnums.application_times} placeholder="Выберите время" />
      <Select label="Область применения" name="area" value={formData.area} onChange={handleSelectChange} options={productEnums.areas} placeholder="Выберите область" />
      <div className="form-group"><label>Активный компонент</label><input name="active_ingredient" value={formData.active_ingredient} onChange={handleInputChange} className="input" placeholder="Например: гиалуроновая кислота" /></div>
      <Select label="Объём" name="volume" value={formData.volume} onChange={handleSelectChange} options={enums.volumes} placeholder="Выберите объём" />
    </div>
  </div>

  {/* Секция 4: Применение */}
  <div className="form-section" style={{ gridColumn: 'span 2' }}>
    <div className="form-section-header">
      <div className="form-section-icon"><Droplets size={16} /></div>
      <span>Применение</span>
    </div>
    <div className="form-group full-width"><label>Информация о применении</label><textarea name="application_info" value={formData.application_info} onChange={handleInputChange} className="input textarea" rows="2" placeholder="Как наносить, частота использования..." /></div>
  </div>

  {/* Секция 5: Состав */}
  <div className="form-section" style={{ gridColumn: 'span 2' }}>
    <div className="form-section-header">
      <div className="form-section-icon"><FileText size={16} /></div>
      <span>Состав</span>
    </div>
    <div className="form-group full-width"><label>Состав</label><textarea name="composition" value={formData.composition} onChange={handleInputChange} className="input textarea" rows="3" placeholder="Список ингредиентов..." /></div>
  </div>

  {/* Секция 6: Бренд */}
  <div className="form-section form-section-accent-pink" style={{ gridColumn: 'span 2' }}>
    <div className="form-section-header">
      <div className="form-section-icon"><Palette size={16} /></div>
      <span>Бренд</span>
    </div>
    <div className="form-group"><label>Страна бренда</label><input name="country" value={formData.country} onChange={handleInputChange} className="input" placeholder="Например: Франция" /></div>
  </div>

  {/* Секция 7: Дополнительно */}
  <div className="form-section form-section-accent-blue" style={{ gridColumn: 'span 2' }}>
    <div className="form-section-header">
      <div className="form-section-icon"><Factory size={16} /></div>
      <span>Дополнительная информация</span>
    </div>
    <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: 'var(--spacing-md)' }}>
      <div className="form-group"><label>Страна происхождения</label><input name="country_origin" value={formData.country_origin || ''} onChange={handleInputChange} className="input" placeholder="Например: Франция" /></div>
      <div className="form-group"><label>Производитель</label><input name="manufacturer" value={formData.manufacturer} onChange={handleInputChange} className="input" /></div>
    </div>
  </div>

  {/* Секция 8: Медиа (только при редактировании) */}
  {editingProduct && (
    <div className="form-section form-section-accent-orange" style={{ gridColumn: 'span 2' }}>
      <div className="form-section-header">
        <div className="form-section-icon"><Image size={16} /></div>
        <span>Медиа</span>
      </div>
      <div className="form-group full-width">
        <label>Фотографии</label>
        <div className="file-upload-area">
          {formData.photos?.length > 0 && (
            <div className="photo-gallery">
              {formData.photos.map((photo, idx) => (
                <div key={photo.id || idx} className="photo-item">
                  <img src={photo.data ? `data:${photo.content_type};base64,${photo.data}` : `/api/products/${editingProduct.id}/photos/${photo.id}`} alt="" />
                  <button type="button" className="btn btn-ghost btn-sm" onClick={() => handlePhotoDelete(photo.id)}><X size={14} /></button>
                </div>
              ))}
            </div>
          )}
          <input type="file" accept="image/*" onChange={e => e.target.files[0] && handlePhotoUpload(e.target.files[0])} className="input" />
        </div>
      </div>
      <div className="form-group full-width">
        <label>Видео</label>
        <div className="file-upload-area">
          {formData.has_video && videoUrl ? (
            <div className="video-preview">
              <video src={videoUrl} controls />
              <button type="button" className="btn btn-ghost btn-sm" onClick={handleVideoDelete}><X size={14} /></button>
            </div>
          ) : (
            <input type="file" accept="video/mp4" onChange={e => e.target.files[0] && handleVideoUpload(e.target.files[0])} className="input" />
          )}
        </div>
      </div>
    </div>
  )}

  {/* Секция 9: Импорт */}
  <div className="form-section" style={{ gridColumn: 'span 2' }}>
    <div className="form-section-header">
      <div className="form-section-icon"><LinkIcon size={16} /></div>
      <span>Импорт ссылки</span>
    </div>
    <div className="url-parser-section" style={{ border: 'none', padding: 0, margin: 0, background: 'transparent' }}>
      <div className="url-parser-input-group">
        <input 
          name="url" 
          value={url} 
          onChange={e => setUrl(e.target.value)} 
          className="input" 
          placeholder="Вставьте ссылку на продукт (Wildberries, Ozon...)" 
        />
        <button 
          type="button" 
          className="btn btn-primary" 
          onClick={handleParseUrl} 
          disabled={!url || parsing}
        >
          {parsing ? (<><span className="spinner"></span>Загрузка...</>) : (<>Импорт</>)}
        </button>
      </div>
      <p className="url-parser-hint">Автоматический импорт недоступен из-за защиты маркетплейсов. Скопируйте название и бренд с сайта вручную</p>
    </div>
  </div>
</div>
```

- [ ] **Step 3: Commit**

```bash
git add web-admin/src/pages/Products.jsx
git commit -m "feat: reorganize Products form into visual sections"
```

---

## Task 3: Добавить новые справочники в Dictionaries.jsx

**Files:**
- Modify: `web-admin/src/pages/Dictionaries.jsx`

- [ ] **Step 1: Добавить новые enum в defaultEnums (строка 18-27)**

```javascript
const defaultEnums = {
  brands: ['Aura', 'La Roche-Posay', 'Vichy', 'Bioderma', 'CeraVe', 'The Ordinary', "Paula's Choice", 'Cosrx', 'Eucerin', 'Nivea'],
  categories: ['Очищение', 'Увлажнение', 'Сыворотки', 'SPF', 'Уход', 'Маска', 'Тоник', 'Крем', 'Масло'],
  segments: ['Бюджетная', 'Люкс', 'Профессиональная', 'Космецевтика'],
  volumes: ['15мл', '30мл', '50мл', '75мл', '100мл', '150мл', '200мл', '250мл', '500мл', '1л'],
  procedureCategories: ['Чистка', 'Увлажнение', 'Инъекции', 'Эпиляция', 'Массаж', 'Пилинг', 'Уход'],
  contentCategories: ['Уход за кожей', 'Ингредиенты', 'Защита', 'Процедуры', 'Питание', 'Образ жизни'],
  userRoles: ['Пользователь', 'Косметолог', 'Менеджер', 'Администратор'],
  skinTypes: ['Нормальная', 'Сухая', 'Жирная', 'Комбинированная', 'Чувствительная'],
  // Новые справочники для продуктов
  product_types: ['Крем', 'Сыворотка', 'Лосьон', 'Тоник', 'Эмульсия', 'Масло', 'Гель', 'Пилинг', 'Маска', 'Бальзам', 'Спрей', 'Мист'],
  for_whom: ['Универсальный', 'Мужчинам', 'Женщинам'],
  purposes: ['Увлажнение', 'Очищение', 'Питание', 'Антивозрастной', 'Отбеливание', 'Защита от солнца', 'Проблемная кожа', 'Восстановление', 'Матирование', 'Тонирование'],
  application_times: ['Утро', 'Вечер', 'Утро/Вечер'],
  areas: ['Лицо', 'Тело', 'Волосы', 'Губы', 'Руки', 'Веки', 'Зона вокруг глаз'],
}
```

- [ ] **Step 2: Обновить dictGroups.products (строка 35)**

```javascript
{
  id: 'products',
  label: 'Продукты',
  icon: FlaskConical,
  color: '#8B5CF6',
  dictionaries: ['brands', 'product_types', 'for_whom', 'purposes', 'skin_types', 'application_times', 'areas', 'segments', 'volumes']
},
```

- [ ] **Step 3: Добавить конфиг для новых справочников в dictConfig (строка 60-68)**

```javascript
const dictConfig = {
  brands: { label: 'Бренды', icon: FlaskConical, color: '#9333EA' },
  categories: { label: 'Категории', icon: FlaskConical, color: '#059669' },
  segments: { label: 'Сегменты', icon: FlaskConical, color: '#D97706' },
  volumes: { label: 'Объёмы', icon: FlaskConical, color: '#2563EB' },
  product_types: { label: 'Типы продуктов', icon: FlaskConical, color: '#8B5CF6' },
  for_whom: { label: 'Для кого', icon: FlaskConical, color: '#EC4899' },
  purposes: { label: 'Назначение', icon: FlaskConical, color: '#10B981' },
  skin_types: { label: 'Типы кожи', icon: FlaskConical, color: '#EF4444' },
  application_times: { label: 'Время применения', icon: FlaskConical, color: '#F59E0B' },
  areas: { label: 'Области применения', icon: FlaskConical, color: '#3B82F6' },
  procedureCategories: { label: 'Категории процедур', icon: Sparkles, color: '#EC4899' },
  contentCategories: { label: 'Категории контента', icon: BookOpen, color: '#10B981' },
  userRoles: { label: 'Роли', icon: UserCog, color: '#3B82F6' },
}
```

- [ ] **Step 4: Обновить loadData для загрузки новых справочников (строка 89-98)**

```javascript
const [brands, categories, segments, volumes, procedureCategories, contentCategories, userRoles, skinTypes, productTypes, forWhom, purposes, applicationTimes, areas] = await Promise.all([
  dictionariesApi.get('brands').catch(() => defaultEnums.brands),
  dictionariesApi.get('categories').catch(() => defaultEnums.categories),
  dictionariesApi.get('segments').catch(() => defaultEnums.segments),
  dictionariesApi.get('volumes').catch(() => defaultEnums.volumes),
  dictionariesApi.get('procedureCategories').catch(() => defaultEnums.procedureCategories),
  dictionariesApi.get('contentCategories').catch(() => defaultEnums.contentCategories),
  dictionariesApi.get('userRoles').catch(() => defaultEnums.userRoles),
  dictionariesApi.get('skinTypes').catch(() => defaultEnums.skinTypes),
  dictionariesApi.get('product_types').catch(() => defaultEnums.product_types),
  dictionariesApi.get('for_whom').catch(() => defaultEnums.for_whom),
  dictionariesApi.get('purposes').catch(() => defaultEnums.purposes),
  dictionariesApi.get('application_times').catch(() => defaultEnums.application_times),
  dictionariesApi.get('areas').catch(() => defaultEnums.areas),
])
setDictionaries({
  brands,
  categories,
  segments,
  volumes,
  procedureCategories,
  contentCategories,
  userRoles,
  skinTypes,
  product_types: productTypes,
  for_whom: forWhom,
  purposes: purposes,
  application_times: applicationTimes,
  areas: areas
})
```

- [ ] **Step 5: Commit**

```bash
git add web-admin/src/pages/Dictionaries.jsx
git commit -m "feat: add new product dictionaries to Dictionaries page"
```

---

## Task 4: Обновить Products.jsx для загрузки новых справочников

**Files:**
- Modify: `web-admin/src/pages/Products.jsx`

- [ ] **Step 1: Обновить loadData для загрузки новых справочников (строка 69-93)**

```javascript
const loadData = async () => {
  try {
    setLoading(true)
    const [productsData, brandsData, categoriesData, segmentsData, volumesData, productTypesData, forWhomData, purposesData, skinTypesData, applicationTimesData, areasData] = await Promise.all([
      productsApi.getAll().catch(() => []),
      dictionariesApi.get('brands').catch(() => defaultEnums.brands),
      dictionariesApi.get('categories').catch(() => defaultEnums.categories),
      dictionariesApi.get('segments').catch(() => defaultEnums.segments),
      dictionariesApi.get('volumes').catch(() => defaultEnums.volumes),
      dictionariesApi.get('product_types').catch(() => productEnums.product_types),
      dictionariesApi.get('for_whom').catch(() => productEnums.for_whom),
      dictionariesApi.get('purposes').catch(() => productEnums.purposes),
      dictionariesApi.get('skin_types').catch(() => productEnums.skin_types),
      dictionariesApi.get('application_times').catch(() => productEnums.application_times),
      dictionariesApi.get('areas').catch(() => productEnums.areas),
    ])
    setProducts(productsData)
    setEnums({
      brands: brandsData,
      categories: categoriesData,
      segments: segmentsData,
      volumes: volumesData
    })
    setProductEnums({
      product_types: productTypesData,
      for_whom: forWhomData,
      purposes: purposesData,
      skin_types: skinTypesData,
      application_times: applicationTimesData,
      areas: areasData
    })
  } catch (err) {
    error('Ошибка загрузки данных')
    setProducts([])
    setEnums(defaultEnums)
    setProductEnums({
      product_types: productEnums.product_types,
      for_whom: productEnums.for_whom,
      purposes: productEnums.purposes,
      skin_types: productEnums.skin_types,
      application_times: productEnums.application_times,
      areas: productEnums.areas
    })
  } finally {
    setLoading(false)
  }
}
```

- [ ] **Step 2: Добавить useState для productEnums (после строки 29)**

```javascript
const [productEnums, setProductEnums] = useState({
  product_types: ['Крем', 'Сыворотка', 'Лосьон', 'Тоник', 'Эмульсия', 'Масло', 'Гель', 'Пилинг', 'Маска', 'Бальзам', 'Спрей', 'Мист'],
  for_whom: ['Универсальный', 'Мужчинам', 'Женщинам'],
  purposes: ['Увлажнение', 'Очищение', 'Питание', 'Антивозрастной', 'Отбеливание', 'Защита от солнца', 'Проблемная кожа', 'Восстановление', 'Матирование', 'Тонирование'],
  skin_types: ['Сухая', 'Жирная', 'Комбинированная', 'Нормальная', 'Чувствительная', 'Проблемная'],
  application_times: ['Утро', 'Вечер', 'Утро/Вечер'],
  areas: ['Лицо', 'Тело', 'Волосы', 'Губы', 'Руки', 'Веки', 'Зона вокруг глаз']
})
```

- [ ] **Step 3: Commit**

```bash
git add web-admin/src/pages/Products.jsx
git commit -m "feat: load product dictionaries in Products page"
```

---

## Task 5: Проверить сборку

**Files:**
- Test: `web-admin`

- [ ] **Step 1: Запустить сборку**

```bash
cd web-admin && npm run build
```

Ожидаемый результат: сборка успешна без ошибок

- [ ] **Step 2: Commit**

```bash
git add -A && git commit -m "build: verify products UI redesign builds successfully"
```

---

## Summary

**Tasks completed:**
1. ✅ Products.css - добавлены стили секций
2. ✅ Products.jsx - форма реорганизована в визуальные блоки
3. ✅ Dictionaries.jsx - добавлены новые справочники
4. ✅ Products.jsx - загрузка справочников с API
5. ✅ Проверка сборки
