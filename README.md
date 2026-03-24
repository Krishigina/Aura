# Aura - Персонализированный подбор косметических средств с ИИ

## 🚀 Быстрый старт

### Требования

- Docker + Docker Compose
- JDK 17+
- Python 3.11+
- Node.js 18+ (для мобильной разработки)

### Запуск

```bash
# 1. Клонировать репозиторий
git clone https://github.com/your-repo/aura.git
cd aura

# 2. Запустить все сервисы
docker-compose up -d

# 3. Проверить статус
docker-compose ps
```

### Доступные сервисы

| Сервис | URL | Описание |
|--------|-----|----------|
| Auth Service | http://localhost:8001 | Регистрация, вход, JWT |
| User Service | http://localhost:8002 | Профиль, анкета |
| Product Service | http://localhost:8003 | Каталог продуктов |
| Recommendation Service | http://localhost:8004 | Рекомендации |
| Tracker Service | http://localhost:8005 | Трекер, напоминания |
| Admin Service | http://localhost:8010 | Панель управления |
| AI Service | http://localhost:9001 | RAG, рекомендации |
| Web Admin | http://localhost:8010 | Веб-интерфейс |
| PostgreSQL | localhost:5432 | База данных |
| Redis | localhost:6379 | Кэш |
| Weaviate | localhost:8080 | Vector DB |

---

## 📱 Mobile App

### Сборка

```bash
cd mobile

# Android
./gradlew assembleDebug

# iOS (требуется macOS)
./gradlew build
```

### Запуск

```bash
# Подключить устройство или эмулятор
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

## 🏗️ Архитектура

```
┌─────────────────────────────────────────────────────────────────┐
│                        MOBILE APP                               │
│                    Kotlin + Compose                             │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                      BACKEND MICROSERVICES                       │
├──────────┬──────────┬──────────┬──────────┬───────────────────┤
│   Auth   │   User   │ Product  │   Rec    │     Tracker      │
│ :8001    │  :8002   │  :8003   │  :8004   │      :8005        │
└──────────┴──────────┴──────────┴──────────┴───────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                         AI SERVICE                               │
│                  Python + FastAPI (:9001)                        │
│                                                              │
│   ┌──────────┐  ┌──────────────┐  ┌───────────────────────┐  │
│   │   RAG    │  │  Recommend   │  │    Ingredient         │  │
│   │ Pipeline │  │   Hybrid    │  │     Analyzer          │  │
│   └──────────┘  └──────────────┘  └───────────────────────┘  │
└────────────────────────────┬────────────────────────────────────┘
                             │
        ┌────────────────────┼────────────────────┐
        ▼                    ▼                    ▼
   PostgreSQL             Redis              Weaviate
   (Users, Products)      (Cache)            (Vectors)
```

---

## 🔧 Конфигурация

### Backend (docker-compose.yml)

```yaml
services:
  auth-service:
    environment:
      - DATABASE_URL=jdbc:postgresql://postgres:5432/aura
      - JWT_SECRET=your-secret-key
```

### AI Service (.env)

```env
WEAVIATE_URL=http://weaviate:8080
OPENAI_API_KEY=your-key
LLM_PROVIDER=openai
```

---

## 📝 API Endpoints

### Auth
- `POST /register` - Регистрация
- `POST /login` - Вход
- `POST /refresh` - Обновление токена

### Profile
- `GET /profile/{userId}` - Получить профиль
- `POST /profile/{userId}` - Сохранить профиль

### Products
- `GET /products` - Список продуктов
- `GET /products/{id}` - Детали продукта

### Recommendations
- `GET /recommendations/{userId}` - Рекомендации
- `POST /recommendations/{userId}/generate` - Сгенерировать

### Tracker
- `POST /tracker/{userId}/log` - Записать использование
- `GET /tracker/{userId}/history` - История

### AI
- `POST /api/v1/rag/query` - Запрос к ассистенту
- `POST /api/v1/recommendations/hybrid` - Гибридные рекомендации
- `POST /api/v1/ingredients/analyze` - Анализ состава

---

## 🧪 Тестирование

```bash
# Backend tests
cd backend
./gradlew test

# AI tests
cd ai-service
pytest tests/ -v

# Mobile tests
cd mobile
./gradlew test
```

---

## 📦 Деплоймент

### Docker

```bash
# Development
docker-compose up -d

# Production
docker-compose -f docker-compose.prod.yml up -d
```

### Kubernetes (example)

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: aura-auth-service
spec:
  replicas: 3
  selector:
    matchLabels:
      app: aura-auth
  template:
    spec:
      containers:
      - name: auth
        image: aura/auth-service:latest
        ports:
        - containerPort: 8001
```

---

## 📄 Лицензия

MIT License

---

## 👤 Авторы

- Aura Team

---

## 🆘 Поддержка

- GitHub Issues
- Email: support@aura-app.com