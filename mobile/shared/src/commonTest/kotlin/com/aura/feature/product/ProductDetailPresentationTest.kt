package com.aura.feature.product

import com.aura.core.domain.model.Product
import com.aura.core.domain.model.ScoreBreakdown
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProductDetailPresentationTest {
    @Test
    fun evidenceStatusNoteAppearsForAutoExtractedFacts() {
        val result = evidenceStatusNote(listOf("auto_only", "mixed"))

        assertEquals(
            "Часть выводов основана на автоматически извлеченных источниках и еще не проверена экспертом.",
            result,
        )
    }

    @Test
    fun evidenceStatusNoteHiddenForConfirmedOnlyFacts() {
        val result = evidenceStatusNote(listOf("confirmed"))

        assertEquals(null, result)
    }

    @Test
    fun brandTrustSectionHiddenWhenOnlyBrandPresent() {
        val product = Product(brand = "Avene")

        assertFalse(shouldShowBrandTrustSection(product))
    }

    @Test
    fun brandTrustSectionShownWhenAnyTrustSignalPresent() {
        val product = Product(country_origin = "France")

        assertTrue(shouldShowBrandTrustSection(product))
    }

    @Test
    fun brandTrustPreviewUsesCountryOriginCountryAndManufacturer() {
        val product = Product(country_origin = "France", country = "EU", manufacturer = "Pierre Fabre")

        assertEquals("France · EU · Pierre Fabre", brandTrustPreview(product))
    }

    @Test
    fun brandTrustPreviewFallsBackToTapHintWhenNoFields() {
        assertEquals("Нажмите, чтобы открыть", brandTrustPreview(Product()))
    }

    @Test
    fun hasAdditionalInciIngredientsTrueWhenMoreThanPreviewLimit() {
        assertTrue(hasAdditionalInciIngredients("A, B, C, D, E, F"))
    }

    @Test
    fun hasAdditionalInciIngredientsFalseWhenAtPreviewLimitOrBelow() {
        assertFalse(hasAdditionalInciIngredients("A, B, C, D, E"))
    }

    @Test
    fun ctaTextShowsLoadingMessageWhenBusy() {
        assertEquals("Открываем чат...", assistantCtaLabel(isLoading = true))
    }

    @Test
    fun ctaTextShowsDefaultMessage() {
        assertEquals("Спросить чат", assistantCtaLabel(isLoading = false))
    }

    @Test
    fun assistantPrimaryCtaUsesShortLabelFromRedesignPlan() {
        assertEquals("Спросить чат", assistantCtaPrimaryLabel())
    }

    @Test
    fun addToRoutineCtaUsesPlanLabel() {
        assertEquals("Добавить в избранное", addToRoutineCtaLabel())
    }

    @Test
    fun buildBrandHeroMetaWithBrandAndOriginFormatting() {
        val product = Product(brand = "La Roche-Posay", country_origin = "France")

        assertEquals("La Roche-Posay \u00b7 France", buildBrandHeroMeta(product))
    }

    @Test
    fun buildBrandHeroMetaUsesOnlyAvailableValues() {
        val product = Product(brand = "Bioderma", country_origin = "")

        assertEquals("Bioderma", buildBrandHeroMeta(product))
    }

    @Test
    fun buildBrandHeroMetaEmptyWhenNothingAvailable() {
        assertEquals("", buildBrandHeroMeta(Product()))
    }

    @Test
    fun buildBrandHeroMetaContainsBrandOnlyOnce() {
        val product = Product(brand = "Avene", country_origin = "France")

        assertEquals(1, buildBrandHeroMeta(product).split("Avene").size - 1)
    }

    @Test
    fun shouldShowBrandSectionHiddenWhenAllFieldsEmpty() {
        val product = Product()

        assertFalse(shouldShowBrandSection(product))
    }

    @Test
    fun shouldShowBrandSectionShownWhenAnyFieldPresent() {
        val product = Product(manufacturer = "L'Oreal")

        assertTrue(shouldShowBrandSection(product))
    }

    @Test
    fun shouldShowBrandSectionHiddenWhenOnlyBrandPresent() {
        val product = Product(brand = "Avene")

        assertFalse(shouldShowBrandSection(product))
    }

    @Test
    fun parseInciPreviewLimitsToFirstFiveIngredients() {
        val preview = parseInciPreview("A, B, C, D, E, F, G")

        assertEquals(listOf("A", "B", "C", "D", "E"), preview)
    }

    @Test
    fun parseInciPreviewTrimsAndRemovesBlanks() {
        val preview = parseInciPreview("  A  , , B ,,   C   ")

        assertEquals(listOf("A", "B", "C"), preview)
    }

    @Test
    fun parseInciPreviewNegativeLimitReturnsEmptyList() {
        val preview = parseInciPreview("A, B, C", limit = -1)

        assertEquals(emptyList(), preview)
    }

    @Test
    fun parseInciPreviewProducesOrderedTrimmedPreviewForScreenList() {
        val preview = parseInciPreview(" Water , ,Glycerin,  Niacinamide ,Panthenol,Allantoin ")

        assertEquals(
            listOf("Water", "Glycerin", "Niacinamide", "Panthenol", "Allantoin"),
            preview,
        )
    }

    @Test
    fun primaryReasonsLimitsToTwo() {
        val reasons = listOf("r1", "r2", "r3")

        assertEquals(listOf("r1", "r2"), primaryReasons(reasons))
    }

    @Test
    fun scoreBreakdownRowsUseIngredientFirstFieldsWithLegacyFallbacks() {
        val breakdown = ScoreBreakdown(
            safety = 7,
            rules = 6,
            skinType = 5,
            skinState = 4,
            semantic = 3,
            ingredientFunctionFit = 21,
            skinStateFit = 12,
            safetyFit = 18,
            evidenceQuality = 8,
            metadataConfirmation = 2,
        )

        val rows = scoreBreakdownRows(breakdown)

        assertEquals(61, scoreBreakdownTotal(breakdown))
        assertEquals("Функции ингредиентов", rows[0].label)
        assertEquals(21, rows[0].value)
        assertEquals(18, rows[2].value)
    }

    @Test
    fun scoreBreakdownRowsFallbackToLegacyFieldsWhenNewFieldsAbsent() {
        val breakdown = ScoreBreakdown(safety = 7, rules = 6, skinType = 5, skinState = 4, semantic = 3, routine = 2)

        assertEquals(27, scoreBreakdownTotal(breakdown))
        assertEquals(6, scoreBreakdownRows(breakdown)[0].value)
        assertEquals(7, scoreBreakdownRows(breakdown)[2].value)
    }

    @Test
    fun primaryReasonsSkipsBlankItems() {
        val reasons = listOf("", "good for hydration", "  ", "supports barrier")

        assertEquals(listOf("good for hydration", "supports barrier"), primaryReasons(reasons, limit = 2))
    }

    @Test
    fun brandSectionMayBeVisibleWhileHeroMetaIsBlank() {
        val product = Product(manufacturer = "L'Oreal")

        assertTrue(shouldShowBrandSection(product))
        assertEquals("", buildVisibleBrandHeroMeta(product))
    }

    @Test
    fun heroBrandBlockVisibleWhenHeroMetaHasBrandOrOrigin() {
        val product = Product(brand = "Avene")

        assertTrue(shouldShowHeroBrandBlock(product))
    }

    @Test
    fun heroBrandBlockHiddenWhenBrandSectionHasNoData() {
        val product = Product()

        assertFalse(shouldShowHeroBrandBlock(product))
    }

    @Test
    fun primaryReasonsNegativeLimitReturnsEmptyList() {
        val reasons = listOf("r1", "r2", "r3")

        assertEquals(emptyList(), primaryReasons(reasons, limit = -1))
    }

    @Test
    fun compatibilityPercentIsClamped() {
        assertEquals(100, normalizeCompatibilityPercent(170))
        assertEquals(0, normalizeCompatibilityPercent(-6))
    }

    @Test
    fun primaryDecisionReasonsLimitedToTwo() {
        val reasons = listOf("r1", "r2", "r3")

        assertEquals(listOf("r1", "r2"), decisionReasons(reasons))
    }

    @Test
    fun decisionReasonsKeepsOnlyTwoNonBlankItems() {
        val result = decisionReasons(listOf("", "Подходит по типу кожи", "", "Нет комедогенных рисков", "Лишний"))

        assertEquals(listOf("Подходит по типу кожи", "Нет комедогенных рисков"), result)
    }

    @Test
    fun decisionStateLabelReturnsInsufficientDataWhenNoSignals() {
        val result = decisionStateLabel(decision = null, compatibilityPercent = null, reasons = emptyList())

        assertEquals("Недостаточно данных", result)
    }

    @Test
    fun decisionStateLabelReturnsInsufficientDataWhenReasonsAreBlank() {
        val result = decisionStateLabel(
            decision = null,
            compatibilityPercent = null,
            reasons = listOf("   ", "\t", "\n"),
        )

        assertEquals("Недостаточно данных", result)
    }

    @Test
    fun decisionSupportingLineFormatsPercentWhenAvailable() {
        val result = decisionSupportingLine(compatibilityPercent = 87)

        assertEquals("Индекс совместимости: 87%", result)
    }

    @Test
    fun decisionSupportingLineReturnsProfilePromptWhenMissing() {
        val result = decisionSupportingLine(compatibilityPercent = null)

        assertEquals("Заполните профиль кожи для точного индекса", result)
    }

    @Test
    fun keyCharacteristicsPreviewBuildsOrderedSummary() {
        val product = Product(product_type = "Сыворотка", category = "Уход", segment = "Премиум")

        assertEquals("Сыворотка · Уход · Премиум", keyCharacteristicsPreview(product))
    }

    @Test
    fun keyCharacteristicsPreviewUsesDefaultWhenNoKeyFields() {
        val product = Product(brand = "Avene")

        assertEquals("Нажмите, чтобы открыть", keyCharacteristicsPreview(product))
    }

    @Test
    fun keyCharacteristicsPreviewTrimsValuesAndSkipsWhitespaceOnlyItems() {
        val product = Product(product_type = "  Сыворотка  ", category = "   ", segment = " Премиум ")

        assertEquals("Сыворотка · Премиум", keyCharacteristicsPreview(product))
    }

    @Test
    fun keyCharacteristicsPreviewFallsBackWhenFieldsContainOnlyWhitespace() {
        val product = Product(product_type = "   ", category = "\t", segment = "\n")

        assertEquals("Нажмите, чтобы открыть", keyCharacteristicsPreview(product))
    }
}
