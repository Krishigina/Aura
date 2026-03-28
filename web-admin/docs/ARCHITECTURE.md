# Aura Web Admin Panel - Техническая документация

## Содержание

1. [Технологический стек](#технологический-стек)
2. [Структура проекта](#структура-проекта)
3. [Архитектурные принципы](#архитектурные-принципы)
4. [Принципы программирования](#принципы-программирования)
5. [CRUD операции](#crud-операции)
6. [Работа с базой данных](#работа-с-базой-данных)
7. [Аутентификация и авторизация](#аутентификация-и-авторизация)
8. [UI/UX принципы](#uiux-принципы)
9. [Поток данных](#поток-данных)

---

## Технологический стек

### Frontend
- **React 18** - JavaScript фреймворк для построения пользовательского интерфейса
- **React Router v6** - Маршрутизация между страницами
- **Vite** - Сборщик проекта (быстрая разработка и продакшн сборка)
- **Lucide React** - Иконки
- **Recharts** - Графики и диаграммы

### Backend (FastAPI)
- **Python 3.12** - Среда выполнения Python
- **FastAPI** - Современный веб-фреймворк для API
- **PostgreSQL** - Реляционная база данных
- **asyncpg** - Асинхронный PostgreSQL клиент для Python
- **Pydantic** - Валидация данных и схемы
- **Uvicorn** - ASGI сервер

### Контейнеризация
- **Docker** - Контейнеризация приложения
- **Docker Compose** - Оркестрация сервисов

---

## Структура проекта

```
web-admin/
├── src/
│   ├── api/
│   │   └── index.js          # HTTP клиент для API вызовов
│   │
│   ├── components/
│   │   ├── Layout/
│   │   │   ├── Layout.jsx    # Основной макет (sidebar + контент)
│   │   │   └── Layout.css
│   │   ├── Select.jsx       # Кастомный компонент выпадающего списка
│   │   ├── Select.css
│   │   └── DictionaryPanel.jsx  # Компонент для справочников
│   │
│   ├── context/
│   │   ├── AuthContext.jsx  # Контекст аутентификации
│   │   ├── ThemeContext.jsx  # Контекст темы (dark/light)
│   │   └── ToastContext.jsx  # Контекст уведомлений (toasts)
│   │
│   ├── pages/
│   │   ├── Dashboard.jsx     # Главная страница со статистикой
│   │   ├── Products.jsx      # Управление продуктами
│   │   ├── Procedures.jsx    # Управление процедурами
│   │   ├── Content.jsx       # Управление контентом/статьями
│   │   ├── Users.jsx         # Управление пользователями
│   │   ├── Dictionaries.jsx  # Управление справочниками
│   │   ├── Reports.jsx       # Отчеты и аналитика
│   │   ├── Settings.jsx      # Настройки системы
│   │   └── Login.jsx         # Страница входа
│   │
│   ├── App.jsx               # Корневой компонент с роутингом
│   ├── main.jsx              # Точка входа
│   └── index.css             # Глобальные стили
│
├── Dockerfile                 # Конфигурация Docker
├── docker-compose.yml        # Оркестрация сервисов
├── vite.config.js            # Конфигурация Vite
└── package.json              # Зависимости

backend/api/
├── main.py                  # FastAPI сервер с API endpoints
├── requirements.txt         # Python зависимости
└── Dockerfile
```

---

## Архитектурные принципы

### 1. Компонентный подход (Component-Based Architecture)

Приложение построено на основе React компонентов. Каждая страница - это отдельный компонент, который:
- Отвечает за свою логику и состояние
- Импортирует необходимые зависимости
- Содержит связанные стили в CSS файле

**Пример структуры страницы:**
```jsx
export default function Products() {
  // Состояние (state)
  const [products, setProducts] = useState([])
  
  // Загрузка данных при монтировании
  useEffect(() => { loadData() }, [])
  
  // Обработчики событий
  const handleSave = async () => { ... }
  
  // Рендеринг
  return (
    <div className="products-page">
      {/* UI компоненты */}
    </div>
  )
}
```

### 2. Разделение ответственности (Separation of Concerns)

- **API слой** (`api/index.js`) - только HTTP запросы
- **Контексты** - управление глобальным состоянием
- **Компоненты** - только презентационная логика
- **Страницы** - бизнес-логика конкретных разделов

### 3. Централизованное управление состоянием

Используются React Context для глобального состояния:
- `AuthContext` - пользователь и права доступа
- `ThemeContext` - тема оформления
- `ToastContext` - уведомления

### 4. Согласованность данных (Data Consistency)

При загрузке данных используется паттерн **Optimistic Updates**:
```jsx
// 1. Обновляем UI сразу
setProducts(prev => [...prev, newProduct])
// 2. Потом отправляем запрос на сервер
try {
  await productsApi.create(data)
} catch (err) {
  // При ошибке откатываем изменения
  setProducts(prev => prev.filter(p => p.id !== newProduct.id))
}
```

---

## Принципы программирования

### 1. Функциональное программирование

- Использование `useState`, `useEffect`, `useCallback`, `useMemo`
- Чистые функции без побочных эффектов
- Неизменяемость данных (immutability)

**Пример:**
```jsx
// ❌ Неправильно - мутация массива
products.push(newProduct)

// ✅ Правильно - создание нового массива
setProducts(prev => [...prev, newProduct])
```

### 2. Раннее возвращение (Early Return)

```jsx
if (loading) {
  return <div>Загрузка...</div>
}

if (error) {
  return <div>Ошибка: {error}</div>
}

return <NormalContent />
```

### 3. Обработка ошибок

Все асинхронные операции обёрнуты в try-catch:
```jsx
try {
  setLoading(true)
  const data = await productsApi.getAll()
  setProducts(data)
} catch (err) {
  error('Ошибка загрузки данных')
  setProducts(defaultProducts)
} finally {
  setLoading(false)
}
```

### 4. Отсутствие магических чисел и строк

```jsx
// ❌ Магические значения
if (category === 'all') { ... }

// ✅ Понятные константы
const CATEGORY_ALL = 'Все'
if (category === CATEGORY_ALL) { ... }
```

---

## CRUD операции

### Общий паттерн CRUD в приложении

Каждая сущность (Products, Procedures, Users и т.д.) следует единому паттерну:

```
┌─────────────────────────────────────────────────────────────┐
│                      СТРАНИЦА                               │
│  ┌──────────────┐    ┌──────────────┐    ┌─────────────┐  │
│  │   LOAD DATA   │    │   ADD/EDIT    │    │   DELETE    │  │
│  │  useEffect    │    │    Modal      │    │    Modal    │  │
│  └──────┬───────┘    └──────┬───────┘    └──────┬──────┘  │
│         │                   │                    │          │
│         ▼                   ▼                    ▼          │
│  ┌──────────────────────────────────────────────────────┐    │
│  │              API LAYER (api/index.js)               │    │
│  │   productsApi.getAll() / create() / update()      │    │
│  └──────────────────────────┬───────────────────────────┘    │
│                             │                                │
└─────────────────────────────┼────────────────────────────────┘
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                    BACKEND API                              │
│  FastAPI server + PostgreSQL                                │
└─────────────────────────────────────────────────────────────┘
```

### Products - Пример CRUD

#### READ (Чтение)
```jsx
// 1. Вызов API при монтировании компонента
useEffect(() => {
  loadData()
}, [])

const loadData = async () => {
  const productsData = await productsApi.getAll()
  setProducts(productsData)
}

// 2. Фильтрация на клиенте
const filteredProducts = products.filter(product => 
  product.name.toLowerCase().includes(search.toLowerCase())
)
```

#### CREATE (Создание)
```jsx
const handleSave = async () => {
  const newProduct = { name, brand, category, ... }
  const created = await productsApi.create(newProduct)
  setProducts(prev => [created, ...prev])
  success('Продукт добавлен')
}
```

#### UPDATE (Обновление)
```jsx
const handleEdit = (product) => {
  setEditingProduct(product)
  setFormData({ ...product })
}

const handleSave = async () => {
  const updated = await productsApi.update(id, formData)
  setProducts(prev => prev.map(p => p.id === id ? updated : p))
  success('Продукт обновлён')
}
```

#### DELETE (Удаление)
```jsx
const handleDelete = (product) => {
  setDeleteModal(product)  // Показать модальное окно подтверждения
}

const confirmDelete = async () => {
  await productsApi.delete(id)
  setProducts(prev => prev.filter(p => p.id !== id))
  success('Продукт удалён')
}
```

### API Layer

```javascript
// api/index.js
export const productsApi = {
  getAll:    () => request('/products'),           // GET
  create:    (data) => request('/products', {      // POST
    method: 'POST', 
    body: JSON.stringify(data) 
  }),
  update:    (id, data) => request(`/products/${id}`, {  // PUT
    method: 'PUT', 
    body: JSON.stringify(data) 
  }),
  delete:    (id) => request(`/products/${id}`, {  // DELETE
    method: 'DELETE' 
  }),
}
```

---

## Работа с базой данных

### Структура базы данных PostgreSQL

```
┌────────────────────────────────────────────────────────────────┐
│                     TABLES                                     │
├────────────────────────────────────────────────────────────────┤
│                                                                │
│  products           - Косметические продукты                   │
│  procedures         - Салоные процедуры                        │
│  content            - Статьи/контент                          │
│  users              - Пользователи системы                     │
│                                                                │
│  ── DICTIONARIES (справочники) ──                             │
│  brands             - Бренды                                  │
│  categories         - Категории продуктов                      │
│  segments           - Сегменты (люкс, бюджетный и т.д.)       │
│  volumes            - Объёмы                                   │
│  procedure_categories - Категории процедур                      │
│  content_categories - Категории контента                       │
│  user_roles         - Роли пользователей                       │
│  skin_types         - Типы кожи                                │
│                                                                │
└────────────────────────────────────────────────────────────────┘
```

### Примеры SQL запросов

#### Products таблица:
```sql
CREATE TABLE products (
  id SERIAL PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  brand VARCHAR(255),
  category VARCHAR(255),
  description TEXT,
  images TEXT[],           -- Массив URL изображений
  volume VARCHAR(50),
  segment VARCHAR(50),
  created_at TIMESTAMP DEFAULT NOW()
);
```

#### Справочники (одинаковая структура):
```sql
CREATE TABLE brands (
  id SERIAL PRIMARY KEY,
  value VARCHAR(255) NOT NULL UNIQUE
);
```

### Backend API Endpoints

```javascript
// server.js

// PRODUCTS
GET    /api/products           // Получить все продукты
POST   /api/products           // Создать продукт
PUT    /api/products/:id       // Обновить продукт
DELETE /api/products/:id       // Удалить продукт

// DICTIONARIES
GET    /api/dictionaries/:key  // Получить значения справочника
POST   /api/dictionaries/:key  // Добавить значение
PUT    /api/dictionaries/:key  // Обновить значение
DELETE /api/dictionaries/:key/:value  // Удалить значение

// PROCEDURES
GET    /api/procedures
POST   /api/procedures
PUT    /api/procedures/:id
DELETE /api/procedures/:id

// CONTENT
GET    /api/content
POST   /api/content
PUT    /api/content/:id
DELETE /api/content/:id

// USERS
GET    /api/users
POST   /api/users
PUT    /api/users/:id
DELETE /api/users/:id
```

### Поток данных от БД к UI

```
PostgreSQL → FastAPI → HTTP Response → React State → UI
    ↓
1. SQL запрос в server.js
2. Результат в JSON формате
3. fetch() в api/index.js
4. useState() в компоненте
5. Рендеринг JSX
```

---

## Аутентификация и авторизация

### AuthContext

```jsx
const { user, login, logout, hasPermission } = useAuth()

// user: { id, name, email, role, permissions: [] }
// role: 'admin' | 'manager' | 'cosmetologist' | 'user'
// permissions: ['products', 'users', 'dictionaries', ...]
```

### Проверка прав доступа

```jsx
// Только для админа
const canManageEnums = user?.role === 'admin'

// Проверка конкретного разрешения
const canEdit = hasPermission('products')
```

### Protected Routes

```jsx
function ProtectedRoute({ children }) {
  const { user, loading } = useAuth()
  
  if (loading) return <Spinner />
  if (!user) return <Navigate to="/login" />
  
  return children
}
```

---

## UI/UX принципы

### 1. Glassmorphism дизайн

Использование полупрозрачных фонов с размытием:
```css
.glass-card {
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.2);
}
```

### 2. Цветовая палитра

```
Mint Green:   #A7F3D0  (основной акцент)
Pink:         #FB6FE8  (вторичный акцент)
Lavender:     #E0C3FC  (декоративный)
```

### 3. Тёмная/светлая тема

```jsx
// Переключение через ThemeContext
<button onClick={toggleTheme}>
  {theme === 'light' ? <Moon /> : <Sun />}
</button>

// CSS переменные адаптируются автоматически
[data-theme="dark"] {
  --color-surface: #1a1a1a;
  --color-on-surface: #ffffff;
}
```

### 4. Адаптивные модальные окна

```jsx
<div className="modal-overlay" onClick={onClose}>
  <div className="modal glass-card" onClick={e => e.stopPropagation()}>
    {/* Контент */}
  </div>
</div>
```

### 5. Визуальная обратная связь

- **Toasts** - успешные/ошибочные операции
- **Loading states** - индикация загрузки
- **Confirm dialogs** - подтверждение удаления
- **Hover effects** - интерактивность

---

## Поток данных

### Полная схема потока данных

```
┌─────────────────────────────────────────────────────────────────────────┐
│                            FRONTEND                                      │
│                                                                          │
│  ┌─────────────┐     ┌─────────────┐     ┌─────────────┐               │
│  │   Pages     │────▶│  API Layer  │────▶│   Context   │               │
│  │ (Products,  │     │ (index.js)  │     │ (Auth,      │               │
│  │  Users...)  │     │             │     │  Toast)     │               │
│  └─────────────┘     └──────┬──────┘     └─────────────┘               │
│         ▲                    │                                          │
│         │                    ▼                                          │
│  ┌─────────────┐     ┌─────────────┐                                   │
│  │  Components │◀────│   fetch()   │                                   │
│  │  (Layout,   │     │             │                                   │
│  │   Select)   │     └──────┬──────┘                                   │
│  └─────────────┘            │                                          │
└─────────────────────────────┼──────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                            BACKEND                                      │
│                                                                          │
│  ┌─────────────┐     ┌─────────────┐     ┌─────────────┐               │
│  │  FastAPI    │────▶│   Routes    │────▶│  asyncpg    │               │
│  │  Server     │     │  (CRUD)     │     │  (queries)  │               │
│  └─────────────┘     └─────────────┘     └──────┬──────┘               │
│                                                  │                       │
└──────────────────────────────────────────────────┼───────────────────────┘
                                                   │
                                                   ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                         DATABASE                                        │
│                                                                          │
│   PostgreSQL (aura_db)                                                  │
│   ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐                   │
│   │ products │ │procedures│ │  content │ │  users   │                   │
│   └──────────┘ └──────────┘ └──────────┘ └──────────┘                   │
│   ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐                   │
│   │ brands   │ │categories│ │ segments │ │ volumes  │ ... (dictionaries) │
│   └──────────┘ └──────────┘ └──────────┘ └──────────┘                   │
└─────────────────────────────────────────────────────────────────────────┘
```

### Пример асинхронного потока

```jsx
// 1. Пользователь нажимает "Сохранить"
<button onClick={handleSave}>Сохранить</button>

// 2. Обработчик формирует данные
const handleSave = async () => {
  const data = { name, brand, category }
  
  // 3. Вызов API
  await productsApi.create(data)
  
  // 4. Обновление состояния
  setProducts(prev => [...prev, created])
  
  // 5. Показ уведомления
  success('Продукт добавлен')
  
  // 6. Закрытие модального окна
  setShowModal(false)
}
```

---

## Развертывание

### Docker Compose

```yaml
services:
  web-admin:
    build: ./web-admin
    ports:
      - "5173:5173"
  
  admin-api:
    build: ./backend/api
    ports:
      - "3001:3001"
    environment:
      - DB_HOST=postgres
      - DB_NAME=aura
  
  postgres:
    image: postgres:15-alpine
    volumes:
      - postgres_data:/var/lib/postgresql/data
```

### Запуск

```bash
# Запуск всех сервисов
docker compose up -d

# Проверка статуса
docker compose ps

# Логи
docker compose logs -f
```

---

## Заключение

Aura Web Admin построен с использованием современных принципов разработки:

- **Модульность** - каждый компонент отвечает за свою область
- **Чистота кода** - понятные имена, последовательный стиль
- **Обработка ошибок** - всё покрыто try-catch
- **UI/UX** - согласованный дизайн, обратная связь
- **Безопасность** - аутентификация, авторизация, защищённые роуты

Эти принципы обеспечивают масштабируемость и поддерживаемость проекта.
