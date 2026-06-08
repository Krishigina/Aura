from datetime import date, datetime

from backend.core.chat_recommendations import should_attach_catalog_context


_RU_SHORT_MONTHS = {
    1: "янв",
    2: "фев",
    3: "мар",
    4: "апр",
    5: "май",
    6: "июн",
    7: "июл",
    8: "авг",
    9: "сен",
    10: "окт",
    11: "ноя",
    12: "дек",
}


def _format_chart_date(value: datetime) -> str:
    if value is None:
        return ""
    return f"{value.day:02d} {_RU_SHORT_MONTHS.get(value.month, value.strftime('%b').lower())}"


def _format_report_month(value: datetime) -> str:
    if value is None:
        return ""
    month = _RU_SHORT_MONTHS.get(value.month, value.strftime('%b').lower())
    return month.capitalize()


def _format_datetime(value: datetime) -> str:
    if value is None:
        return ""
    return value.strftime("%d.%m.%Y %H:%M")


def _normalize_anchor_day(value) -> date:
    if isinstance(value, datetime):
        return value.date()
    if isinstance(value, date):
        return value
    return datetime.utcnow().date()


def _build_daily_series(anchor_day: date, values_by_day: dict[str, int], field_name: str) -> list[dict]:
    rows = []
    for offset in range(6, -1, -1):
        day = date.fromordinal(anchor_day.toordinal() - offset)
        rows.append(
            {
                "date": _format_chart_date(day),
                field_name: int(values_by_day.get(day.isoformat(), 0) or 0),
            }
        )
    return rows


def _count_recommendation_requests(message_rows, *, window_start: date, window_end: date) -> dict[str, int]:
    history_by_session: dict[int, list[dict]] = {}
    values_by_day: dict[str, int] = {}

    for row in message_rows:
        session_id = int(row["session_id"])
        role = str(row["role"] or "").strip().lower()
        content = str(row["content"] or "").strip()
        created_at = row["created_at"]
        history = history_by_session.setdefault(session_id, [])

        if role == "user" and content:
            created_day = created_at.date() if isinstance(created_at, datetime) else created_at
            if window_start <= created_day <= window_end and should_attach_catalog_context(
                content,
                chat_history=history,
                product_context=None,
            ):
                day_key = created_day.isoformat()
                values_by_day[day_key] = values_by_day.get(day_key, 0) + 1

        if role in {"user", "assistant"} and content:
            history.append({"role": role, "content": content})

    return values_by_day


