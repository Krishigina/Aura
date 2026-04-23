def validate_passport_suggestion_payload(payload):
    if payload.suggestion_type == "update_field":
        if not payload.target_field:
            raise ValueError("target_field is required for update_field")
        values = payload.proposed_value.get("values")
        if isinstance(values, list):
            if not any(str(item).strip() for item in values):
                raise ValueError("proposed_value.values must contain at least one non-empty value")
            return
        value = payload.proposed_value.get("value")
        if value is None:
            value = payload.proposed_value.get("normalized_value")
        if value is None or not str(value).strip():
            raise ValueError("proposed_value must include a non-empty scalar value")
        return

    if payload.suggestion_type == "append_insight":
        value = payload.proposed_value.get("normalized_value")
        if value is None or not str(value).strip():
            raise ValueError("append_insight suggestions require proposed_value.normalized_value")


def validate_passport_suggestion_decision_status(status: str) -> str:
    normalized = (status or "").strip().lower()
    if normalized not in {"accepted", "rejected"}:
        raise ValueError("invalid decision status")
    return normalized
