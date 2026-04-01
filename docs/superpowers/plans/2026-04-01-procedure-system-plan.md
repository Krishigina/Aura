# Система процедур косметологии - План реализации

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Создать систему управления косметологическими процедурами с wizard-интерфейсом из 7 шагов, справочниками и карточками процедур.

**Architecture:** Backend расширяет модель Procedure, добавляет все необходимые поля и справочники. Frontend создаёт wizard-компонент для пошагового ввода с использованием существующих Select/SelectMulti компонентов.

**Tech Stack:** FastAPI, PostgreSQL, React, Vite

---

## Файловая структура

### Backend (создание/модификация)
- `backend/api/main.py` - добавить новые таблицы справочников
- `backend/api/app/models/procedure.py` - расширить модель Procedure
- `backend/api/app/services/product_service.py` - добавить ProcedureService (или новый service)
- `backend/api/app/api/routes/procedures.py` - расширить API endpoints

### Frontend (создание/модификация)
- `web-admin/src/pages/Procedures.jsx` - обновить страницу процедур
- `web-admin/src/components/ProcedureWizard.jsx` - создать wizard компонент (новый)
- `web-admin/src/api/index.js` - добавить API методы для справочников процедур

---

## План реализации

### Этап 1: Backend - Расширение модели и БД

#### Task 1: Добавить справочники в БД

**Files:**
- Modify: `backend/api/main.py`
- Modify: `backend/api/app/database.py`

- [ ] **Step 1: Добавить CREATE TABLE для справочников процедур в main.py**

Добавить после существующих таблиц:
```python
# Справочники для процедур
CREATE TABLE IF NOT EXISTS procedure_method_types (
    id SERIAL PRIMARY KEY,
    value VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS procedure_durations (
    id SERIAL PRIMARY KEY,
    value VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS procedure_equipment (
    id SERIAL PRIMARY KEY,
    value VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS procedure_zones (
    id SERIAL PRIMARY KEY,
    value VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS procedure_effects (
    id SERIAL PRIMARY KEY,
    value VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS procedure_problems (
    id SERIAL PRIMARY KEY,
    value VARCHAR(255) NOT NULL UNIQUE
);
```

- [ ] **Step 2: Расширить таблицу procedures новыми полями**

```python
ALTER TABLE procedures ADD COLUMN IF NOT EXISTS direction VARCHAR(50);
ALTER TABLE procedures ADD COLUMN IF NOT EXISTS method_type VARCHAR(100);
ALTER TABLE procedures ADD COLUMN IF NOT EXISTS duration VARCHAR(50);
ALTER TABLE procedures ADD COLUMN IF NOT EXISTS equipment VARCHAR(255);
ALTER TABLE procedures ADD COLUMN IF NOT EXISTS zones JSONB;
ALTER TABLE procedures ADD COLUMN IF NOT EXISTS effects JSONB;
ALTER TABLE procedures ADD COLUMN IF NOT EXISTS problems JSONB;
ALTER TABLE procedures ADD COLUMN IF NOT EXISTS procedure_about TEXT;
ALTER TABLE procedures ADD COLUMN IF NOT EXISTS advantages TEXT;
ALTER TABLE procedures ADD COLUMN IF NOT EXISTS indications TEXT;
ALTER TABLE procedures ADD COLUMN IF NOT EXISTS principle TEXT;
ALTER TABLE procedures ADD COLUMN IF NOT EXISTS how_it_goes TEXT;
ALTER TABLE procedures ADD COLUMN IF NOT EXISTS for_whom TEXT;
ALTER TABLE procedures ADD COLUMN IF NOT EXISTS problems_solved TEXT;
ALTER TABLE procedures ADD COLUMN IF NOT EXISTS contraindications_full TEXT;
ALTER TABLE procedures ADD COLUMN IF NOT EXISTS preparation TEXT;
ALTER TABLE procedures ADD COLUMN IF NOT EXISTS recommended_course TEXT;
ALTER TABLE procedures ADD COLUMN IF NOT EXISTS rehabilitation TEXT;
ALTER TABLE procedures ADD COLUMN IF NOT EXISTS post_care TEXT;
ALTER TABLE procedures ADD COLUMN IF NOT EXISTS side_effects TEXT;
ALTER TABLE procedures ADD COLUMN IF NOT EXISTS photos JSONB;
```

- [ ] **Step 3: Заполнить базовые значения справочников**