async def build_dashboard_analytics_payload(conn) -> dict:
    totals_row = await conn.fetchrow(
        """
        SELECT
            (SELECT COUNT(*)::int FROM users) AS users,
            (SELECT COUNT(*)::int FROM products) AS products,
            (SELECT COUNT(*)::int FROM procedures) AS procedures,
            (SELECT COUNT(*)::int FROM content) AS content
        """
    )
    requests_anchor_day = _normalize_anchor_day(
        await conn.fetchval(
            """
            SELECT COALESCE(MAX(created_at), CURRENT_DATE)::date
            FROM chat_messages
            WHERE COALESCE(is_from_user, false) = true
               OR COALESCE(role, '') = 'user'
            """
        )
    )
    user_activity_rows = await conn.fetch(
        """
        WITH days AS (
            SELECT generate_series(($1::date - INTERVAL '6 days')::date, $1::date, INTERVAL '1 day')::date AS day
        ),
        requests_daily AS (
            SELECT DATE(created_at) AS day, COUNT(*)::int AS requests
            FROM chat_messages
            WHERE created_at >= ($1::date - INTERVAL '6 days')
              AND created_at < ($1::date + INTERVAL '1 day')
              AND (
                  COALESCE(is_from_user, false) = true
                  OR COALESCE(role, '') = 'user'
              )
            GROUP BY DATE(created_at)
        )
        SELECT
            days.day,
            COALESCE(requests_daily.requests, 0) AS requests
        FROM days
        LEFT JOIN requests_daily ON requests_daily.day = days.day
        ORDER BY days.day
        """,
        requests_anchor_day,
    )
    recommendation_message_rows = await conn.fetch(
        """
        SELECT
            session_id,
            created_at,
            COALESCE(role, CASE WHEN is_from_user THEN 'user' ELSE 'assistant' END) AS role,
            COALESCE(content, text, '') AS content
        FROM chat_messages
        WHERE session_id IN (
            SELECT DISTINCT session_id
            FROM chat_messages
            WHERE created_at >= ($1::date - INTERVAL '6 days')
              AND created_at < ($1::date + INTERVAL '1 day')
              AND (
                  COALESCE(is_from_user, false) = true
                  OR COALESCE(role, '') = 'user'
              )
        )
          AND created_at < ($1::date + INTERVAL '1 day')
        ORDER BY session_id ASC, created_at ASC, id ASC
        """,
        requests_anchor_day,
    )
    recommendation_values_by_day = _count_recommendation_requests(
        recommendation_message_rows,
        window_start=date.fromordinal(requests_anchor_day.toordinal() - 6),
        window_end=requests_anchor_day,
    )
    recommendation_activity_rows = _build_daily_series(
        requests_anchor_day,
        recommendation_values_by_day,
        "recommendations",
    )
    recent_rows = await conn.fetch(
        """
        SELECT *
        FROM (
            SELECT CONCAT('user-', id::text) AS item_id, COALESCE(NULLIF(email, ''), NULLIF(name, ''), 'Пользователь') AS actor, 'Зарегистрирован новый пользователь'::text AS action, created_at AS event_time, 'success'::text AS status FROM users
            UNION ALL
            SELECT CONCAT('product-', id::text) AS item_id, COALESCE(NULLIF(name, ''), 'Продукт') AS actor, 'Добавлен новый продукт'::text AS action, created_at AS event_time, 'success'::text AS status FROM products
            UNION ALL
            SELECT CONCAT('procedure-', id::text) AS item_id, COALESCE(NULLIF(name, ''), 'Процедура') AS actor, 'Добавлена новая процедура'::text AS action, created_at AS event_time, 'success'::text AS status FROM procedures
            UNION ALL
            SELECT CONCAT('content-', id::text) AS item_id, COALESCE(NULLIF(title, ''), 'Материал') AS actor, CASE WHEN published THEN 'Опубликован контент' ELSE 'Создан черновик контента' END AS action, created_at AS event_time, CASE WHEN published THEN 'success' ELSE 'pending' END AS status FROM content
        ) AS combined_events
        ORDER BY event_time DESC
        LIMIT 10
        """
    )
    return {
        "stats": {
            "users": int(totals_row["users"]) if totals_row else 0,
            "products": int(totals_row["products"]) if totals_row else 0,
            "procedures": int(totals_row["procedures"]) if totals_row else 0,
            "content": int(totals_row["content"]) if totals_row else 0,
        },
        "userActivityData": [
            {
                "date": _format_chart_date(row["day"]),
                "requests": int(row["requests"] or 0),
            }
            for row in user_activity_rows
        ],
        "recommendationActivityData": [
            dict(row)
            for row in recommendation_activity_rows
        ],
        "activityData": [
            dict(row)
            for row in recommendation_activity_rows
        ],
        "recentActivity": [
            {
                "id": idx + 1,
                "user": row["actor"],
                "action": row["action"],
                "time": _format_datetime(row["event_time"]),
                "status": row["status"],
            }
            for idx, row in enumerate(recent_rows)
        ],
    }


