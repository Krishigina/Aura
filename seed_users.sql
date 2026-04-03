-- Аккаунты для доступа к панели управления
-- Логин/пароль будут предоставлены отдельно

-- Создаём таблицу users если не существует
CREATE TABLE IF NOT EXISTS users (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    role VARCHAR(50) DEFAULT 'user',
    nickname VARCHAR(255),
    phone VARCHAR(50),
    avatar VARCHAR(500),
    password_hash VARCHAR(255),
    created_at TIMESTAMP DEFAULT NOW()
);

-- Вставляем аккаунты (пароли bcrypt)
INSERT INTO users (name, email, role, nickname, password_hash) VALUES
('Администратор', 'admin@aura.ru', 'Администратор', '@admin', '$2b$12$ZHHwuqJvTxj8pBtAIw.vZeeNFf0R6SqQGaBQVAP0oQybI7wvAXRxa'),
('Менеджер', 'manager@aura.ru', 'Менеджер', '@manager', '$2b$12$NIAYmZsv3bUdQ/qV9FsBEu8YmewrUvmdAselIabXMAqfqRSsVumB2'),
('Косметолог', 'cosmetolog@aura.ru', 'Косметолог', '@cosmetolog', '$2b$12$7C6.DSL9h6guXtFefspVEu7NYf6fuX6YjMaQo/Sk3T3nm87/qTg06')
ON CONFLICT (email) DO UPDATE SET
    password_hash = EXCLUDED.password_hash,
    role = EXCLUDED.role;