```python
# Тип методики
_seed_procedure_dict(cursor, "procedure_method_types", [
    "Лазер", "УЗ-терапия", "RF-лифтинг", "Микротоки", "Криотерапия",
    "Мезотерапия", "Биоревитализация", "Ботокс", "Филлеры", "Пилинг",
    "Электропорация", "Вакуумный массаж", "Светотерапия"
])

# Продолжительность
_seed_procedure_dict(cursor, "procedure_durations", [
    "30 минут", "45 минут", "1 час", "1.5 часа", "2 часа"
])

# Зоны
_seed_procedure_dict(cursor, "procedure_zones", [
    "Лицо", "Шея", "Декольте", "Руки", "Тело", "Веки", "Губы", "Живот", "Бедра"
])

# Желаемый эффект
_seed_procedure_dict(cursor, "procedure_effects", [
    "Омоложение", "Лифтинг", "Увлажнение", "Осветление", "Липолиз",
    "Укрепление", "Очищение", "Тонизация", "Anti-age"
])

# Решаемые проблемы
_seed_procedure_dict(cursor, "procedure_problems", [
    "Морщины", "Акне", "Пигментация", "Целлюлит", "Растяжки",
    "Сухость", "Жирность", "Расширенные поры", "Шрамы", "Веснушки",
    "Купероз", "Отечность", "Тусклый цвет"
])
```

- [ ] **Step 4: Commit**
```bash
git add backend/api/main.py backend/api/app/database.py
git commit -m "feat: add procedure dictionaries tables and fields"
```

---

#### Task 2: Расширить модель Procedure

**Files:**
- Modify: `backend/api/app/models/procedure.py`

- [ ] **Step 1: Обновить Pydantic модель**

```python
from pydantic import BaseModel
from typing import Optional, List

class ProcedureBase(BaseModel):
    name: str
    direction: Optional[str] = None
    method_type: Optional[str] = None
    duration: Optional[str] = None
    equipment: Optional[str] = None
    zones: Optional[List[str]] = []
    effects: Optional[List[str]] = []
    problems: Optional[List[str]] = []
    
    # Text fields
    description: Optional[str] = None
    procedure_about: Optional[str] = None
    advantages: Optional[str] = None
    indications: Optional[str] = None
    principle: Optional[str] = None
    how_it_goes: Optional[str] = None
    for_whom: Optional[str] = None
    problems_solved: Optional[str] = None
    contraindications_full: Optional[str] = None
    preparation: Optional[str] = None
    recommended_course: Optional[str] = None
    rehabilitation: Optional[str] = None
    post_care: Optional[str] = None
    side_effects: Optional[str] = None
    
    photos: Optional[List[dict]] = []

class ProcedureCreate(ProcedureBase):
    pass

class Procedure(ProcedureBase):
    id: int
    created_at: Optional[str] = None

    class Config:
        from_attributes = True
```

- [ ] **Step 2: Commit**
```bash
git add backend/api/app/models/procedure.py
git commit -m "feat: extend Procedure model with all fields"
```

---

#### Task 3: Расширить API endpoints

**Files:**
- Modify: `backend/api/app/api/routes/procedures.py`

- [ ] **Step 1: Добавить endpoints для справочников**

```python
@router.get("/dictionaries/method-types")
def get_method_types():
    return ProcedureService.get_dictionary("procedure_method_types")

@router.get("/dictionaries/durations")
def get_durations():
    return ProcedureService.get_dictionary("procedure_durations")

@router.get("/dictionaries/equipment")
def get_equipment():
    return ProcedureService.get_dictionary("procedure_equipment")

@router.get("/dictionaries/zones")
def get_zones():
    return ProcedureService.get_dictionary("procedure_zones")

@router.get("/dictionaries/effects")
def get_effects():
    return ProcedureService.get_dictionary("procedure_effects")

@router.get("/dictionaries/problems")
def get_problems():
    return ProcedureService.get_dictionary("procedure_problems")

@router.post("/dictionaries/{dict_type}")
def add_dictionary_value(dict_type: str, value: str):
    return ProcedureService.add_dictionary_value(dict_type, value)
```

- [ ] **Step 2: Добавить Photo endpoints**

```python
@router.post("/{procedure_id}/photos")
async def upload_photo(procedure_id: int, file: UploadFile = File(...)):
    # Similar to products - save to disk
    pass

@router.delete("/{procedure_id}/photos/{photo_id}")
def delete_photo(procedure_id: int, photo_id: int):
    pass
```

- [ ] **Step 3: Commit**
```bash
git add backend/api/app/api/routes/procedures.py
git commit -m "feat: add procedure dictionary API endpoints"
```

---

### Этап 2: Frontend - API и базовые компоненты

#### Task 4: Добавить API методы

**Files:**
- Modify: `web-admin/src/api/index.js`

- [ ] **Step 1: Добавить proceduresApi**

