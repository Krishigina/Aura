package com.aura.feature.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.aura.core.ui.components.AuraTopBar
import com.aura.core.ui.components.SoftPastelBackground
import com.aura.core.ui.components.SoftPastelVariant
import com.aura.core.ui.components.auraToolbarContentTopPadding
import com.aura.core.ui.theme.MintGreen
import com.aura.core.ui.theme.aura
import com.aura.core.ui.theme.runtime.AppState
import com.aura.feature.catalog.presentation.CatalogFiltersViewModel
import com.aura.feature.catalog.presentation.components.CatalogFilterDropdown
import org.koin.compose.koinInject

@Composable
fun CatalogFilterScreen(
    initialFilters: CatalogProductFilters,
    onBack: () -> Unit,
    onApply: (CatalogProductFilters) -> Unit,
    viewModel: CatalogFiltersViewModel = koinInject(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val dark = isSystemInDarkTheme() || AppState.isDarkMode
    val tokens = MaterialTheme.aura.catalog
    val textPrimary = if (dark) tokens.textPrimaryDark else tokens.textPrimaryLight
    val textSecondary = if (dark) tokens.textSecondaryDark else tokens.textSecondaryLight
    val glassAlpha = if (dark) tokens.glassDarkAlpha else tokens.glassLightAlpha
    val glassBorderAlpha = if (dark) tokens.glassBorderDarkAlpha else tokens.glassBorderLightAlpha

    LaunchedEffect(initialFilters) {
        viewModel.load(initialFilters)
    }

    CatalogFiltersScreen(
        options = uiState.options,
        filters = uiState.filters,
        isLoading = uiState.isLoading,
        onFiltersChange = viewModel::updateFilters,
        onDismiss = onBack,
        onClear = viewModel::clearFilters,
        onApply = { onApply(uiState.filters) },
        textPrimary = textPrimary,
        textSecondary = textSecondary,
        glassAlpha = glassAlpha,
        glassBorderAlpha = glassBorderAlpha,
        dark = dark,
    )
}

@Composable
fun CatalogFiltersScreen(
    options: CatalogFilterOptions,
    filters: CatalogProductFilters,
    isLoading: Boolean,
    onFiltersChange: (CatalogProductFilters) -> Unit,
    onDismiss: () -> Unit,
    onClear: () -> Unit,
    onApply: () -> Unit,
    textPrimary: Color,
    textSecondary: Color,
    glassAlpha: Float,
    glassBorderAlpha: Float,
    dark: Boolean,
) {
    val tokens = MaterialTheme.aura.catalog
    val screenBackground = if (dark) tokens.backgroundDark else tokens.backgroundLight
    val panelBackground = if (dark) tokens.panelDark else Color.White.copy(alpha = glassAlpha.coerceAtLeast(0.88f))

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(screenBackground),
    ) {
        SoftPastelBackground(dark = dark, variant = SoftPastelVariant.Catalog)

        AuraTopBar(
            title = "\u0424\u0438\u043b\u044c\u0442\u0440\u044b",
            onBack = onDismiss,
            titleColor = textPrimary,
            iconTint = textSecondary,
            modifier = Modifier.align(Alignment.TopCenter),
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(horizontal = tokens.screenHorizontalPadding)
                .padding(top = auraToolbarContentTopPadding(), bottom = tokens.filterBottomPadding),
            verticalArrangement = Arrangement.spacedBy(tokens.listGap),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(tokens.filterPanelRadius))
                    .background(panelBackground)
                    .border(tokens.searchBorderWidth, Color.White.copy(alpha = glassBorderAlpha), RoundedCornerShape(tokens.filterPanelRadius))
                    .padding(tokens.filterPanelPadding)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(tokens.filterPanelGap),
            ) {
                CatalogFilterDropdown(
                    title = "\u0422\u0438\u043f \u043a\u043e\u0436\u0438",
                    options = options.skinTypes,
                    selected = filters.skinTypes,
                    onToggle = { onFiltersChange(filters.copy(skinTypes = filters.skinTypes.toggleValue(it))) },
                    isLoading = isLoading,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    dark = dark,
                )
                CatalogFilterDropdown(
                    title = "\u0422\u0438\u043f \u043f\u0440\u043e\u0434\u0443\u043a\u0442\u0430",
                    options = options.productTypes,
                    selected = filters.productTypes,
                    onToggle = { onFiltersChange(filters.copy(productTypes = filters.productTypes.toggleValue(it))) },
                    isLoading = isLoading,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    dark = dark,
                )
                CatalogFilterDropdown(
                    title = "\u041a\u0430\u0442\u0435\u0433\u043e\u0440\u0438\u044f",
                    options = options.segments,
                    selected = filters.segments,
                    onToggle = { onFiltersChange(filters.copy(segments = filters.segments.toggleValue(it))) },
                    isLoading = isLoading,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    dark = dark,
                )
                CatalogFilterDropdown(
                    title = "\u0420\u0430\u0437\u0434\u0435\u043b",
                    options = options.categories,
                    selected = filters.categories,
                    onToggle = { onFiltersChange(filters.copy(categories = filters.categories.toggleValue(it))) },
                    isLoading = isLoading,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    dark = dark,
                )
                CatalogFilterDropdown(
                    title = "\u0411\u0440\u0435\u043d\u0434",
                    options = options.brands,
                    selected = filters.brands,
                    onToggle = { onFiltersChange(filters.copy(brands = filters.brands.toggleValue(it))) },
                    isLoading = isLoading,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    dark = dark,
                )
                CatalogFilterDropdown(
                    title = "\u0421\u043e\u0432\u043c\u0435\u0441\u0442\u0438\u043c\u043e\u0441\u0442\u044c",
                    options = options.compatibilityRanges.map { compatibilityRangeLabel(it) },
                    selected = filters.compatibilityRanges.map { compatibilityRangeLabel(it) }.toSet(),
                    onToggle = {
                        val key = COMPATIBILITY_RANGE_KEYS.find { range -> compatibilityRangeLabel(range) == it } ?: return@CatalogFilterDropdown
                        onFiltersChange(filters.copy(compatibilityRanges = filters.compatibilityRanges.toggleValue(key)))
                    },
                    isLoading = isLoading,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    dark = dark,
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(tokens.filterPanelRadius))
                    .background(panelBackground)
                    .border(tokens.searchBorderWidth, Color.White.copy(alpha = glassBorderAlpha), RoundedCornerShape(tokens.filterPanelRadius))
                    .padding(tokens.filterFooterPadding),
                horizontalArrangement = Arrangement.spacedBy(tokens.filterFooterGap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onClear, modifier = Modifier.weight(1f)) {
                    Text("\u0421\u0431\u0440\u043e\u0441\u0438\u0442\u044c", color = textSecondary)
                }
                Button(
                    onClick = onApply,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(tokens.filterApplyRadius),
                    colors = ButtonDefaults.buttonColors(containerColor = MintGreen),
                ) {
                    Text("\u041f\u0440\u0438\u043c\u0435\u043d\u0438\u0442\u044c", color = textPrimary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

fun Set<String>.toggleValue(value: String): Set<String> {
    return if (contains(value)) this - value else this + value
}
