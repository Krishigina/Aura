from typing import Dict, List, Optional

from backend.core.matching.domain import (
    DRYNESS_CONCERNS,
    FUNCTION_LABELS,
    HYDRATION_SENSOR_KEYS,
    HYDRATION_TERMS,
    IRRITATION_RISK_TERMS,
    SCORE_MAX,
    SEBUM_TERMS,
    MatchingProfile,
    ProductCandidate,
    ProductFunctionSignal,
    get_fuzzy_hydration,
    normalize_inci_key,
)


def _normalized_values(values: List[str]) -> set[str]:
    return {normalize_inci_key(item) for item in values if str(item).strip()}


def _profile_values(profile: MatchingProfile, key: str) -> set[str]:
    values = profile.answers.get(key, [])
    if not values and key in {"concern", "goal", "skin_type"}:
        plural = "skin_types" if key == "skin_type" else f"{key}s"
        values = profile.answers.get(plural, [])
    return _normalized_values(values)


def _extract_hydration_sensor_value(state: Dict[str, object]) -> Optional[float]:
    for key in HYDRATION_SENSOR_KEYS:
        value = state.get(key)
        if value is None:
            continue
        try:
            return float(value)
        except (TypeError, ValueError):
            continue
    return None


def state_sets(profile: MatchingProfile) -> tuple[set[str], set[str]]:
    state = profile.skin_state or {}
    raw_tags = state.get("state_tags", [])
    tags = _normalized_values([str(item) for item in raw_tags]) if isinstance(raw_tags, list) else set()
    raw_concerns = state.get("concerns", [])
    state_values = _normalized_values([str(item) for item in raw_concerns]) if isinstance(raw_concerns, list) else set()
    zone_concerns = state.get("zone_concerns", {})
    zone_values = set(state_values)
    if isinstance(zone_concerns, dict):
        for values in zone_concerns.values():
            if isinstance(values, list):
                zone_values.update(_normalized_values([str(item) for item in values]))
    return tags, zone_values


def desired_functions(profile: MatchingProfile) -> set[str]:
    concerns = _profile_values(profile, "concern") | _profile_values(profile, "goal")
    skin_types = _profile_values(profile, "skin_type")
    tags, zone_values = state_sets(profile)
    desired: set[str] = set()
    dryness_values = {"dry", "dryness", "dehydration", "dehydrated", "low_hydration", *DRYNESS_CONCERNS}
    if (
        concerns.intersection(dryness_values)
        or skin_types.intersection(dryness_values)
        or tags.intersection({"dehydrated_areas", "dry_areas"})
        or zone_values.intersection(dryness_values)
    ):
        desired.update({"hydration", "barrier_support"})
    if (
        concerns.intersection({"oiliness", "oily", "visible_pores"})
        or tags.intersection({"oily_t_zone", "oiliness"})
        or zone_values.intersection({"oiliness", "comedone_risk", "visible_pores"})
    ):
        desired.update({"sebum_control"})
    if concerns.intersection({"acne", "breakouts", "comedones"}) or zone_values.intersection({"comedone_risk"}):
        desired.update({"acne_support", "sebum_control"})
    if (
        concerns.intersection({"sensitivity", "sensitive"})
        or tags.intersection({"sensitive", "damaged_barrier"})
        or zone_values.intersection({"sensitivity", "damaged_barrier"})
    ):
        desired.update({"soothing", "barrier_support"})
    return desired


def _inferred_function_signals(ingredient_keys: set[str]) -> List[ProductFunctionSignal]:
    signals: List[ProductFunctionSignal] = []
    if ingredient_keys.intersection(HYDRATION_TERMS):
        signals.append(ProductFunctionSignal("hydration", 0.8, evidence_count=1))
    if ingredient_keys.intersection({"ceramide", "ceramides", "panthenol", "niacinamide"}):
        signals.append(ProductFunctionSignal("barrier_support", 0.6, evidence_count=1))
    if ingredient_keys.intersection(SEBUM_TERMS):
        signals.append(ProductFunctionSignal("sebum_control", 0.75, evidence_count=1))
    if ingredient_keys.intersection({"salicylic acid", "niacinamide"}):
        signals.append(ProductFunctionSignal("acne_support", 0.7, evidence_count=1))
    if ingredient_keys.intersection({"salicylic acid", "glycolic acid", "lactic acid"}):
        signals.append(ProductFunctionSignal("exfoliation", 0.75, evidence_count=1))
    if ingredient_keys.intersection(IRRITATION_RISK_TERMS):
        signals.append(ProductFunctionSignal("irritation_risk", 0.65, evidence_count=1))
    return signals


def build_function_signal_map(product: ProductCandidate, ingredient_keys: set[str]) -> Dict[str, ProductFunctionSignal]:
    by_key: Dict[str, ProductFunctionSignal] = {}
    for signal in [*_inferred_function_signals(ingredient_keys), *product.function_signals]:
        key = normalize_inci_key(signal.function_key)
        if not key:
            continue
        current = by_key.get(key)
        if current is None or float(signal.score or 0) > float(current.score or 0):
            by_key[key] = ProductFunctionSignal(
                function_key=key,
                score=max(0.0, min(float(signal.score or 0), 1.0)),
                evidence_status=signal.evidence_status or "auto_only",
                evidence_count=int(signal.evidence_count or 0),
                source_ids=list(signal.source_ids or []),
            )
    return by_key