```javascript
export const proceduresApi = {
  getAll: () => request('/procedures'),
  getById: (id) => request(`/procedures/${id}`),
  create: (data) => request('/procedures', { method: 'POST', body: JSON.stringify(data) }),
  update: (id, data) => request(`/procedures/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
  delete: (id) => request(`/procedures/${id}`, { method: 'DELETE' }),
  
  getDictionaries: () => request('/procedures/dictionaries/all'),
  getMethodTypes: () => request('/procedures/dictionaries/method-types'),
  getDurations: () => request('/procedures/dictionaries/durations'),
  getEquipment: () => request('/procedures/dictionaries/equipment'),
  getZones: () => request('/procedures/dictionaries/zones'),
  getEffects: () => request('/procedures/dictionaries/effects'),
  getProblems: () => request('/procedures/dictionaries/problems'),
  
  uploadPhoto: async (procedureId, file) => {
    const formData = new FormData()
    formData.append('file', file)
    const response = await fetch(`${API_URL}/procedures/${procedureId}/photos`, {
      method: 'POST',
      headers: { 'Accept': 'application/json' },
      body: formData
    })
    if (!response.ok) throw new Error('Upload failed')
    return response.json()
  },
  
  deletePhoto: async (procedureId, photoId) => {
    const response = await fetch(`${API_URL}/procedures/${procedureId}/photos/${photoId}`, {
      method: 'DELETE'
    })
    if (!response.ok) throw new Error('Delete failed')
    return response.json()
  },
  
  getPhotoUrl: (procedureId, photoId) => `${API_URL}/procedures/${procedureId}/photos/${photoId}`,
}
```

- [ ] **Step 2: Commit**
```bash
git add web-admin/src/api/index.js
git commit -m "feat: add procedures API methods"
```

---

### Этап 3: Frontend - Wizard компонент

#### Task 5: Создать ProcedureWizard

**Files:**
- Create: `web-admin/src/components/ProcedureWizard.jsx`
- Create: `web-admin/src/components/ProcedureWizard.css`

- [ ] **Step 1: Создать базовую структуру wizard**

```jsx
// 7 шагов wizard
const STEPS = [
  { id: 'basic', title: 'Базовая информация', fields: ['name', 'direction', 'method_type'] },
  { id: 'description', title: 'Описание', fields: ['description', 'procedure_about', 'principle', 'how_it_goes'] },
  { id: 'indications', title: 'Показания', fields: ['indications', 'for_whom', 'problems'] },
  { id: 'effect', title: 'Эффект', fields: ['effects', 'advantages'] },
  { id: 'preparation', title: 'Подготовка', fields: ['preparation', 'recommended_course'] },
  { id: 'rehabilitation', title: 'Реабилитация', fields: ['rehabilitation', 'post_care', 'side_effects', 'contraindications_full'] },
  { id: 'equipment', title: 'Оборудование и фото', fields: ['equipment', 'zones', 'photos'] },
]

// Компонент с:
// - Прогресс-бар (шаги 1-7)
// - Навигация (назад/вперед/сохранить)
// - Формы на каждом шаге
// - Сохранение данных между шагами
```

- [ ] **Step 2: Реализовать каждый шаг**

- Шаг 1: name (input), direction (select), method_type (select)
- Шаг 2-5: текстовые поля (textarea)
- Шаг 3: мульти-селект для problems
- Шаг 4: мульти-селект для effects
- Шаг 7: select для equipment, мульти-селект для zones, загрузка фото

- [ ] **Step 3: Добавить стили**

- Progress bar с индикацией текущего шага
- Кнопки навигации
- Анимация переходов между шагами

- [ ] **Step 4: Commit**
```bash
git add web-admin/src/components/ProcedureWizard.jsx web-admin/src/components/ProcedureWizard.css
git commit -m "feat: create ProcedureWizard component with 7 steps"
```

---

### Этап 4: Frontend - Обновление страницы Procedures

#### Task 6: Обновить страницу процедур

**Files:**
- Modify: `web-admin/src/pages/Procedures.jsx`

- [ ] **Step 1: Добавить загрузку справочников**

```javascript
const [dictionaries, setDictionaries] = useState({
  method_types: [],
  durations: [],
  equipment: [],
  zones: [],
  effects: [],
  problems: []
})

useEffect(() => {
  const loadDicts = async () => {
    const [method_types, durations, equipment, zones, effects, problems] = await Promise.all([
      proceduresApi.getMethodTypes(),
      proceduresApi.getDurations(),
      proceduresApi.getEquipment(),
      proceduresApi.getZones(),
      proceduresApi.getEffects(),
      proceduresApi.getProblems(),
    ])
    setDictionaries({ method_types, durations, equipment, zones, effects, problems })
  }
  loadDicts()
}, [])
```

- [ ] **Step 2: Обновить карточки процедур**

- Добавить отображение направления
- Показать только заполненные текстовые поля (compact view)
- Фото-галерея

- [ ] **Step 3: Интегрировать Wizard**

- Кнопка "Добавить процедуру" открывает wizard
- Редактирование открывает wizard с данными

- [ ] **Step 4: Commit**
```bash
git add web-admin/src/pages/Procedures.jsx
git commit -m "feat: update Procedures page with wizard integration"
```

---

### Этап 5: Тестирование и полировка

#### Task 7: Тестирование

- [ ] **Step 1: Test CRUD operations**
- [ ] **Step 2: Test wizard navigation**
- [ ] **Step 3: Test photo upload**
- [ ] **Step 4: Test dictionary add**

---

## Рекомендуемый порядок выполнения

1. Backend: БД и модель → API
2. Frontend: API → Wizard → Страница
3. Тестирование и полировка

---

**Plan created:** docs/superpowers/plans/2026-04-01-procedure-system-plan.md