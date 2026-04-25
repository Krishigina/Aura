from backend.core.matching.domain import (
    MatchingProfile,
    MatchingRule,
    ProductCandidate,
    ProductFunctionSignal,
    apply_score_decision_threshold,
    calculate_compatibility_percent,
    get_fuzzy_hydration,
    normalize_inci_key,
    parse_inci_ingredients,
)
from backend.core.matching.scoring import match_product
