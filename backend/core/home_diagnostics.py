from typing import Dict, List


def build_diagnostics_summary(answers: Dict[str, List[str]]):
    answers_count = sum(len(value) for value in answers.values())
    hydration = min(95, 35 + answers_count * 4)
    oiliness = max(5, 35 - answers_count)
    ph = round(5.0 + min(10, answers_count) * 0.05, 1)
    sensitivity = "Низкая"
    if answers_count >= 8:
        sensitivity = "Высокая"
    elif answers_count >= 4:
        sensitivity = "Средняя"
    battery = max(20, 85 - answers_count)

    return {
        "metrics": {
            "hydration": f"{hydration}%",
            "oiliness": f"{oiliness}%",
            "ph": f"{ph}",
            "sensitivity": sensitivity,
        },
        "device": {
            "name": "SkinSensor Pro",
            "status": "Подключено",
            "battery": f"{battery}%",
        },
    }