async def build_reports_summary_payload(conn, *, product_select_sql) -> dict:
    today_label = datetime.utcnow().strftime("%d.%m.%Y")
    monthly_rows = await conn.fetch(
        """
        WITH months AS (
            SELECT generate_series(date_trunc('month', CURRENT_DATE) - INTERVAL '5 months', date_trunc('month', CURRENT_DATE), INTERVAL '1 month')::date AS month_start
        ),
        users_monthly AS (
            SELECT date_trunc('month', created_at)::date AS month_start, COUNT(*)::int AS users
            FROM users
            WHERE created_at >= date_trunc('month', CURRENT_DATE) - INTERVAL '5 months'
            GROUP BY date_trunc('month', created_at)
        ),
        products_monthly AS (
            SELECT date_trunc('month', created_at)::date AS month_start, COUNT(*)::int AS products
            FROM products
            WHERE created_at >= date_trunc('month', CURRENT_DATE) - INTERVAL '5 months'
            GROUP BY date_trunc('month', created_at)
        )
        SELECT months.month_start, COALESCE(users_monthly.users, 0) AS users, COALESCE(products_monthly.products, 0) AS products
        FROM months
        LEFT JOIN users_monthly ON users_monthly.month_start = months.month_start
        LEFT JOIN products_monthly ON products_monthly.month_start = months.month_start
        ORDER BY months.month_start
        """
    )
    compatibility_row = await conn.fetchrow(
        f"""
        WITH scored_products AS (
            SELECT (
                CASE WHEN COALESCE(NULLIF(TRIM(category), ''), NULL) IS NOT NULL THEN 1 ELSE 0 END +
                CASE WHEN COALESCE(NULLIF(TRIM(description), ''), NULL) IS NOT NULL THEN 1 ELSE 0 END +
                CASE WHEN COALESCE(NULLIF(TRIM(active_ingredient), ''), NULL) IS NOT NULL THEN 1 ELSE 0 END +
                CASE WHEN COALESCE(NULLIF(TRIM(composition), ''), NULL) IS NOT NULL THEN 1 ELSE 0 END +
                CASE WHEN COALESCE(NULLIF(TRIM(application_info), ''), NULL) IS NOT NULL THEN 1 ELSE 0 END +
                CASE WHEN COALESCE(NULLIF(TRIM(CAST(purpose AS text)), ''), NULL) IS NOT NULL THEN 1 ELSE 0 END +
                CASE WHEN COALESCE(NULLIF(TRIM(CAST(skin_type AS text)), ''), NULL) IS NOT NULL THEN 1 ELSE 0 END
            ) AS profile_score
            FROM ({product_select_sql('p')}) AS hydrated_products
        )
        SELECT COALESCE(SUM(CASE WHEN profile_score >= 6 THEN 1 ELSE 0 END), 0)::int AS high_compatibility,
               COALESCE(SUM(CASE WHEN profile_score BETWEEN 3 AND 5 THEN 1 ELSE 0 END), 0)::int AS medium_compatibility,
               COALESCE(SUM(CASE WHEN profile_score <= 2 THEN 1 ELSE 0 END), 0)::int AS low_compatibility
        FROM scored_products
        """
    )
    users_report_row = await conn.fetchrow(
        """
        SELECT (SELECT COUNT(*)::int FROM users) AS total_users,
               (SELECT COUNT(*)::int FROM users WHERE created_at >= date_trunc('month', CURRENT_DATE)) AS new_this_month,
               (SELECT COUNT(*)::int FROM user_profiles WHERE extra_data ? 'skin_passport') AS users_with_skin_passport
        """
    )
    products_report_row = await conn.fetchrow(
        f"""
        SELECT (SELECT COUNT(*)::int FROM products) AS total_products,
               (SELECT COUNT(*)::int FROM products WHERE has_video = true) AS products_with_video,
               (SELECT COUNT(DISTINCT category)::int FROM ({product_select_sql('p')}) AS hydrated_products WHERE category IS NOT NULL AND TRIM(category) <> '') AS categories_count
        """
    )
    recommendations_report_row = await conn.fetchrow(
        """
        SELECT (SELECT COUNT(*)::int FROM user_profiles WHERE extra_data ? 'skin_passport') AS total_recommendations,
               (SELECT COUNT(*)::int FROM content WHERE published = true) AS accepted_recommendations,
               CASE WHEN (SELECT COUNT(*) FROM user_profiles WHERE extra_data ? 'skin_passport') = 0 THEN 0
                    ELSE ROUND(((SELECT COUNT(*)::numeric FROM content WHERE published = true) / (SELECT COUNT(*)::numeric FROM user_profiles WHERE extra_data ? 'skin_passport')) * 100)::int
               END AS satisfaction_rate
        """
    )
    return {
        "monthlyData": [
            {"month": _format_report_month(row["month_start"]), "users": int(row["users"] or 0), "products": int(row["products"] or 0)}
            for row in monthly_rows
        ],
        "compatibilityData": [
            {"name": "Высокая (>90%)", "value": int(compatibility_row["high_compatibility"] or 0) if compatibility_row else 0, "color": "#A7F3D0"},
            {"name": "Средняя (70-90%)", "value": int(compatibility_row["medium_compatibility"] or 0) if compatibility_row else 0, "color": "#FB6FE8"},
            {"name": "Низкая (<70%)", "value": int(compatibility_row["low_compatibility"] or 0) if compatibility_row else 0, "color": "#E0C3FC"},
        ],
        "reports": [
            {
                "id": 1,
                "name": "Отчет по пользователям",
                "description": "Статистика регистраций и заполнения паспортов кожи",
                "format": "PDF",
                "date": today_label,
                "data": {
                    "totalUsers": int(users_report_row["total_users"] or 0) if users_report_row else 0,
                    "newThisMonth": int(users_report_row["new_this_month"] or 0) if users_report_row else 0,
                    "usersWithSkinPassport": int(users_report_row["users_with_skin_passport"] or 0) if users_report_row else 0,
                },
            },
            {
                "id": 2,
                "name": "Отчет по продуктам",
                "description": "Заполненность каталога и мультимедиа",
                "format": "Excel",
                "date": today_label,
                "data": {
                    "totalProducts": int(products_report_row["total_products"] or 0) if products_report_row else 0,
                    "productsWithVideo": int(products_report_row["products_with_video"] or 0) if products_report_row else 0,
                    "categoriesCount": int(products_report_row["categories_count"] or 0) if products_report_row else 0,
                },
            },
            {
                "id": 3,
                "name": "Отчет по рекомендациям",
                "description": "Сводка по заполнению профилей и публикациям контента",
                "format": "PDF",
                "date": today_label,
                "data": {
                    "totalRecommendations": int(recommendations_report_row["total_recommendations"] or 0) if recommendations_report_row else 0,
                    "acceptedRecommendations": int(recommendations_report_row["accepted_recommendations"] or 0) if recommendations_report_row else 0,
                    "satisfactionRate": int(recommendations_report_row["satisfaction_rate"] or 0) if recommendations_report_row else 0,
                },
            },
        ],
    }
