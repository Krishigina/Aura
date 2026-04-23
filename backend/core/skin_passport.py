from typing import Dict, List


def sanitize_skin_passport_answers(raw_answers) -> Dict[str, List[str]]:
    if not isinstance(raw_answers, dict):
        return {}

    sanitized: Dict[str, List[str]] = {}
    for key, value in raw_answers.items():
        if isinstance(value, list):
            sanitized[str(key)] = [str(item) for item in value]
    return sanitized
