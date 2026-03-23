# Aura - Персонализированный подбор косметических средств с ИИ

## 📋 Содержание

- [Этап 0: Анализ проекта](#этап-0-анализ-проекта)
- [Этап 1: Архитектура системы](#этап-1-архитектура-системы)
- [Этап 2: База данных](#этап-2-проектирование-базы-данных)
- [Этап 3: Backend (Kotlin)](#этап-3-backend-kotlin)
- [Этап 4: AI Service (Python)](#этап-4-ai-service-pythonfastapi)
- [Этап 5: Mobile App](#этап-5-mobile-app-kotlincompose)
- [Этап 6: Рекомендательная система](#этап-6-рекомендательная-система)

---

## ✅ СТАТУС ПРОЕКТА

### Созданные файлы

```
Aura/
├── docker-compose.yml              # Orchestration всех сервисов
├── docs/
│   └── ARCHITECTURE.md             # Архитектурная документация
│
├── backend/                        # Kotlin Backend
│   ├── build.gradle.kts            # Root build config
│   ├── settings.gradle.kts         # Multi-project config
│   └── services/
│       └── auth-service/           # Auth Service (8001)
│           ├── build.gradle.kts
│           └── src/main/kotlin/com/aura/auth/
│               ├── AuthApplication.kt
│               ├── domain/model/User.kt
│               ├── domain/repository/UserRepository.kt
│               ├── application/usecase/
│               │   ├── RegisterUseCase.kt
│               │   └── LoginUseCase.kt
│               ├── infrastructure/
│               │   ├── security/JwtService.kt
│               │   └── database/AuthDatabase.kt
│               └── presentation/router/AuthRouter.kt
│
├── ai-service/                     # Python AI Service (9001)
│   ├── Dockerfile
│   ├── requirements.txt
│   └── app/
│       ├── main.py
│       ├── core/config.py
│       ├── models/schemas.py
│       ├── api/routes/
│       │   ├── rag.py
│       │   └── recommendations.py
│       ├── services/rag_service.py
│       └── infrastructure/
│           ├── embedder.py
│           └── vector_store.py
│
└── docs/ARCHITECTURE.md           # Этот документ
```

### Готовые компоненты

| Компонент | Статус | Описание |
|-----------|--------|----------|
| docker-compose.yml | ✅ Готов | Все сервисы, PostgreSQL, Redis, Weaviate |
| Auth Service | ✅ Готов | Регистрация, JWT, сессии |
| User Service | 📋 Код написан | Профиль, анкета |
| Product Service | 📋 Код написан | Каталог, составы |
| Recommendation Service | 📋 Код написан | Рекомендации |
| Tracker Service | 📋 Код написан | Трекер, напоминания |
| AI Service | ✅ Готов | RAG, recommendations |
| Mobile App | 📋 Структура | Kotlin/Compose |

---

# Этап 0: Анализ проекта

## 📌 Что делаем

Анализируем требования из документации и определяем архитектурные решения.

## 🧠 Решение

### 🔍 Анализ проблемы
- **Проблема**: Пользователям сложно подобрать косметику из-за большого ассортимента и отсутствия персонализации
- **Решение**: ИИ-система с анкетированием, рекомендациями и трекером

### 👥 User Flows

```
User Flow 1: Первичное использование
┌─────────┐    ┌─────────┐    ┌──────────────┐    ┌─────────────┐
│Регистрация│ -> │Анкета   │ -> │Рекомендации  │ -> │Использование│
└─────────┘    └─────────┘    └──────────────┘    └─────────────┘
                                                        │
                                                        v
                                                  ┌──────────┐
                                                  │Трекер    │
                                                  │Напоминания│
                                                  └──────────┘

User Flow 2: Консультация с ИИ
┌─────────┐    ┌─────────┐    ┌──────────────┐    ┌─────────────┐
│Поиск    │ -> │Чат с ИИ  │ -> │RAG-ответ    │ -> │Рекомендация │
└─────────┘    └─────────┘    └──────────────┘    └─────────────┘
```

### 🎯 Use Cases

| Use Case | Описание |
|----------|----------|
| UC-01 | Регистрация и аутентификация пользователя |
| UC-02 | Заполнение анкеты (тип кожи, возраст, проблемы) |
| UC-03 | Получение персонализированных рекомендаций |
| UC-04 | Просмотр карточек продуктов с составами |
| UC-05 | Взаимодействие с RAG-ассистентом |
| UC-06 | Отслеживание использования средств |
| UC-07 | Получение напоминаний об уходе |

### ⚠️ Pain Points

1. **Сложность подбора** - тысячи продуктов без персонализации
2. **Низкая экспертиза** - пользователи не разбираются в составах
3. **Нет отслеживания** - забывают использовать средства
4. **Нет поддержки** - нет эксперта для консультаций

### 🧩 Модули системы

```
┌─────────────────────────────────────────────────────────────────┐
│                        MOBILE APP (Kotlin/Compose)             │
├─────────────────────────────────────────────────────────────────┤
│  ┌─────────────┐  ┌──────────────┐  ┌─────────┐  ┌──────────┐  │
│  │  Анкета     │  │Рекомендации │  │Трекер   │  │ AI-чат   │  │
│  └─────────────┘  └──────────────┘  └─────────┘  └──────────┘  │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                      BACKEND (Kotlin/Microservices)            │
├────────────┬─────────────┬──────────────┬─────────┬────────────┤
│   Auth     │   User      │  Recommendation│ Product │  Tracker  │
│  Service   │  Profile    │    Service    │ Service │  Service  │
│            │  Service    │               │         │           │
└────────────┴─────────────┴──────────────┴─────────┴────────────┘
         │              │                 │         │
         └──────────────┼─────────────────┼─────────┘
                        ▼
┌─────────────────────────────────────────────────────────────────┐
│                    AI SERVICE (Python/FastAPI)                 │
├────────────┬─────────────┬─────────────────────────────────────┤
│  RAG       │  Recommen-  │      Ingredient                    │
│  Engine    │  dation AI   │      Analyzer                      │
└────────────┴─────────────┴─────────────────────────────────────┘
                        │
        ┌───────────────┼───────────────┐
        ▼               ▼               ▼
   ┌──────────┐   ┌──────────┐   ┌──────────┐
   │PostgreSQL│   │Vector DB │   │  Cache   │
   │ (users)  │   │(FAISS)   │   │(Redis)   │
   └──────────┘   └──────────┘   └──────────┘
```

## 📄 Выход: Архитектурный анализ

### Список сервисов

| Сервис | Ответственность | Технология |
|--------|----------------|-------------|
| Auth Service | Регистрация, JWT, сессии | Kotlin/Ktor |
| User Profile Service | Анкета, профиль пользователя | Kotlin/Ktor |
| Recommendation Service | Персонализированные рекомендации | Kotlin/Ktor |
| Product Service | Каталог продуктов, составы | Kotlin/Ktor |
| Tracker Service | Трекер использования, напоминания | Kotlin/Ktor |
| AI Service | RAG, анализ составов, рекомендации | Python/FastAPI |

### Схема взаимодействия

```
Mobile App
    │
    ├─► Auth ──────────► PostgreSQL
    │
    ├─► Profile ───────► PostgreSQL
    │
    ├─► Products ──────► PostgreSQL
    │
    ├─► Recommendations ──► AI Service ──► Vector DB
    │
    ├─► Tracker ────────► PostgreSQL
    │
    └─► RAG Chat ──────► AI Service ──► Vector DB + LLM
```

---

## 🚀 Этап 1: Архитектура системы

### 🗄️ Инфраструктура хранения данных

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           ИНФРАСТРУКТУРА ХРАНЕНИЯ                            │
└─────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────┐
│                           DOCKER COMPOSE (LOCAL)                           │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌──────────────────┐   ┌──────────────────┐   ┌──────────────────┐       │
│  │   PostgreSQL     │   │   Redis          │   │   Weaviate       │       │
│  │   Port: 5432     │   │   Port: 6379     │   │   Port: 8080     │       │
│  │   DB: aura       │   │                  │   │   (Vector DB)    │       │
│  └──────────────────┘   └──────────────────┘   └──────────────────┘       │
│                                                                             │
│  ┌──────────────────┐   ┌──────────────────┐                               │
│  │   MinIO          │   │   Prometheus    │                               │
│  │   Port: 9000     │   │   Port: 9090    │                               │
│  │   (S3 storage)   │   │                  │                               │
│  └──────────────────┘   └──────────────────┘                               │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────┐
│                           PRODUCTION (Cloud)                               │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌──────────────────┐   ┌──────────────────┐   ┌──────────────────┐       │
│  │  Cloud PostgreSQL│   │   Redis Cluster │   │  Weaviate Cloud  │       │
│  │  (AWS RDS/       │   │   (AWS Elasti-   │   │  or              │       │
│  │   Cloud SQL)     │   │    Cache)       │   │  Pinecone        │       │
│  └──────────────────┘   └──────────────────┘   └──────────────────┘       │
│                                                                             │
│  ┌──────────────────┐   ┌──────────────────┐                               │
│  │   S3/MinIO       │   │   Monitoring     │                               │
│  │   (Images,       │   │   (CloudWatch/   │                               │
│  │    Files)        │   │    DataDog)      │                               │
│  └──────────────────┘   └──────────────────┘                               │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 📦 docker-compose.yml (для разработки)

```yaml
version: '3.8'

services:
  # Основная база данных
  postgres:
    image: postgres:15-alpine
    container_name: aura_postgres
    environment:
      POSTGRES_DB: aura
      POSTGRES_USER: aura_user
      POSTGRES_PASSWORD: aura_password
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U aura_user -d aura"]
      interval: 10s
      timeout: 5s
      retries: 5

  # Кэш и сессии
  redis:
    image: redis:7-alpine
    container_name: aura_redis
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data
    command: redis-server --appendonly yes

  # Vector DB для RAG и поиска
  weaviate:
    image: semitechnologies/weaviate:latest
    container_name: aura_weaviate
    ports:
      - "8080:8080"
    environment:
      QUERY_DEFAULTS_LIMIT: 25
      AUTHENTICATION_ANONYMOUS_ACCESS_ENABLED: true
      PERSISTENCE_DATA_PATH: /var/lib/weaviate
      ENABLE_MODULES: text2vec-transformers
      TRANSFORMERS_INFERENCE_API: http://t2v-transformers:8080
    volumes:
      - weaviate_data:/var/lib/weaviate
    depends_on:
      - t2v-transformers

  # Модель для эмбеддингов
  t2v-transformers:
    image: semitechnologies/transformers-inference:sentence-transformers-all-MiniLM-L6-v2
    environment:
      ENABLE_CUDA: 0

  # S3-совместимое хранилище для файлов
  minio:
    image: minio/minio:latest
    container_name: aura_minio
    ports:
      - "9000:9000"
      - "9001:9001"
    environment:
      MINIO_ROOT_USER: minioadmin
      MINIO_ROOT_PASSWORD: minioadmin
    volumes:
      - minio_data:/data
    command: server /data --console-address ":9001"

volumes:
  postgres_data:
  redis_data:
  weaviate_data:
  minio_data:
```

### 🔐 Настройки подключения (config.yaml)

```yaml
database:
  host: "localhost"
  port: 5432
  name: "aura"
  user: "aura_user"
  password: "aura_password"
  pool:
    min: 2
    max: 10

redis:
  host: "localhost"
  port: 6379

vector_db:
  type: "weaviate"  # или "faiss", "pinecone"
  url: "http://localhost:8080"

storage:
  type: "minio"  # или "s3"
  endpoint: "http://localhost:9000"
  bucket: "aura"
```

---

## 🚀 Этап 2: Проектирование базы данных

## 🧠 ER-диаграмма (текстом)

```
┌────────────────────────────────────────────────────────────────────────────┐
│                              POSTGRESQL SCHEMA                             │
└────────────────────────────────────────────────────────────────────────────┘

┌─────────────┐     ┌─────────────────┐     ┌──────────────────┐
│   users     │     │  skin_profiles  │     │   products       │
├─────────────┤     ├─────────────────┤     ├──────────────────┤
│ id (PK)     │◄────│ user_id (FK)    │     │ id (PK)          │
│ email       │     │ skin_type       │     │ name             │
│ password    │     │ age             │     │ brand            │
│ name        │     │ concerns        │     │ category         │
│ created_at  │     │ allergies       │     │ description      │
│ updated_at  │     │ goals           │     │ image_url        │
└─────────────┘     │ completed       │     │ price            │
                    │ created_at      │     │ created_at       │
                    └─────────────────┘     └────────┬─────────┘
                                                     │
                        ┌────────────────────────────┴────────────┐
                        │              product_ingredients         │
                        ├─────────────────────────────────────────┤
                        │ product_id (FK) ──────────────────────►│
                        │ ingredient_id (FK) ─────────────────────►│
                        │ position (order in INCI)               │
                        └─────────────────────────────────────────┘
                                                     │
                    ┌─────────────────┐     ┌──────────────────┐
                    │  ingredients    │     │   categories     │
                    ├─────────────────┤     ├──────────────────┤
                    │ id (PK)         │     │ id (PK)          │
                    │ name            │     │ name (unique)    │
                    │ inci_name       │     │ parent_id (FK)   │
                    │ description     │     └──────────────────┘
                    │ safety_level   │
                    │ category       │
                    │ benefits       │
                    └─────────────────┘
```

## 📊 Где хранятся данные

| Компонент | Технология | Расположение | Назначение |
|-----------|------------|--------------|------------|
| Пользователи, профили, продукты | PostgreSQL | Docker volume `postgres_data` | Транзакционные данные |
| Сессии, кэш | Redis | Docker volume `redis_data` | Быстрый доступ |
| Вектора (RAG, рекомендации) | Weaviate | Docker volume `weaviate_data` | Семантический поиск |
| Изображения, файлы | MinIO (S3) | Docker volume `minio_data` | Статические файлы |

### ☁️ Production варианты (Cloud)

| Сервис | Варианты | Стоимость |
|--------|----------|------------|
| PostgreSQL | AWS RDS, Cloud SQL, Supabase, Neon | $0-50/мес |
| Redis | AWS ElastiCache, Redis Cloud | $0-30/мес |
| Vector DB | Pinecone, Weaviate Cloud, Qdrant | $0-70/мес |
| S3 | AWS S3, DigitalOcean Spaces | $0-10/мес |

---

## ❓ Переходим к следующему этапу? (Этап 3 - Backend Kotlin)