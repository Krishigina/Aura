package com.aura.feature.catalog

import com.aura.feature.catalog.domain.model.CatalogProduct

fun shouldHydrateCatalogProductPhotos(): Boolean = false

fun maxCatalogThumbnailPrefetchCount(): Int = 40

val COMPATIBILITY_RANGE_KEYS = listOf("0-25", "25-50", "50-75", "75-100")

data class CatalogProductFilters(
    val skinTypes: Set<String> = emptySet(),
    val productTypes: Set<String> = emptySet(),
    val segments: Set<String> = emptySet(),
    val categories: Set<String> = emptySet(),
    val brands: Set<String> = emptySet(),
    val compatibilityRanges: Set<String> = emptySet(),
) {
    val activeCount: Int
        get() = skinTypes.size + productTypes.size + segments.size + categories.size + brands.size + compatibilityRanges.size
}

data class CatalogFilterOptions(
    val skinTypes: List<String> = emptyList(),
    val productTypes: List<String> = emptyList(),
    val segments: List<String> = emptyList(),
    val categories: List<String> = emptyList(),
    val brands: List<String> = emptyList(),
    val compatibilityRanges: List<String> = COMPATIBILITY_RANGE_KEYS,
)

fun filterCatalogProducts(
    products: List<CatalogProduct>,
    query: String,
    filters: CatalogProductFilters,
): List<CatalogProduct> {
    val normalizedQuery = query.trim()
    return products.filter { product ->
        val matchesSearch = normalizedQuery.isEmpty() ||
            product.name.containsValue(normalizedQuery) ||
            product.brand.containsValue(normalizedQuery)

        val matchesFilters = product.skinTypes.matchesAny(filters.skinTypes) &&
            product.productType.matchesAny(filters.productTypes) &&
            product.segment.matchesAny(filters.segments) &&
            product.category.matchesAny(filters.categories) &&
            product.brand.matchesAny(filters.brands) &&
            product.compatibilityPercent.matchesAnyRange(filters.compatibilityRanges)

        matchesSearch && matchesFilters
    }
}

fun buildCatalogFilterOptions(products: List<CatalogProduct>): CatalogFilterOptions {
    return CatalogFilterOptions(
        skinTypes = products.flatMap { it.skinTypes }.cleanSortedDistinct(),
        productTypes = products.mapNotNull { it.productType }.cleanSortedDistinct(),
        segments = products.mapNotNull { it.segment }.cleanSortedDistinct(),
        categories = products.mapNotNull { it.category }.cleanSortedDistinct(),
        brands = products.mapNotNull { it.brand }.cleanSortedDistinct(),
    )
}

private fun String?.containsValue(value: String): Boolean {
    return this?.contains(value, ignoreCase = true) == true
}

private fun String?.matchesAny(selected: Set<String>): Boolean {
    if (selected.isEmpty()) return true
    val value = this?.trim().orEmpty()
    return value.isNotEmpty() && selected.any { it.equals(value, ignoreCase = true) }
}

private fun List<String>?.matchesAny(selected: Set<String>): Boolean {
    if (selected.isEmpty()) return true
    return orEmpty().any { productValue ->
        selected.any { it.equals(productValue.trim(), ignoreCase = true) }
    }
}

private fun Iterable<String>.cleanSortedDistinct(): List<String> {
    return map { it.trim() }
        .filter { it.isMeaningfulFilterValue() }
        .distinctBy { it.lowercase() }
        .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it })
}

private fun String.isMeaningfulFilterValue(): Boolean {
    if (isBlank()) return false
    val normalized = trim()
    return normalized.any { it.isLetterOrDigit() } && normalized.any { it != '?' && it != '�' }
}

fun compatibilityRangeLabel(rangeKey: String): String {
    val parts = rangeKey.split("-")
    if (parts.size != 2) return rangeKey
    val low = parts[0]
    val high = parts[1]
    return "$low-$high%"
}

private fun Int?.matchesAnyRange(selected: Set<String>): Boolean {
    if (selected.isEmpty()) return true
    val percent = this ?: return false
    return selected.any { rangeKey ->
        val parts = rangeKey.split("-")
        if (parts.size != 2) return@any false
        val low = parts[0].toIntOrNull() ?: return@any false
        val high = parts[1].toIntOrNull() ?: return@any false
        if (high == 100) percent in low..high else percent in low until high
    }
}
