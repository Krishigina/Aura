import os

from backend.core.entity_dictionary_refs import sync_content_category_ref, sync_content_tag_refs


async def _seed_dictionary_values(conn, table_name: str, values: list[str], *, reset: bool = False):
    if reset:
        await conn.execute(f"DELETE FROM {table_name}")
    else:
        count = await conn.fetchval(f"SELECT COUNT(*) FROM {table_name}")
        if count != 0:
            return
    for value in values:
        await conn.execute(f"INSERT INTO {table_name} (value) VALUES ($1)", value)


async def seed_reference_data(conn):
    await _seed_dictionary_values(conn, "brands", [
        "Aura", "La Roche-Posay", "Vichy", "Bioderma", "CeraVe",
        "The Ordinary", "Paula's Choice", "Cosrx", "Eucerin", "Nivea",
    ])
    await _seed_dictionary_values(conn, "categories", [
        "Очищение", "Увлажнение", "Сыворотки", "SPF", "Уход",
        "Маска", "Тоник", "Крем", "Масло",
    ])
    await _seed_dictionary_values(conn, "segments", [
        "Бюджетная", "Люкс", "Профессиональная", "Космецевтика",
    ])
    await _seed_dictionary_values(conn, "volumes", [
        "15мл", "30мл", "50мл", "75мл", "100мл", "150мл", "200мл", "250мл", "500мл", "1л",
    ])
    await _seed_dictionary_values(conn, "skin_types", [
        "Нормальная", "Сухая", "Жирная", "Комбинированная", "Чувствительная",
    ])
    await _seed_dictionary_values(conn, "product_types", [
        "Крем", "Сыворотка", "Лосьон", "Тоник", "Маска", "Масло", "Спрей", "Гель", "Эмульсия", "Бальзам",
    ])
    await _seed_dictionary_values(conn, "for_whom", [
        "Универсальный", "Мужчинам", "Женщинам", "Детям", "Беременным",
    ])
    await _seed_dictionary_values(conn, "purposes", [
        "Увлажнение", "Питание", "Очищение", "Омоложение", "Отбеливание", "Защита от солнца", "Против акне", "Восстановление",
    ])
    await _seed_dictionary_values(conn, "application_times", [
        "Утро", "Вечер", "Утро и вечер", "По необходимости",
    ])
    await _seed_dictionary_values(conn, "areas", [
        "Лицо", "Тело", "Волосы", "Губы", "Глаза", "Шея", "Руки", "Ноги",
    ])
    await _seed_dictionary_values(conn, "countries", [
        "Франция", "Корея", "Япония", "США", "Германия", "Швейцария", "Россия", "Италия", "Испания", "Израиль",
    ])
    await _seed_dictionary_values(conn, "procedure_categories", [
        "Аппаратная косметология", "Инъекционная косметология", "Эстетическая косметология",
    ], reset=True)
    await _seed_dictionary_values(conn, "content_categories", [
        "Уход за кожей", "Ингредиенты", "Защита", "Процедуры", "Питание", "Образ жизни",
    ])
    await _seed_dictionary_values(conn, "user_roles", [
        "Пользователь", "Косметолог", "Менеджер", "Администратор",
    ])


async def seed_demo_data(conn, get_password_hash):
    if os.getenv("SEED_DEMO_DATA", "false").lower() != "true":
        return

    users_count = await conn.fetchval("SELECT COUNT(*) FROM users")
    if users_count == 0:
        demo_users = [
            ("admin@aura.ru", "Администратор", "admin", "admin123", "Администратор"),
            ("manager@aura.ru", "Менеджер", "manager", "manager123", "Менеджер"),
            ("cosmetolog@aura.ru", "Косметолог", "cosmo", "cosmo123", "Косметолог"),
        ]
        for email, name, nickname, password, role in demo_users:
            await conn.execute(
                """
                INSERT INTO users (name, email, role, nickname, password_hash)
                VALUES ($1, $2, $3, $4, $5)
                """,
                name,
                email,
                role,
                f"@{nickname}",
                get_password_hash(password),
            )
        print("Demo users created")

    products_count = await conn.fetchval("SELECT COUNT(*) FROM products")
    if products_count == 0:
        demo_products = [
            ("La Roche-Posay Effaclar Duo+", "Очищение", "Корректирующий крем для проблемной кожи.", "50мл", "Космецевтика"),
            ("Vichy Mineral 89", "Увлажнение", "Укрепляющий ежедневный уход.", "50мл", "Космецевтика"),
            ("CeraVe Hydrating Cleanser", "Очищение", "Увлажняющее очищение для сухой и нормальной кожи.", "250мл", "Космецевтика"),
        ]
        for name, category, description, volume, segment in demo_products:
            await conn.execute(
                """
                INSERT INTO products (name, category, description, volume, segment)
                VALUES ($1, $2, $3, $4, $5)
                """,
                name,
                category,
                description,
                volume,
                segment,
            )
        print("Demo products created")

    procedures_count = await conn.fetchval("SELECT COUNT(*) FROM procedures")
    if procedures_count == 0:
        demo_procedures = [
            ("Пилинг лица", "Эстетическая косметология", "Химический", "30-60 мин", "Не требуется", "Лицо", "Обновление кожи"),
            ("Мезотерапия", "Инъекционная косметология", "Инъекционный", "45-90 мин", "Инъекционный аппарат", "Лицо, шея", "Глубокое увлажнение"),
        ]
        for name, direction, method_type, duration, equipment, zones, effects in demo_procedures:
            await conn.execute(
                """
                INSERT INTO procedures (name, direction, method_type, duration, equipment, zones, effects, description)
                VALUES ($1, $2, $3, $4, $5, $6, $7, $8)
                """,
                name,
                direction,
                method_type,
                duration,
                equipment,
                zones,
                effects,
                effects,
            )
        print("Demo procedures created")

    content_count = await conn.fetchval("SELECT COUNT(*) FROM content")
    if content_count == 0:
        demo_content = [
            ("Как правильно очищать кожу", "Уход за кожей", "очищение, уход, кожа", "Администратор", "Базовые правила очищения кожи.", True),
            ("Ниацинамид: полный гид", "Ингредиенты", "ниацинамид, витамин B3, поры", "Администратор", "Краткий обзор одного из самых популярных ингредиентов.", True),
            ("SPF: зачем и как использовать", "Защита", "SPF, защита от солнца, фотостарение", "Администратор", "Практические рекомендации по ежедневной SPF-защите.", True),
        ]
        for title, category, tags, author_name, body, published in demo_content:
            row = await conn.fetchrow(
                """
                INSERT INTO content (title, category, author_name, body, published)
                VALUES ($1, $2, $3, $4, $5)
                RETURNING id
                """,
                title,
                category,
                author_name,
                body,
                published,
            )
            await sync_content_category_ref(conn, {"id": row["id"], "category": category})
            await sync_content_tag_refs(conn, {"id": row["id"], "tags": tags})
        print("Demo content created")


async def seed_database(conn, get_password_hash):
    await seed_reference_data(conn)
    await seed_demo_data(conn, get_password_hash)
