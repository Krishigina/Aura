package com.aura.feature.product

import com.aura.core.domain.model.Product
import com.aura.core.domain.model.ScoreBreakdown

data class ScoreBreakdownRow(val label: String, val value: Int, val max: Int)

private fun ScoreBreakdown.hasIngredientFirstScores(): Boolean {
    return ingredientFunctionFit != 0 || skinStateFit != 0 || safetyFit != 0 || evidenceQuality != 0 || metadataConfirmation != 0
}

fun scoreBreakdownRows(breakdown: ScoreBreakdown): List<ScoreBreakdownRow> {
    return if (breakdown.hasIngredientFirstScores()) {
        listOf(
            ScoreBreakdownRow("Функции ингредиентов", breakdown.ingredientFunctionFit, 45),
            ScoreBreakdownRow("Состояние кожи", breakdown.skinStateFit, 20),
            ScoreBreakdownRow("Безопасность", breakdown.safetyFit, 20),
            ScoreBreakdownRow("Качество доказательств", breakdown.evidenceQuality, 10),
            ScoreBreakdownRow("Метаданные", breakdown.metadataConfirmation, 5),
        )
    } else {
        listOf(
            ScoreBreakdownRow("Состав и правила", breakdown.rules, 25),
            ScoreBreakdownRow("Тип кожи", breakdown.skinType, 15),
            ScoreBreakdownRow("Безопасность", breakdown.safety, 30),
            ScoreBreakdownRow("Состояние кожи", breakdown.skinState, 15),
            ScoreBreakdownRow("AI-сходство", breakdown.semantic, 10),
            ScoreBreakdownRow("Рутина", breakdown.routine, 5),
        )
    }
}

fun scoreBreakdownTotal(breakdown: ScoreBreakdown): Int {
    return scoreBreakdownRows(breakdown).sumOf { it.value }
}

fun buildBrandHeroMeta(product: Product): String {
    val brand = product.brand.orEmpty().trim()
    val origin = product.country_origin.orEmpty().trim()

    return listOf(brand, origin)
        .filter { it.isNotBlank() }
        .joinToString(" \u00b7 ")
}

fun buildVisibleBrandHeroMeta(product: Product): String {
    return buildBrandHeroMeta(product)
}

fun shouldShowHeroBrandBlock(product: Product): Boolean {
    return buildBrandHeroMeta(product).isNotBlank()
}

fun shouldShowBrandSection(product: Product): Boolean {
    return shouldShowBrandTrustSection(product)
}

fun shouldShowBrandTrustSection(product: Product): Boolean {
    return !product.description.isNullOrBlank() ||
        !product.country_origin.isNullOrBlank() ||
        !product.country.isNullOrBlank() ||
        !product.manufacturer.isNullOrBlank()
}

fun brandTrustPreview(product: Product): String {
    return listOfNotNull(product.country_origin, product.country, product.manufacturer)
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .joinToString(" · ")
        .ifBlank { "Нажмите, чтобы открыть" }
}

fun parseInciPreview(composition: String?, limit: Int = 5): List<String> {
    if (limit <= 0) {
        return emptyList()
    }

    return composition.orEmpty()
        .split(',')
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .take(limit)
}

fun primaryReasons(items: List<String>, limit: Int = 2): List<String> {
    if (limit <= 0) {
        return emptyList()
    }

    return items
        .filter { it.isNotBlank() }
        .take(limit)
}

fun evidenceStatusNote(statuses: List<String>): String? {
    return if (statuses.any { it == "auto_only" || it == "mixed" || it == "auto_high_confidence" }) {
        "Часть выводов основана на автоматически извлеченных источниках и еще не проверена экспертом."
    } else {
        null
    }
}

fun hasAdditionalInciIngredients(composition: String?, previewLimit: Int = 5): Boolean {
    if (previewLimit <= 0) {
        return false
    }

    return parseInciPreview(composition, limit = Int.MAX_VALUE).size > previewLimit
}

fun normalizeCompatibilityPercent(value: Int): Int = value.coerceIn(0, 100)

fun decisionStateLabel(decision: String?, compatibilityPercent: Int?, reasons: List<String>): String {
    val hasReasons = reasons.any { it.isNotBlank() }
    val hasSignal = compatibilityPercent != null || hasReasons

    if (!hasSignal) {
        return "Недостаточно данных"
    }

    return when (decision) {
        "exclude" -> "Не подходит"
        "caution" -> "С осторожностью"
        else -> "Подходит"
    }
}

fun decisionSupportingLine(compatibilityPercent: Int?): String {
    return if (compatibilityPercent != null) {
        "Индекс совместимости: ${normalizeCompatibilityPercent(compatibilityPercent)}%"
    } else {
        "Заполните профиль кожи для точного индекса"
    }
}

fun decisionReasons(items: List<String>): List<String> {
    return items
        .filter { it.isNotBlank() }
        .take(2)
}

fun keyCharacteristicsPreview(product: Product): String {
    return listOfNotNull(product.product_type, product.category, product.segment)
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .joinToString(" · ")
        .ifBlank { "Нажмите, чтобы открыть" }
}

fun assistantCtaLabel(isLoading: Boolean): String {
    return if (isLoading) "Открываем чат..." else assistantCtaPrimaryLabel()
}

fun assistantCtaPrimaryLabel(): String {
    return "Спросить чат"
}

fun addToRoutineCtaLabel(isInFavorites: Boolean = false): String {
    return if (isInFavorites) "В избранном" else "Добавить в избранное"
}
