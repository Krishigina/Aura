# Дизайн: Расширенная сущность Продукт

## Обзор

Расширение модели Product для хранения детальной информации о косметических средствах.

## Сущность Product

| Поле | Тип | Описание |
|------|-----|----------|
| id | INT | Primary key |
| name | VARCHAR(255) | Название продукта |
| what_is_it | TEXT | Что это (напр. "Крем для глаз с маслом из листьев конопли") |
| brand | VARCHAR(255) | Бренд (FK → dictionary brands) |
| product_type | VARCHAR(50) | Тип продукта (enum) |
| for_whom | VARCHAR(50) | Для кого (enum) |
| purpose | VARCHAR(255) | Назначение (enum) |
| skin_type | VARCHAR(50) | Тип кожи (enum) |
| application_time | VARCHAR(50) | Время нанесения (enum) |
| area | VARCHAR(50) | Область применения (enum) |
| active_ingredient | TEXT | Действующий компонент (свободный текст) |
| volume | VARCHAR(50) | Объём (FK → dictionary volumes) |
| segment | VARCHAR(50) | Сегмент (enum) |
| composition | TEXT | Состав (большой текст) |
| application_info | TEXT | Инструкция по применению |
| country | VARCHAR(100) | Страна происхождения |
| manufacturer | VARCHAR(255) | Изготовитель |
| description | TEXT | Описание |
| video | BYTEA | Видеообзор (mp4) |
| photos | JSONB | Фото [ {id, data, filename} ] |
| created_at | TIMESTAMP | Дата создания |

## Enum значения

### product_type
Крем, Сыворотка, Лосьон, Тоник, Эмульсия, Масло, Гель, Пилинг, Маска, Бальзам, Спрей, Мист

### for_whom
Универсальный, Мужчинам, Женщинам

### purpose
Увлажнение, Очищение, Питание, Антивозрастной, Отбеливание, Защита от солнца, Проблемная кожа, Восстановление, Матирование, Тонирование

### skin_type
Сухая, Жирная, Комбинированная, Нормальная, Чувствительная, Проблемная

### application_time
Утро, Вечер, Утро/Вечер

### area
Лицо, Тело, Волосы, Губы, Руки, Веки, Зона вокруг глаз

### segment
Бюджетная, Люкс, Профессиональная, Космецевтика

## API Endpoints

### GET /api/products
Возвращает список всех продуктов

### POST /api/products
Создать новый продукт

### PUT /api/products/{id}
Обновить продукт

### DELETE /api/products/{id}
Удалить продукт

### POST /api/products/{id}/photos
Загрузить фото продукта (multipart/form-data)

### DELETE /api/products/{id}/photos/{photo_id}
Удалить фото

### POST /api/products/{id}/video
Загрузить видео (multipart/form-data, только mp4)

### DELETE /api/products/{id}/video
Удалить видео

### GET /api/products/{id}/video
Скачать видео (stream)

## Фронтенд

### Products.jsx
- Форма с новыми полями
- Drag & drop загрузка фото
- Drag & drop загрузка видео
- Предпросмотр фото (grid)
- Плеер для видео

### Компоненты
- FileUpload - загрузка файлов
- PhotoGallery - галерея фото
- VideoPlayer - проигрыватель видео

## Хранение файлов

Фото и видео хранятся в PostgreSQL:
- photos: JSONB массив {id, filename, data (base64), content_type}
- video: BYTEA

## Миграция

1. Создать новую таблицу products с расширенной схемой
2. Перенести данные из старой таблицы
3. Удалить старую таблицу
