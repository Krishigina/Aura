from fastapi import HTTPException


async def update_brand_record(conn, data):
    row = await conn.fetchrow(
        """
        UPDATE brands
        SET description=$1, country=$2, country_origin=$3, manufacturer=$4
        WHERE value=$5
        RETURNING id, value, description, country, country_origin, manufacturer
        """,
        data.description,
        data.country,
        data.country_origin,
        data.manufacturer,
        data.value,
    )
    if not row:
        raise HTTPException(status_code=404, detail="Бренд не найден")
    return dict(row)


async def list_dictionary_values(conn, *, key: str, table: str):
    if key == "brands":
        rows = await conn.fetch(
            "SELECT id, value, description, country, country_origin, manufacturer FROM brands ORDER BY id"
        )
        return [dict(row) for row in rows]

    rows = await conn.fetch(f"SELECT value FROM {table} ORDER BY id")
    return [row["value"] for row in rows]


async def create_dictionary_record(conn, *, key: str, table: str, value: str):
    try:
        if key == "brands":
            row = await conn.fetchrow(
                """
                INSERT INTO brands (value, description, country, country_origin, manufacturer)
                VALUES ($1, NULL, NULL, NULL, NULL)
                RETURNING id, value, description, country, country_origin, manufacturer
                """,
                value,
            )
            return dict(row)

        row = await conn.fetchrow(
            f"INSERT INTO {table} (value) VALUES ($1) RETURNING id, value",
            value,
        )
        return dict(row)
    except Exception as exc:
        message = str(exc).lower()
        if "duplicate key" in message or "unique" in message:
            raise HTTPException(status_code=400, detail="Значение уже существует") from exc
        raise


async def update_dictionary_record(conn, *, key: str, table: str, data: dict):
    if key == "brands":
        value = data.get("value")
        if not value or not isinstance(value, str):
            raise HTTPException(status_code=400, detail="Поле value обязательно для бренда")

        row = await conn.fetchrow(
            """
            UPDATE brands
            SET description=$1, country=$2, country_origin=$3, manufacturer=$4
            WHERE value=$5
            RETURNING id, value, description, country, country_origin, manufacturer
            """,
            data.get("description"),
            data.get("country"),
            data.get("country_origin"),
            data.get("manufacturer"),
            value,
        )
        if not row:
            raise HTTPException(status_code=404, detail="Бренд не найден")
        return dict(row)

    old_value = data.get("oldValue")
    new_value = data.get("newValue")
    if not old_value or not new_value:
        raise HTTPException(status_code=400, detail="Требуются поля oldValue и newValue")

    row = await conn.fetchrow(
        f"UPDATE {table} SET value=$1 WHERE value=$2 RETURNING id, value",
        new_value,
        old_value,
    )
    if not row:
        raise HTTPException(status_code=404, detail="Значение не найдено")
    return dict(row)


async def delete_dictionary_record(conn, *, table: str, value: str):
    result = await conn.execute(f"DELETE FROM {table} WHERE value=$1", value)
    deleted_count = int(result.split()[-1]) if result else 0
    if deleted_count == 0:
        raise HTTPException(status_code=404, detail="Значение не найдено")
    return {"success": True}
