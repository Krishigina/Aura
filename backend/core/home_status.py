from typing import Any, Dict, Optional

import aiohttp


def build_home_air_quality(answers_count: int) -> str:
    if answers_count >= 8:
        return "Отличное"
    if answers_count <= 2:
        return "Умеренное"
    return "Хорошее"


def build_home_weather_advice(humidity_percent: int, uv_index: float) -> str:
    if uv_index >= 6.0:
        return "Высокий UV: используйте SPF-средства сегодня."
    if humidity_percent < 40:
        return "Низкая влажность: добавьте более интенсивное увлажнение."
    return "Влажность комфортная: выбирайте легкое увлажнение."


def build_fallback_home_status(answers_count: int) -> Dict[str, Any]:
    temperature = 19 + min(8, answers_count)
    uv_index = round(2.0 + min(8, answers_count) * 0.4, 1)
    humidity_percent = min(90, 38 + answers_count * 4)
    return {
        "weather": {"temperature": f"{temperature}°C", "uv_index": f"UV {uv_index}"},
        "top_widget": {
            "humidity_value": f"{humidity_percent}%",
            "humidity_subtitle": "Определено по профилю кожи",
            "air_quality": build_home_air_quality(answers_count),
            "air_status": "Актуально",
            "weather_advice": build_home_weather_advice(humidity_percent, uv_index),
        },
    }


def build_home_status_from_open_meteo(payload: Dict[str, Any], fallback_air_quality: str) -> Optional[Dict[str, Any]]:
    current = payload.get("current") if isinstance(payload, dict) else None
    daily = payload.get("daily") if isinstance(payload, dict) else None
    if not isinstance(current, dict) or not isinstance(daily, dict):
        return None
    temperature = current.get("temperature_2m")
    humidity = current.get("relative_humidity_2m")
    uv_values = daily.get("uv_index_max")
    uv_index = uv_values[0] if isinstance(uv_values, list) and uv_values else None
    if temperature is None or humidity is None or uv_index is None:
        return None
    try:
        temperature_value = round(float(temperature))
        humidity_value = round(float(humidity))
        uv_value = round(float(uv_index), 1)
    except (TypeError, ValueError):
        return None
    return {
        "weather": {"temperature": f"{temperature_value}°C", "uv_index": f"UV {uv_value}"},
        "top_widget": {
            "humidity_value": f"{humidity_value}%",
            "humidity_subtitle": "По данным погоды рядом с вами",
            "air_quality": fallback_air_quality,
            "air_status": "Актуально",
            "weather_advice": build_home_weather_advice(humidity_value, uv_value),
        },
    }


async def fetch_open_meteo_home_status(latitude: float, longitude: float, fallback_air_quality: str) -> Optional[Dict[str, Any]]:
    params = {"latitude": latitude, "longitude": longitude, "current": "temperature_2m,relative_humidity_2m", "daily": "uv_index_max", "timezone": "auto"}
    timeout = aiohttp.ClientTimeout(total=5)
    try:
        async with aiohttp.ClientSession(timeout=timeout) as session:
            async with session.get("https://api.open-meteo.com/v1/forecast", params=params) as response:
                if response.status >= 400:
                    return None
                payload = await response.json()
    except Exception:
        return None
    return build_home_status_from_open_meteo(payload, fallback_air_quality=fallback_air_quality)
