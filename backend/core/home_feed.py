from datetime import datetime


FEED_SUBTITLE_SEPARATOR = " / "


def build_home_feed_ritual_items(ritual_rows):
    ritual_items = []
    for idx, row in enumerate(ritual_rows):
        subtitle_chunks = [
            value for value in [row["application_time"], row["category"]] if value and str(value).strip()
        ]
        subtitle = FEED_SUBTITLE_SEPARATOR.join(subtitle_chunks) if subtitle_chunks else "Шаг ухода"
        ritual_items.append(
            {
                "id": f"product-{row['id']}",
                "title": row["name"] or "Продукт ухода",
                "subtitle": subtitle,
                "checked": False,
                "is_active": idx == 0,
                "is_warning": False,
            }
        )
    return ritual_items


def build_home_feed_insights(insights_rows):
    insights = []
    for idx, row in enumerate(insights_rows):
        created_at = row["created_at"]
        date_suffix = created_at.strftime("%d.%m") if isinstance(created_at, datetime) else ""
        subtitle = FEED_SUBTITLE_SEPARATOR.join([part for part in [row["category"] or "Контент", date_suffix] if part])
        insight_type = "profile"
        if idx == 1:
            insight_type = "result"
        elif idx >= 2:
            insight_type = "hydration"
        insights.append(
            {
                "id": f"content-{row['id']}",
                "title": row["title"] or "Новый инсайт",
                "subtitle": subtitle,
                "type": insight_type,
            }
        )
    return insights
