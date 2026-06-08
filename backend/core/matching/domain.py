import re
from dataclasses import dataclass, field
from typing import Dict, List, Optional


SCORE_MAX = {
    "ingredient_function_fit": 34,
    "skin_state_fit": 24,
    "evidence_quality": 20,
    "safety_fit": 15,
    "metadata_confirmation": 7,
}

HYDRATION_TERMS = {"glycerin", "hyaluronic acid", "sodium hyaluronate", "ceramide", "ceramides", "panthenol"}
SEBUM_TERMS = {"niacinamide", "zinc", "salicylic acid", "clay", "kaolin"}
AGGRESSIVE_TERMS = {"retinol", "ретин", "acid", "кислот", "peel", "пилинг", "salicylic acid"}
IRRITATION_RISK_TERMS = AGGRESSIVE_TERMS | {"glycolic acid", "lactic acid", "mandelic acid", "aha", "bha"}
HYDRATION_SENSOR_KEYS = ("hydration", "hydration_percent", "moisture", "moisture_percent")
DRYNESS_CONCERNS = {"dryness", "low_hydration", "dehydration", "сухость", "обезвоженность", "сухая кожа"}
FUNCTION_LABELS = {
    "hydration": "увлажнение",
    "barrier_support": "поддержка барьера",
    "sebum_control": "sebum-control",
    "acne_support": "поддержка при акне",
    "exfoliation": "эксфолиация",
    "soothing": "снижение чувствительности",
}


def normalize_inci_key(value: str) -> str:
    cleaned = re.sub(r"\s+", " ", str(value or "").strip())
    return cleaned.lower()


def parse_inci_ingredients(composition: str) -> List[Dict[str, object]]:
    items: List[Dict[str, object]] = []
    for token in re.split(r"[,;]", str(composition or "")):
        raw_name = token.strip().rstrip(".").strip()
        if not raw_name:
            continue
        items.append(
            {
                "raw_name": raw_name,
                "normalized_key": normalize_inci_key(raw_name),
                "position": len(items) + 1,
            }
        )
    return items


def get_fuzzy_hydration(sensor_value: float) -> dict[str, float]:
    fuzzy_sets = {"dry": 0.0, "normal": 0.0, "hydrated": 0.0}
    value = max(0.0, min(float(sensor_value), 100.0))

    if value <= 30:
        fuzzy_sets["dry"] = 1.0
    elif value < 50:
        fuzzy_sets["dry"] = (50 - value) / 20.0

    if 35 < value <= 50:
        fuzzy_sets["normal"] = (value - 35) / 15.0
    elif 50 < value < 75:
        fuzzy_sets["normal"] = (75 - value) / 25.0

    if 65 < value < 85:
        fuzzy_sets["hydrated"] = (value - 65) / 20.0
    elif value >= 85:
        fuzzy_sets["hydrated"] = 1.0

    return {key: round(score, 4) for key, score in fuzzy_sets.items()}


@dataclass(frozen=True)
class MatchingRule:
    id: int
    status: str
    target_type: str
    target_key: str
    condition_type: str
    condition_value: str
    effect: str
    weight_delta: float
    severity: str
    source_id: Optional[int]
    evidence_quote: str


@dataclass(frozen=True)
class ProductFunctionSignal:
    function_key: str
    score: float
    evidence_status: str = "auto_only"
    evidence_count: int = 0
    source_ids: List[int] = field(default_factory=list)


@dataclass(frozen=True)
class ProductCandidate:
    id: int
    name: str
    composition: str
    category: str = ""
    product_type: str = ""
    purpose: str = ""
    skin_type: str = ""
    function_signals: List[ProductFunctionSignal] = field(default_factory=list)


@dataclass(frozen=True)
class MatchingProfile:
    answers: Dict[str, List[str]]
    accepted_insights: List[str] = field(default_factory=list)
    skin_state: Dict[str, object] = field(default_factory=dict)


@dataclass
class ProductMatchResult:
    product_id: int
    decision: str
    final_score: float
    compatibility_percent: int = 0
    matched_goals: List[str] = field(default_factory=list)
    matched_concerns: List[str] = field(default_factory=list)
    warnings: List[str] = field(default_factory=list)
    contraindications: List[str] = field(default_factory=list)
    reason_codes: List[str] = field(default_factory=list)
    evidence_sources: List[int] = field(default_factory=list)
    score_breakdown: Dict[str, int] = field(default_factory=lambda: {key: 0 for key in SCORE_MAX})
    explanations: List[str] = field(default_factory=list)
    matched_functions: List[str] = field(default_factory=list)
    evidence_explanations: List[Dict[str, object]] = field(default_factory=list)


def calculate_compatibility_percent(decision: str, final_score: float) -> int:
    if decision == "exclude":
        return 0
    percent = max(0, min(int(round(final_score)), 100))
    if decision == "caution":
        if percent < 40:
            percent = max(percent, 35)
        return min(percent, 69)
    return percent


def apply_score_decision_threshold(result: ProductMatchResult) -> None:
    if result.decision == "exclude":
        return
    if result.final_score < 50:
        result.decision = "caution"
        if result.final_score < 40:
            result.explanations.append(
                "Низкая совместимость: средство не является приоритетной рекомендацией для текущего профиля"
            )
