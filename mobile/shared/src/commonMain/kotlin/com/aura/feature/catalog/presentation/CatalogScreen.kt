package com.aura.feature.catalog.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.aura.core.ui.components.SoftPastelBackground
import com.aura.core.ui.components.SoftPastelVariant
import com.aura.core.ui.components.auraToolbarTopOffset
import com.aura.core.ui.theme.aura
import com.aura.core.ui.theme.runtime.AppState
import com.aura.feature.catalog.CatalogProductFilters
import com.aura.feature.catalog.presentation.components.ProductCard
import com.aura.feature.catalog.presentation.components.SearchBar
import com.aura.feature.catalog.presentation.components.SkeletonCard
import org.koin.compose.koinInject

@Composable
fun CatalogRoute(
    filters: CatalogProductFilters,
    onFilterClick: () -> Unit = {},
    onProductClick: (Int) -> Unit = {},
    viewModel: CatalogViewModel = koinInject(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(filters) {
        viewModel.onFiltersChange(filters)
    }

    CatalogScreen(
        uiState = uiState,
        onSearchQueryChange = viewModel::onSearchQueryChange,
        onFilterClick = onFilterClick,
        onProductClick = onProductClick,
    )
}

@Composable
fun CatalogScreen(
    uiState: CatalogUiState,
    onSearchQueryChange: (String) -> Unit,
    onFilterClick: () -> Unit = {},
    onProductClick: (Int) -> Unit = {},
) {
    val dark = isSystemInDarkTheme() || AppState.isDarkMode
    val tokens = MaterialTheme.aura.catalog
    val bg = if (dark) tokens.backgroundDark else tokens.backgroundLight
    val glassAlpha = if (dark) tokens.glassDarkAlpha else tokens.glassLightAlpha
    val glassBorderAlpha = if (dark) tokens.glassBorderDarkAlpha else tokens.glassBorderLightAlpha
    val textPrimary = if (dark) tokens.textPrimaryDark else tokens.textPrimaryLight
    val textSecondary = if (dark) tokens.textSecondaryDark else tokens.textSecondaryLight
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bg),
    ) {
        SoftPastelBackground(dark = dark, variant = SoftPastelVariant.Catalog)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = tokens.screenHorizontalPadding)
                .padding(top = auraToolbarTopOffset(tokens.catalogTopOffset)),
            verticalArrangement = Arrangement.spacedBy(tokens.listGap),
            contentPadding = PaddingValues(bottom = tokens.catalogBottomPadding),
        ) {
            item {
                Text(
                    text = "Каталог",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary,
                )
            }

            item {
                SearchBar(
                    query = uiState.searchQuery,
                    onQueryChange = onSearchQueryChange,
                    activeFiltersCount = uiState.activeFiltersCount,
                    onFilterClick = onFilterClick,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    glassAlpha = glassAlpha,
                    glassBorderAlpha = glassBorderAlpha,
                )
            }

            if (uiState.isLoading) {
                items((0 until 4).toList()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(tokens.productRowGap),
                    ) {
                        SkeletonCard(modifier = Modifier.weight(1f), dark = dark)
                        SkeletonCard(modifier = Modifier.weight(1f), dark = dark)
                    }
                }
            } else if (uiState.error != null) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(tokens.emptyStateHeight),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Ошибка: ${uiState.error}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = tokens.errorColor,
                            )
                            Spacer(modifier = Modifier.height(tokens.emptyStateGap))
                            Text(
                                text = "Продуктов: ${uiState.products.size}",
                                style = MaterialTheme.typography.bodySmall,
                                color = textSecondary,
                            )
                        }
                    }
                }
            } else if (uiState.filteredProducts.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(tokens.emptyStateHeight),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Rounded.Spa,
                                contentDescription = null,
                                tint = textSecondary,
                                modifier = Modifier.size(tokens.emptyStateIconSize),
                            )
                            Spacer(modifier = Modifier.height(tokens.emptyStateGap))
                            Text(
                                text = "Нет продуктов",
                                style = MaterialTheme.typography.bodyLarge,
                                color = textSecondary,
                            )
                        }
                    }
                }
            } else {
                items(uiState.filteredProducts.chunked(2)) { rowProducts ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(tokens.productRowGap),
                    ) {
                        rowProducts.forEach { product ->
                            ProductCard(
                                product = product,
                                thumbnailData = uiState.thumbnailDataByProductId[product.id],
                                onClick = { onProductClick(product.id) },
                                modifier = Modifier.weight(1f),
                                textPrimary = textPrimary,
                                textSecondary = textSecondary,
                                glassAlpha = glassAlpha,
                                glassBorderAlpha = glassBorderAlpha,
                                dark = dark,
                            )
                        }
                        if (rowProducts.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}
