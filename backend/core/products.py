import base64
import json
from typing import Any, Dict, List, Optional

from backend.core.storage import upload_dir, upload_path


def coerce_list_field(value: Any) -> List[str]:
    if value is None:
        return []
    if isinstance(value, list):
        return [str(item) for item in value if str(item).strip()]
    if isinstance(value, str):
        try:
            parsed = json.loads(value)
            if isinstance(parsed, list):
                return [str(item) for item in parsed if str(item).strip()]
        except json.JSONDecodeError:
            pass
        return [item.strip() for item in value.split(",") if item.strip()]
    return [str(value)]


def normalize_product_response(row: Any) -> Dict[str, Any]:
    product = dict(row)
    product["purpose"] = coerce_list_field(product.get("purpose"))
    product["skin_type"] = coerce_list_field(product.get("skin_type"))
    return product


def product_photo_content_type(filename: str) -> str:
    ext = str(filename or "").split(".")[-1].lower()
    content_type_map = {
        "jpg": "image/jpeg",
        "jpeg": "image/jpeg",
        "png": "image/png",
        "webp": "image/webp",
        "gif": "image/gif",
    }
    return content_type_map.get(ext, "image/jpeg")


MAX_INLINE_PRODUCT_PHOTO_BYTES = 5 * 1024 * 1024


def product_select_sql(alias: str = "p") -> str:
    return f"""
        SELECT
            {alias}.id,
            {alias}.name,
            COALESCE(br.value, {alias}.brand) AS brand,
            COALESCE(cat.value, {alias}.category) AS category,
            {alias}.description,
            COALESCE(vol.value, {alias}.volume) AS volume,
            COALESCE(seg.value, {alias}.segment) AS segment,
            {alias}.has_video,
            {alias}.video,
            {alias}.what_is_it,
            COALESCE(pt.value, {alias}.product_type) AS product_type,
            COALESCE(fw.value, {alias}.for_whom) AS for_whom,
            COALESCE(
                (
                SELECT json_agg(purpose_dict.value ORDER BY purpose_dict.value)
                FROM product_purpose_links purpose_links
                JOIN purposes purpose_dict ON purpose_dict.id = purpose_links.purpose_id
                WHERE purpose_links.product_id = {alias}.id
                )::text,
                {alias}.purpose
            ) AS purpose,
            COALESCE(
                (
                SELECT json_agg(skin_type_dict.value ORDER BY skin_type_dict.value)
                FROM product_skin_type_links skin_type_links
                JOIN skin_types skin_type_dict ON skin_type_dict.id = skin_type_links.skin_type_id
                WHERE skin_type_links.product_id = {alias}.id
                )::text,
                {alias}.skin_type
            ) AS skin_type,
            COALESCE(app_time.value, {alias}.application_time) AS application_time,
            COALESCE(area_dict.value, {alias}.area) AS area,
            {alias}.active_ingredient,
            {alias}.composition,
            {alias}.application_info,
            COALESCE(country_dict.value, {alias}.country) AS country,
            {alias}.created_at,
            {alias}.images,
            {alias}.brand AS legacy_brand,
            {alias}.category AS legacy_category,
            {alias}.volume AS legacy_volume,
            {alias}.segment AS legacy_segment,
            {alias}.product_type AS legacy_product_type,
            {alias}.for_whom AS legacy_for_whom,
            {alias}.purpose AS legacy_purpose,
            {alias}.skin_type AS legacy_skin_type,
            {alias}.application_time AS legacy_application_time,
            {alias}.area AS legacy_area,
            {alias}.country AS legacy_country,
            {alias}.brand_id,
            {alias}.category_id,
            {alias}.segment_id,
            {alias}.volume_id,
            {alias}.product_type_id,
            {alias}.for_whom_id,
            {alias}.application_time_id,
            {alias}.area_id,
            {alias}.country_id
        FROM products {alias}
        LEFT JOIN brands br ON br.id = {alias}.brand_id
        LEFT JOIN categories cat ON cat.id = {alias}.category_id
        LEFT JOIN segments seg ON seg.id = {alias}.segment_id
        LEFT JOIN volumes vol ON vol.id = {alias}.volume_id
        LEFT JOIN product_types pt ON pt.id = {alias}.product_type_id
        LEFT JOIN for_whom fw ON fw.id = {alias}.for_whom_id
        LEFT JOIN application_times app_time ON app_time.id = {alias}.application_time_id
        LEFT JOIN areas area_dict ON area_dict.id = {alias}.area_id
        LEFT JOIN countries country_dict ON country_dict.id = {alias}.country_id
    """


def load_product_photos_payload(photo_rows: List[Any], include_data: bool = True) -> List[Dict[str, Any]]:
    photos = []
    uploads_dir = upload_dir("product_photos")
    for row in photo_rows:
        row_data = dict(row)
        filename = row["filename"]
        product_id = row_data.get("product_id")
        photo_url = f"/api/products/{product_id}/photos/{row['id']}" if product_id else None
        if not include_data:
            photos.append(
                {
                    "id": row["id"],
                    "filename": filename,
                    "content_type": product_photo_content_type(filename),
                    "data": "",
                    "url": photo_url,
                }
            )
            continue
        file_path = uploads_dir / filename
        if file_path.exists():
            if file_path.stat().st_size > MAX_INLINE_PRODUCT_PHOTO_BYTES:
                photos.append(
                    {
                        "id": row["id"],
                        "filename": filename,
                        "content_type": product_photo_content_type(filename),
                        "data": "",
                        "url": photo_url,
                    }
                )
                continue
            with file_path.open("rb") as file_handle:
                data = base64.b64encode(file_handle.read()).decode()
            photos.append(
                {
                    "id": row["id"],
                    "filename": filename,
                    "content_type": product_photo_content_type(filename),
                    "data": data,
                    "url": photo_url,
                }
            )
    return photos


def load_product_video_payload(product_id: int, filename: Optional[str], include_data: bool = True) -> Optional[Dict[str, Any]]:
    if not filename:
        return None
    video_url = f"/api/products/{product_id}/video"
    if not include_data:
        return {"filename": filename, "data": "", "content_type": "video/mp4", "url": video_url}
    file_path = upload_path("product_videos", filename)
    if not file_path.exists():
        return None
    with file_path.open("rb") as file_handle:
        data = base64.b64encode(file_handle.read()).decode()
    return {"filename": filename, "data": data, "content_type": "video/mp4", "url": video_url}
