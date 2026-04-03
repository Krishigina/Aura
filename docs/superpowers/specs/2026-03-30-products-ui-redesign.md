# Products UI/UX Redesign Specification

## Overview
Реорганизация формы продукта в модальном окне с логическими секциями, добавление новых справочников, улучшение UI/UX карточек продуктов.

## Changes

### 1. Products.jsx - Форма продукта

**Секции с визуальными заголовками:**

| Секция | Поля | Акцент |
|--------|------|--------|
| О продукте | Название *, Что это?, Бренд * | #8B5CF6 |
| Описание | Описание | - |
| Характеристики | Тип продукта, Для кого, Назначение, Тип кожи, Время нанесения, Область применения, Активный компонент, Объём | #10B981 |
| Применение | Информация о применении | - |
| Состав | Состав | - |
| Бренд | Страна бренда | - |
| Дополнительно | Страна происхождения, Производитель | - |
| Медиа | Фотографии, Видео | - |
| Импорт | URL парсер | - |

**CSS стили для секций:**
- Каждая секция - это `form-section` div с заголовком
- Заголовок секции - font-weight 600, цвет акцента
- Отступ между секциями - 24px
- Секция "Характеристики" - 2 колонки grid

### 2. Dictionaries.jsx - Новые справочники

Добавить в `defaultEnums`:
```javascript
product_types: ['Крем', 'Сыворотка', 'Лосьон', 'Тоник', 'Эмульсия', 'Масло', 'Гель', 'Пилинг', 'Маска', 'Бальзам', 'Спрей', 'Мист'],
for_whom: ['Универсальный', 'Мужчинам', 'Женщинам'],
purposes: ['Увлажнение', 'Очищение', 'Питание', 'Антивозрастной', 'Отбеливание', 'Защита от солнца', 'Проблемная кожа', 'Восстановление', 'Матирование', 'Тонирование'],
application_times: ['Утро', 'Вечер', 'Утро/Вечер'],
areas: ['Лицо', 'Тело', 'Волосы', 'Губы', 'Руки', 'Веки', 'Зона вокруг глаз'],
```

Добавить в `dictConfig`:
```javascript
product_types: { label: 'Типы продуктов', icon: FlaskConical, color: '#8B5CF6' },
for_whom: { label: 'Для кого', icon: FlaskConical, color: '#EC4899' },
purposes: { label: 'Назначение', icon: FlaskConical, color: '#10B981' },
application_times: { label: 'Время применения', icon: FlaskConical, color: '#F59E0B' },
areas: { label: 'Области применения', icon: FlaskConical, color: '#3B82F6' },
```

Обновить `dictGroups.products`:
```javascript
dictionaries: ['brands', 'product_types', 'for_whom', 'purposes', 'skin_types', 'application_times', 'areas', 'segments', 'volumes']
```

### 3. Products.css - Стили секций

```css
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
```

### 4. Улучшение карточки продукта в таблице

- Добавить thumbnail фото (40x40px, rounded)
- Цветовая кодировка бейджей по типу продукта
- Активный компонент в tooltip при наведении

## Files to Modify

1. `web-admin/src/pages/Products.jsx` - реорганизация формы
2. `web-admin/src/pages/Products.css` - стили секций
3. `web-admin/src/pages/Dictionaries.jsx` - новые справочники
4. `web-admin/src/api/index.js` - загрузка новых справочников

## Acceptance Criteria

1. Форма продукта разделена на логические секции с заголовками
2. Все новые справочники доступны для редактирования в Settings > Dictionaries
3. Секция "Характеристики" отображается в 2 колонки
4. Стили соответствуют современному UI (glass-card, hover effects)
5. Форма сохраняет функциональность (сохранение, загрузка фото/видео, импорт URL)