def score_ingredient_function_fit(
    desired: set[str],
    signals: Dict[str, ProductFunctionSignal],
) -> tuple[int, List[str], List[str]]:
    if not desired:
        return 0, [], []
    matched = sorted(function for function in desired if function in signals)
    if not matched:
        return 0, [], []
    strongest = max(float(signals[function].score or 0) for function in matched)
    coverage = len(matched) / max(len(desired), 1)
    score = int(round(strongest * 27 + coverage * 3))
    labels = [FUNCTION_LABELS.get(function, function) for function in matched]
    return min(score, SCORE_MAX["ingredient_function_fit"]), matched, [f"Состав подтверждает функции: {', '.join(labels)}"]


def score_skin_state(
    signals: Dict[str, ProductFunctionSignal],
    product: ProductCandidate,
    profile: MatchingProfile,
) -> tuple[int, List[str]]:
    state = profile.skin_state or {}
    tags, zone_values = state_sets(profile)
    searchable = " ".join([product.purpose or "", product.product_type or "", product.name or ""]).lower()
    score = 0
    explanations: List[str] = []
    hydration_value = _extract_hydration_sensor_value(state)
    moisture_power = max(
        float(signals.get("hydration", ProductFunctionSignal("", 0)).score or 0),
        0.8 if "увлаж" in searchable or "hydration" in searchable or "moistur" in searchable else 0.0,
    )
    if hydration_value is not None and moisture_power > 0:
        fuzzy_hydration = get_fuzzy_hydration(hydration_value)
        fuzzy_score = int(round(fuzzy_hydration["dry"] * moisture_power * SCORE_MAX["skin_state_fit"]))
        if fuzzy_score > 0:
            score += fuzzy_score
            explanations.append("Нечеткая логика увлажнения: датчик показывает сухость, а продукт поддерживает увлажнение")
    profile_concerns = _profile_values(profile, "concern")
    has_dryness = bool(
        tags.intersection({"dehydrated_areas", "dry_areas"})
        or zone_values.intersection(DRYNESS_CONCERNS)
        or profile_concerns.intersection(DRYNESS_CONCERNS)
    )
    has_oiliness = bool(tags.intersection({"oily_t_zone", "oiliness"}) or zone_values.intersection({"oiliness", "comedone_risk", "visible_pores"}))
    if has_dryness and moisture_power > 0:
        score += 12
        explanations.append("Актуальный профиль кожи содержит сухие или обезвоженные зоны, продукт поддерживает увлажнение")
    if has_oiliness and ("sebum_control" in signals or "sebum" in searchable or "жир" in searchable or "oil" in searchable):
        score += 10
        explanations.append("Актуальный профиль кожи содержит жирность T-зоны, продукт поддерживает sebum-control")
    if (
        tags.intersection({"sensitive", "damaged_barrier"}) or zone_values.intersection({"sensitivity", "damaged_barrier"})
    ) and "irritation_risk" in signals:
        score = max(0, score - 5)
        explanations.append("Есть признаки чувствительности или ослабленного барьера, активные компоненты требуют осторожности")
    return min(score, SCORE_MAX["skin_state_fit"]), explanations


def score_safety(signals: Dict[str, ProductFunctionSignal], profile: MatchingProfile) -> tuple[int, List[str], List[str]]:
    score = SCORE_MAX["safety_fit"]
    warnings: List[str] = []
    explanations: List[str] = []
    concerns = _profile_values(profile, "concern")
    tags, zone_values = state_sets(profile)
    is_sensitive = bool(
        concerns.intersection({"sensitivity", "sensitive"})
        or tags.intersection({"sensitive", "damaged_barrier"})
        or zone_values.intersection({"sensitivity", "damaged_barrier"})
    )
    risk = signals.get("irritation_risk")
    if is_sensitive and risk:
        penalty = int(round(float(risk.score or 0) * SCORE_MAX["safety_fit"]))
        score = max(0, score - penalty)
        warning = "Чувствительная кожа: состав содержит факторы потенциального раздражения"
        warnings.append(warning)
        explanations.append(warning)
    return score, warnings, explanations


def score_evidence_quality(
    signals: Dict[str, ProductFunctionSignal],
    matched: List[str],
) -> tuple[int, List[Dict[str, object]]]:
    relevant = [signals[function] for function in matched if function in signals]
    if not relevant:
        return 0, []
    best = 0
    evidence: List[Dict[str, object]] = []
    for signal in relevant:
        status_points = 6 if signal.evidence_status == "confirmed" else 3 if signal.evidence_status == "auto_high_confidence" else 1
        count_points = min(int(signal.evidence_count or 0), 3)
        source_points = 1 if signal.source_ids else 0
        best = max(best, status_points + count_points + source_points)
        evidence.append(
            {
                "function_key": signal.function_key,
                "evidence_status": signal.evidence_status,
                "evidence_count": signal.evidence_count,
                "source_ids": signal.source_ids,
            }
        )
    return min(best, SCORE_MAX["evidence_quality"]), evidence


def score_metadata_confirmation(product: ProductCandidate, matched: List[str]) -> tuple[int, List[str]]:
    if not matched:
        return 0, []
    searchable = " ".join([product.purpose or "", product.skin_type or "", product.product_type or "", product.name or ""]).lower()
    confirms = False
    if "hydration" in matched and any(term in searchable for term in ["hydration", "moistur", "увлаж", "dry", "dryness", "сух"]):
        confirms = True
    if "sebum_control" in matched and any(term in searchable for term in ["oil", "sebum", "жир", "комбинирован"]):
        confirms = True
    if "acne_support" in matched and any(term in searchable for term in ["acne", "акне", "blemish"]):
        confirms = True
    if "barrier_support" in matched and any(term in searchable for term in ["barrier", "барьер", "sensitive", "dry", "сух"]):
        confirms = True
    if not confirms:
        return 0, []
    return SCORE_MAX["metadata_confirmation"], ["Метаданные продукта согласуются с функциями, подтвержденными составом"]
