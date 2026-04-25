from typing import Optional

from backend.core.matching.domain import (
    MatchingProfile,
    MatchingRule,
    ProductCandidate,
    ProductMatchResult,
    SCORE_MAX,
    apply_score_decision_threshold,
    calculate_compatibility_percent,
    parse_inci_ingredients,
)
from backend.core.matching.rule_application import apply_matching_rules
from backend.core.matching.signal_scoring import (
    build_function_signal_map,
    desired_functions,
    score_evidence_quality,
    score_ingredient_function_fit,
    score_metadata_confirmation,
    score_safety,
    score_skin_state,
)


def match_product(
    product: ProductCandidate,
    profile: MatchingProfile,
    rules: list[MatchingRule],
    semantic_score: Optional[float] = None,
) -> ProductMatchResult:
    ingredients = parse_inci_ingredients(product.composition)
    ingredient_keys = {item["normalized_key"] for item in ingredients}
    ingredient_positions = {item["normalized_key"]: int(item["position"]) for item in ingredients}
    result = ProductMatchResult(product_id=product.id, decision="recommend", final_score=0)
    result.score_breakdown["safety_fit"] = SCORE_MAX["safety_fit"]
    signals = build_function_signal_map(product, ingredient_keys)

    if apply_matching_rules(
        result,
        ingredient_keys=ingredient_keys,
        ingredient_positions=ingredient_positions,
        profile=profile,
        rules=rules,
    ):
        return result

    function_score, matched_functions, function_explanations = score_ingredient_function_fit(
        desired_functions(profile),
        signals,
    )
    result.score_breakdown["ingredient_function_fit"] += function_score
    result.matched_functions = matched_functions

    skin_state_score, skin_state_explanations = score_skin_state(signals, product, profile)
    safety_score, safety_warnings, safety_explanations = score_safety(signals, profile)
    evidence_score, evidence_explanations = score_evidence_quality(signals, matched_functions)
    if (result.matched_concerns or result.matched_goals) and result.evidence_sources:
        evidence_score = max(evidence_score, min(10, 6 + min(len(result.evidence_sources), 3)))
    metadata_score, metadata_explanations = score_metadata_confirmation(product, matched_functions)

    result.score_breakdown["safety_fit"] = min(result.score_breakdown["safety_fit"], safety_score)
    result.score_breakdown["skin_state_fit"] = skin_state_score
    result.score_breakdown["evidence_quality"] = evidence_score
    result.score_breakdown["metadata_confirmation"] = metadata_score
    result.warnings.extend(safety_warnings)
    result.evidence_explanations = evidence_explanations

    for evidence in evidence_explanations:
        for source_id in evidence.get("source_ids", []):
            if source_id not in result.evidence_sources:
                result.evidence_sources.append(source_id)

    result.explanations.extend(
        function_explanations
        + skin_state_explanations
        + safety_explanations
        + metadata_explanations
    )
    if semantic_score is not None:
        result.explanations.append("Семантическое сходство учтено отдельно от ingredient-first формулы")
    if result.warnings and result.decision != "exclude":
        result.decision = "caution"
    result.final_score = max(sum(result.score_breakdown.values()), 0)
    apply_score_decision_threshold(result)
    result.compatibility_percent = calculate_compatibility_percent(result.decision, result.final_score)
    return result
