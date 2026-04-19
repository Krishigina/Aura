package com.aura.feature.catalog

import com.aura.feature.catalog.domain.model.CatalogProduct
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class CatalogLogicTest {
    @Test
    fun catalogProductListDoesNotHydrateFullPhotoPayloads() {
        assertFalse(shouldHydrateCatalogProductPhotos())
    }

    @Test
    fun filtersProductsWithOrInsideGroupsAndAndBetweenGroups() {
        val products = listOf(
            catalogProduct(id = 1, name = "Hydra Cream", brand = "Aura", productType = "Крем", segment = "Premium", category = "Уход", skinTypes = listOf("Сухая", "Чувствительная")),
            catalogProduct(id = 2, name = "Clear Gel", brand = "Vichy", productType = "Гель", segment = "Budget", category = "Очищение", skinTypes = listOf("Жирная")),
            catalogProduct(id = 3, name = "Repair Cream", brand = "CeraVe", productType = "Крем", segment = "Premium", category = "Уход", skinTypes = listOf("Нормальная")),
        )

        val filters = CatalogProductFilters(
            skinTypes = setOf("Сухая", "Нормальная"),
            productTypes = setOf("Крем"),
            segments = setOf("Premium"),
            brands = setOf("Aura", "CeraVe"),
        )

        val result = filterCatalogProducts(products, query = "", filters = filters)

        assertEquals(listOf(1, 3), result.map { it.id })
    }

    @Test
    fun buildsFilterOptionsFromLoadedProducts() {
        val products = listOf(
            catalogProduct(id = 1, brand = "Vichy", productType = "Крем", segment = "Premium", category = "Уход", skinTypes = listOf("Сухая", "Чувствительная")),
            catalogProduct(id = 2, brand = "Aura", productType = "Гель", segment = "Budget", category = "Очищение", skinTypes = listOf("Жирная")),
            catalogProduct(id = 3, brand = "Aura", productType = "Крем", segment = "Premium", category = "Уход", skinTypes = listOf("Сухая")),
        )

        val options = buildCatalogFilterOptions(products)

        assertEquals(listOf("Жирная", "Сухая", "Чувствительная"), options.skinTypes)
        assertEquals(listOf("Гель", "Крем"), options.productTypes)
        assertEquals(listOf("Budget", "Premium"), options.segments)
        assertEquals(listOf("Очищение", "Уход"), options.categories)
        assertEquals(listOf("Aura", "Vichy"), options.brands)
        assertEquals(COMPATIBILITY_RANGE_KEYS, options.compatibilityRanges)
    }

    @Test
    fun filtersProductsBySegment() {
        val products = listOf(
            catalogProduct(id = 1, name = "Budget Cream", brand = "Aura", segment = "Budget"),
            catalogProduct(id = 2, name = "Premium Cream", brand = "Aura", segment = "Premium"),
            catalogProduct(id = 3, name = "Middle Cream", brand = "Aura", segment = "Middle"),
        )

        val result = filterCatalogProducts(
            products = products,
            query = "",
            filters = CatalogProductFilters(segments = setOf("Budget", "Premium")),
        )

        assertEquals(listOf(1, 2), result.map { it.id })
    }

    @Test
    fun filtersProductsByCompatibilityRange() {
        val products = listOf(
            catalogProduct(id = 1, name = "Low Match", brand = "A", compatibilityPercent = 10),
            catalogProduct(id = 2, name = "Mid Match", brand = "B", compatibilityPercent = 42),
            catalogProduct(id = 3, name = "High Match", brand = "C", compatibilityPercent = 88),
            catalogProduct(id = 4, name = "Perfect Match", brand = "D", compatibilityPercent = 100),
            catalogProduct(id = 5, name = "No Percent", brand = "E", compatibilityPercent = null),
        )

        val lowRange = filterCatalogProducts(products, "", CatalogProductFilters(compatibilityRanges = setOf("0-25")))
        assertEquals(listOf(1), lowRange.map { it.id })

        val midRange = filterCatalogProducts(products, "", CatalogProductFilters(compatibilityRanges = setOf("25-50")))
        assertEquals(listOf(2), midRange.map { it.id })

        val highRange = filterCatalogProducts(products, "", CatalogProductFilters(compatibilityRanges = setOf("75-100")))
        assertEquals(listOf(3, 4), highRange.map { it.id })

        val multipleRanges = filterCatalogProducts(products, "", CatalogProductFilters(compatibilityRanges = setOf("0-25", "75-100")))
        assertEquals(listOf(1, 3, 4), multipleRanges.map { it.id })

        val emptyRange = filterCatalogProducts(products, "", CatalogProductFilters(compatibilityRanges = emptySet()))
        assertEquals(listOf(1, 2, 3, 4, 5), emptyRange.map { it.id })
    }

    @Test
    fun compatibilityRangesAffectActiveCount() {
        val noFilters = CatalogProductFilters()
        assertEquals(0, noFilters.activeCount)

        val withRanges = CatalogProductFilters(compatibilityRanges = setOf("0-25", "75-100"))
        assertEquals(2, withRanges.activeCount)

        val withSegments = CatalogProductFilters(segments = setOf("Budget", "Premium"))
        assertEquals(2, withSegments.activeCount)
    }

    @Test
    fun compatibilityRangeLabelFormatsCorrectly() {
        assertEquals("0–25%", compatibilityRangeLabel("0-25"))
        assertEquals("25–50%", compatibilityRangeLabel("25-50"))
        assertEquals("50–75%", compatibilityRangeLabel("50-75"))
        assertEquals("75–100%", compatibilityRangeLabel("75-100"))
    }

    private fun catalogProduct(
        id: Int,
        name: String? = null,
        brand: String? = null,
        productType: String? = null,
        segment: String? = null,
        category: String? = null,
        skinTypes: List<String> = emptyList(),
        compatibilityPercent: Int? = null,
    ) = CatalogProduct(
        id = id,
        name = name,
        brand = brand,
        productType = productType,
        segment = segment,
        category = category,
        skinTypes = skinTypes,
        compatibilityPercent = compatibilityPercent,
        photos = emptyList(),
        imageUrl = null,
        thumbnailUrl = null,
        hasVideo = false,
        decision = null,
        explanations = emptyList(),
    )
}
