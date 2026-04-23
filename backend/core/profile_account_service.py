from fastapi import HTTPException


async def update_profile_account_record(
    conn,
    *,
    request,
    current_user_id: int,
    ensure_users_columns,
    ensure_unique_login,
    is_valid_login,
    normalize_login,
):
    name = (request.name or "").strip()
    if not name:
        raise HTTPException(status_code=400, detail="Имя не может быть пустым")

    login = normalize_login(request.nickname)
    if login and not is_valid_login(login):
        raise HTTPException(status_code=400, detail="Логин должен быть в формате @login")

    await ensure_users_columns(conn)
    await ensure_unique_login(conn, login, exclude_user_id=current_user_id)
    row = await conn.fetchrow(
        """
        UPDATE users
        SET name=$1, nickname=$2, updated_at=NOW()
        WHERE id=$3
        RETURNING *
        """,
        name,
        login,
        current_user_id,
    )
    if not row:
        raise HTTPException(status_code=404, detail="Пользователь не найден")

    user = dict(row)
    user.pop("password_hash", None)
    return user


async def update_profile_password_record(
    conn,
    *,
    request,
    current_user_id: int,
    verify_password,
    get_password_hash,
):
    current_password = (request.current_password or "").strip()
    new_password = (request.new_password or "").strip()

    if not current_password:
        raise HTTPException(status_code=400, detail="Введите текущий пароль")
    if len(new_password) < 6:
        raise HTTPException(status_code=400, detail="Новый пароль минимум 6 символов")

    row = await conn.fetchrow("SELECT id, password_hash FROM users WHERE id=$1", current_user_id)
    if not row:
        raise HTTPException(status_code=404, detail="Пользователь не найден")

    stored_password = dict(row).get("password_hash")
    if not stored_password or not verify_password(current_password, stored_password):
        raise HTTPException(status_code=400, detail="Текущий пароль введен неверно")

    await conn.execute(
        "UPDATE users SET password_hash=$1, updated_at=NOW() WHERE id=$2",
        get_password_hash(new_password),
        current_user_id,
    )
    return {"ok": True}


async def delete_profile_account_record(
    conn,
    *,
    request,
    current_user_id: int,
    verify_password,
):
    current_password = (request.current_password or "").strip()
    if not current_password:
        raise HTTPException(status_code=400, detail="Введите текущий пароль")

    row = await conn.fetchrow("SELECT id, password_hash FROM users WHERE id=$1", current_user_id)
    if not row:
        raise HTTPException(status_code=404, detail="Пользователь не найден")

    stored_password = dict(row).get("password_hash")
    if not stored_password or not verify_password(current_password, stored_password):
        raise HTTPException(status_code=400, detail="Текущий пароль введен неверно")

    await conn.execute("DELETE FROM user_profiles WHERE user_id=$1", current_user_id)
    await conn.execute("DELETE FROM users WHERE id=$1", current_user_id)
    return {"ok": True}
