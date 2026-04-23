from fastapi import HTTPException


DEFAULT_USER_ROLE = "Пользователь"
INVALID_CREDENTIALS_DETAIL = "Неверный email или пароль"
INVALID_EMAIL_DETAIL = "Некорректный email"
LOGIN_FORMAT_DETAIL = "Логин должен быть в формате @login"
ADMIN_LOGIN_FORMAT_DETAIL = "Логин должен быть в формате @login, только a-z, 0-9 и _"
LOGIN_TAKEN_DETAIL = "Логин уже занят"


async def login_user_account(conn, *, request, user_select_sql, verify_password, create_access_token, token_response_model):
    row = await conn.fetchrow(
        f"SELECT * FROM ({user_select_sql('u')}) AS hydrated_users WHERE LOWER(email) = LOWER($1)",
        request.email,
    )
    if not row:
        raise HTTPException(status_code=401, detail=INVALID_CREDENTIALS_DETAIL)

    user_dict = dict(row)
    stored_password = user_dict.get("password_hash")
    if not stored_password or not verify_password(request.password, stored_password):
        raise HTTPException(status_code=401, detail=INVALID_CREDENTIALS_DETAIL)

    access_token = create_access_token(data={"sub": user_dict["id"]})
    user_dict.pop("password_hash", None)
    return token_response_model(access_token=access_token, user=user_dict)


async def register_user_account(
    conn,
    *,
    user,
    ensure_users_columns,
    is_valid_email,
    normalize_login,
    is_valid_login,
    ensure_unique_login,
    get_password_hash,
    sync_user_role_ref,
    user_select_sql,
    create_access_token,
    token_response_model,
):
    if not user.password:
        raise HTTPException(status_code=400, detail="Пароль обязателен")
    if not is_valid_email(user.email):
        raise HTTPException(status_code=400, detail=INVALID_EMAIL_DETAIL)

    login_value = normalize_login(user.nickname)
    if login_value and not is_valid_login(login_value):
        raise HTTPException(status_code=400, detail=LOGIN_FORMAT_DETAIL)

    await ensure_users_columns(conn)
    existing = await conn.fetchrow("SELECT id FROM users WHERE LOWER(email) = LOWER($1)", user.email)
    if existing:
        raise HTTPException(status_code=400, detail="Email уже занят")
    if login_value:
        await ensure_unique_login(conn, login_value)

    password_hash = get_password_hash(user.password)
    role = user.role or DEFAULT_USER_ROLE
    row = await conn.fetchrow(
        """INSERT INTO users (name, email, nickname, avatar, password_hash)
           VALUES ($1, $2, $3, $4, $5) RETURNING *""",
        user.name,
        user.email,
        login_value,
        user.avatar,
        password_hash,
    )
    await sync_user_role_ref(conn, {"id": row["id"], "role": role})
    row = await conn.fetchrow(f"SELECT * FROM ({user_select_sql('u')}) AS hydrated_users WHERE id=$1", row["id"])

    user_dict = dict(row)
    access_token = create_access_token(data={"sub": user_dict["id"]})
    user_dict.pop("password_hash", None)
    return token_response_model(access_token=access_token, user=user_dict)


async def list_users_for_admin(conn, *, role, ensure_users_columns, user_select_sql):
    await ensure_users_columns(conn)
    if role and role != "all":
        rows = await conn.fetch(
            f"SELECT * FROM ({user_select_sql('u')}) AS hydrated_users WHERE role=$1 ORDER BY id DESC",
            role,
        )
    else:
        rows = await conn.fetch(f"SELECT * FROM ({user_select_sql('u')}) AS hydrated_users ORDER BY id DESC")
    return [dict(row) for row in rows]


async def create_user_record(
    conn,
    *,
    user,
    normalize_login,
    is_valid_email,
    is_valid_login,
    ensure_users_columns,
    ensure_unique_login,
    get_password_hash,
    sync_user_role_ref,
    user_select_sql,
):
    login = normalize_login(user.nickname)
    if not is_valid_email(user.email):
        raise HTTPException(status_code=400, detail=INVALID_EMAIL_DETAIL)
    if login and not is_valid_login(login):
        raise HTTPException(status_code=400, detail=ADMIN_LOGIN_FORMAT_DETAIL)

    await ensure_users_columns(conn)
    await ensure_unique_login(conn, login)
    password_hash = get_password_hash(user.password) if user.password else None

    row = await conn.fetchrow(
        """INSERT INTO users (name, email, nickname, avatar, password_hash)
           VALUES ($1, $2, $3, $4, $5) RETURNING *""",
        user.name,
        user.email,
        login,
        user.avatar,
        password_hash,
    )
    await sync_user_role_ref(conn, {"id": row["id"], "role": user.role})
    row = await conn.fetchrow(f"SELECT * FROM ({user_select_sql('u')}) AS hydrated_users WHERE id=$1", row["id"])
    return dict(row)


async def update_user_record(
    conn,
    *,
    user_id: int,
    user,
    normalize_login,
    is_valid_email,
    is_valid_login,
    ensure_users_columns,
    ensure_unique_login,
    sync_user_role_ref,
    user_select_sql,
):
    login = normalize_login(user.nickname)
    if not is_valid_email(user.email):
        raise HTTPException(status_code=400, detail=INVALID_EMAIL_DETAIL)
    if login and not is_valid_login(login):
        raise HTTPException(status_code=400, detail=ADMIN_LOGIN_FORMAT_DETAIL)

    await ensure_users_columns(conn)
    await ensure_unique_login(conn, login, user_id)
    row = await conn.fetchrow(
        """UPDATE users SET name=$1, email=$2, nickname=$3, avatar=$4
           WHERE id=$5 RETURNING *""",
        user.name,
        user.email,
        login,
        user.avatar,
        user_id,
    )
    if not row:
        raise HTTPException(status_code=404, detail="User not found")
    await sync_user_role_ref(conn, {"id": row["id"], "role": user.role})
    row = await conn.fetchrow(f"SELECT * FROM ({user_select_sql('u')}) AS hydrated_users WHERE id=$1", row["id"])
    return dict(row)


async def delete_user_record(conn, *, user_id: int):
    await conn.execute("DELETE FROM users WHERE id=$1", user_id)
    return {"success": True}


def sanitize_current_user_payload(current_user: dict):
    user = current_user.copy()
    user.pop("password_hash", None)
    return user
