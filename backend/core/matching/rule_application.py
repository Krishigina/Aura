from typing import List

from backend.core.matching.domain import (
    SCORE_MAX,
    MatchingProfile,
    MatchingRule,
    ProductMatchResult,
    normalize_inci_key,
)


def _profile_has_condition(profile: MatchingProfile, condition_type: str, condition_value: str) -> bool:
    expected = normalize_inci_key(condition_value)
    if condition_type == "accepted_insight":
        return expected in {normalize_inci_key(item) for item in profile.accepted_insights}
    values = profile.answers.get(condition_type, [])
    if not values and condition_type in {"concern", "goal"}:
        values = profile.answers.get(f"{condition_type}s", [])
    return expected in {normalize_inci_key(item) for item in values}


def _ingredient_position_weight(position: int) -> float:
    if position <= 5:
        return 1.0
    if position <= 10:
        return 0.7
    return 0.4


def apply_matching_rules(
    result: ProductMatchResult,
    *,
    ingredient_keys: set[str],
    ingredient_positions: dict[str, int],
    profile: MatchingProfile,
    rules: List[MatchingRule],
) -> bool:
    for rule in rules:
        if rule.status != "confirmed":
            continue
        if rule.target_type != "ingredient":
            continue
        target_key = normalize_inci_key(rule.target_key)
        if target_key not in ingredient_keys:
            continue
        if not _profile_has_condition(profile, rule.condition_type, rule.condition_value):
            continue
        position_factor = _ingredient_position_weight(ingredient_positions.get(target_key, 99))
        weighted_delta = rule.weight_delta * position_factor

        if rule.source_id is not None:
            result.evidence_sources.append(rule.source_id)

        if rule.effect == "block":
            result.decision = "exclude"
            result.final_score = 0
            result.compatibility_percent = 0
            result.score_breakdown = {key: 0 for key in SCORE_MAX}
            result.contraindications.append(rule.evidence_quote)
            result.reason_codes.append(f"rule:{rule.id}:block")
            result.explanations.append(rule.evidence_quote or "Продукт исключен подтвержденным правилом безопасности")
            return True

        if rule.effect == "warning":
            result.decision = "caution"
            result.warnings.append(rule.evidence_quote)
            result.reason_codes.append(f"rule:{rule.id}:warning")
            if weighted_delta < 0:
                result.score_breakdown["safety_fit"] = max(0, result.score_breakdown["safety_fit"] + int(round(weighted_delta)))
            result.explanations.append(rule.evidence_quote or "Есть предупреждение по составу")
            continue

        if rule.effect == "boost":
            result.score_breakdown["ingredient_function_fit"] = min(
                SCORE_MAX["ingredient_function_fit"],
                result.score_breakdown["ingredient_function_fit"] + max(0, int(round(weighted_delta))),
            )
            result.reason_codes.append(f"rule:{rule.id}:boost")
            if rule.condition_type == "goal":
                result.matched_goals.append(rule.condition_value)
            if rule.condition_type == "concern":
                result.matched_concerns.append(rule.condition_value)
            result.explanations.append(rule.evidence_quote or f"{rule.target_key} соответствует профилю пользователя")
            continue

        if rule.effect == "penalty":
            result.score_breakdown["ingredient_function_fit"] = max(
                0, result.score_breakdown["ingredient_function_fit"] + int(round(weighted_delta))
            )
            result.reason_codes.append(f"rule:{rule.id}:penalty")
            result.explanations.append(rule.evidence_quote or "Есть штрафующий фактор по составу")
    return False
