from __future__ import annotations

import re
from dataclasses import dataclass, field


AUTO_CONFIDENCE_THRESHOLD = 0.82


@dataclass(frozen=True)
class SeedIngredient:
    canonical_name: str
    aliases: tuple[str, ...]
    default_effects: tuple[str, ...]


@dataclass
class ExtractedIngredientFact:
    ingredient_key: str
    effect_key: str
    condition_type: str | None
    condition_value: str | None
    matching_effect: str
    confidence: float
    evidence_status: str
    source_id: int | None
    source_title: str | None
    evidence_quote: str


@dataclass
class FunctionProfileEntry:
    effect_key: str
    score: float = 0.0
    evidence_count: int = 0
    evidence_status: str = "auto_only"
    ingredient_keys: set[str] = field(default_factory=set)


SEED_INGREDIENTS = (
    SeedIngredient(
        canonical_name="panthenol",
        aliases=("panthenol", "пантенол", "декспантенол", "dexpanthenol"),
        default_effects=("barrier_support", "soothing"),
    ),
    SeedIngredient(
        canonical_name="glycerin",
        aliases=("glycerin", "glycerol", "глицерин", "глицерина"),
        default_effects=("hydration",),
    ),
    SeedIngredient(
        canonical_name="niacinamide",
        aliases=("niacinamide", "ниацинамид", "никотинамид"),
        default_effects=("barrier_support", "tone_evening"),
    ),
    SeedIngredient(
        canonical_name="salicylic_acid",
        aliases=("salicylic acid", "салициловая кислота", "салициловой кислоты", "bha"),
        default_effects=("exfoliation", "anti_acne"),
    ),
    SeedIngredient(
        canonical_name="hyaluronic_acid",
        aliases=("hyaluronic acid", "sodium hyaluronate", "гиалуроновая кислота", "гиалуронат натрия"),
        default_effects=("hydration",),
    ),
    SeedIngredient(
        canonical_name="urea",
        aliases=("urea", "мочевина", "мочевины"),
        default_effects=("hydration", "keratolytic"),
    ),
    SeedIngredient(
        canonical_name="ceramide",
        aliases=("ceramide", "ceramides", "церамид", "церамиды", "керамид", "керамиды"),
        default_effects=("barrier_support",),
    ),
    SeedIngredient(
        canonical_name="retinol",
        aliases=("retinol", "ретинол", "ретинола"),
        default_effects=("renewal",),
    ),
    SeedIngredient(
        canonical_name="zinc",
        aliases=("zinc", "цинк", "цинка", "zinc pca", "цинк pca"),
        default_effects=("sebum_control", "anti_acne"),
    ),
    SeedIngredient(
        canonical_name="azelaic_acid",
        aliases=("azelaic acid", "азелаиновая кислота", "азелаиновой кислоты"),
        default_effects=("anti_acne", "tone_evening"),
    ),
)

def normalize_key(value: str) -> str:
    return re.sub(r"\s+", " ", value.strip().lower())


def resolve_seed_alias(value: str) -> SeedIngredient | None:
    normalized = normalize_key(value)
    for ingredient in SEED_INGREDIENTS:
        if normalized == ingredient.canonical_name:
            return ingredient
        if normalized in {normalize_key(alias) for alias in ingredient.aliases}:
            return ingredient
    return None


def extract_ingredient_facts(
    text: str,
    *,
    source_id: int | None = None,
    source_title: str | None = None,
) -> list[ExtractedIngredientFact]:
    facts: list[ExtractedIngredientFact] = []

    for sentence in _sentences(text):
        normalized_sentence = normalize_key(sentence)
        for ingredient in SEED_INGREDIENTS:
            if not _mentions_ingredient(normalized_sentence, ingredient):
                continue

            is_warning = _is_negated_recommendation(normalized_sentence)
            effect_key = "warning" if is_warning else _choose_effect(ingredient, normalized_sentence)
            condition_value = _condition_value(normalized_sentence)
            confidence = _confidence(normalized_sentence, condition_value)
            facts.append(
                ExtractedIngredientFact(
                    ingredient_key=ingredient.canonical_name,
                    effect_key=effect_key,
                    condition_type="concern" if condition_value else None,
                    condition_value=condition_value,
                    matching_effect="warning" if is_warning else "boost",
                    confidence=confidence,
                    evidence_status=(
                        "auto_high_confidence" if confidence >= AUTO_CONFIDENCE_THRESHOLD else "draft"
                    ),
                    source_id=source_id,
                    source_title=source_title,
                    evidence_quote=sentence.strip(),
                )
            )

    return facts


def aggregate_function_profile(facts: list[ExtractedIngredientFact]) -> dict[str, FunctionProfileEntry]:
    profile: dict[str, FunctionProfileEntry] = {}

    for fact in facts:
        if fact.evidence_status in {"draft", "rejected"}:
            continue

        entry = profile.setdefault(fact.effect_key, FunctionProfileEntry(effect_key=fact.effect_key))
        weight = 1.35 if fact.evidence_status == "confirmed" else 1.0
        entry.score += fact.confidence * weight
        entry.evidence_count += 1
        entry.ingredient_keys.add(fact.ingredient_key)

        if entry.evidence_status == "auto_only" and fact.evidence_status == "confirmed":
            entry.evidence_status = "confirmed" if entry.evidence_count == 1 else "mixed"
        elif entry.evidence_status == "confirmed" and fact.evidence_status != "confirmed":
            entry.evidence_status = "mixed"
        elif entry.evidence_status == "auto_only" and fact.evidence_status != "auto_high_confidence":
            entry.evidence_status = "mixed"

    return profile


def _sentences(text: str) -> list[str]:
    return [part.strip() for part in re.split(r"(?<=[.!?。])\s+|\n+", text) if part.strip()]


def _mentions_ingredient(normalized_sentence: str, ingredient: SeedIngredient) -> bool:
    aliases = (ingredient.canonical_name, *ingredient.aliases)
    return any(_contains_phrase(normalized_sentence, normalize_key(alias)) for alias in aliases)


def _contains_phrase(text: str, phrase: str) -> bool:
    return re.search(rf"(?<!\w){re.escape(phrase)}(?!\w)", text) is not None


def _is_negated_recommendation(normalized_sentence: str) -> bool:
    return any(
        phrase in normalized_sentence
        for phrase in ("не рекомендуется", "не рекомендовано", "не следует")
    )


def _choose_effect(ingredient: SeedIngredient, normalized_sentence: str) -> str:
    if any(word in normalized_sentence for word in ("сух", "ксероз", "hydration", "увлаж")):
        if "hydration" in ingredient.default_effects:
            return "hydration"
    if any(word in normalized_sentence for word in ("барьер", "barrier")):
        if "barrier_support" in ingredient.default_effects:
            return "barrier_support"
    return ingredient.default_effects[0]


def _condition_value(normalized_sentence: str) -> str | None:
    if any(word in normalized_sentence for word in ("сух", "ксероз", "dryness", "xerosis")):
        return "dryness"
    if any(word in normalized_sentence for word in ("акне", "acne")):
        return "acne"
    return None


def _confidence(normalized_sentence: str, condition_value: str | None) -> float:
    recommendation_words = (
        "рекоменд",
        "показан",
        "применение",
        "помогает",
        "поддерживать",
        "указано",
        "recommended",
        "helps",
    )
    confidence = 0.45
    if condition_value:
        confidence += 0.22
    if any(word in normalized_sentence for word in recommendation_words):
        confidence += 0.2
    if re.search(r"\d+\s*(?:-|%)", normalized_sentence):
        confidence += 0.05
    return min(confidence, 0.95)
