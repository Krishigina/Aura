package com.aura.feature.catalog

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aura.core.data.api.AuraApiClient
import com.aura.core.domain.model.Product
import com.aura.core.ui.components.ProductNetworkImage
import com.aura.core.ui.theme.AppState
import com.aura.core.ui.theme.AuraPalette
import com.aura.core.ui.theme.auraThemeColors
import com.aura.feature.catalog.mvi.CatalogIntent
import com.aura.feature.catalog.mvi.CatalogStore

@Composable
fun CatalogScreen(
    apiClient: AuraApiClient,
    onProductClick: (Int) -> Unit = {}
) {
    val dark = isSystemInDarkTheme() || AppState.isDarkMode
    val theme = auraThemeColors(dark)
    val bg = theme.background
    val glassAlpha = if (dark) 0.08f else 0.45f
    val glassBorderAlpha = if (dark) 0.15f else 0.6f
    val textPrimary = theme.textPrimary
    val textSecondary = theme.textSecondary
    val textBody = theme.textBody

    val store = remember(apiClient) { CatalogStore(apiClient) }
    var uiState by remember { mutableStateOf(store.currentState()) }

    LaunchedEffect(Unit) {
        uiState = store.loadProducts()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
    ) {
        CatalogMeshBackground(dark = dark)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 56.dp, bottom = 100.dp)
        ) {
            // Header
            Text(
                text = "Каталог",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = textPrimary
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Search Bar
            SearchBar(
                query = uiState.searchQuery,
                onQueryChange = {
                    uiState = store.dispatch(CatalogIntent.SearchChanged(it))
                },
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                glassAlpha = glassAlpha,
                glassBorderAlpha = glassBorderAlpha
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Filter Chips
            if (uiState.filterChips.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    uiState.filterChips.forEach { filter ->
                        FilterChip(
                            text = filter,
                            isSelected = uiState.selectedFilter == filter,
                            onClick = {
                                uiState = store.dispatch(CatalogIntent.FilterToggled(filter))
                            },
                            textPrimary = textPrimary,
                            textSecondary = textSecondary,
                            dark = dark
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Products Grid
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AuraPalette.BrandMint)
                }
            } else if (uiState.error != null) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Ошибка: ${uiState.error}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AuraPalette.Error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Продуктов: ${uiState.products.size}",
                            style = MaterialTheme.typography.bodySmall,
                            color = textSecondary
                        )
                    }
                }
            } else if (uiState.filteredProducts.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Rounded.Spa,
                            contentDescription = null,
                            tint = textSecondary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Нет продуктов",
                            style = MaterialTheme.typography.bodyLarge,
                            color = textSecondary
                        )
                    }
                }
            } else {
                // Grid of products
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    uiState.filteredProducts.chunked(2).forEach { rowProducts ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowProducts.forEach { product ->
                                ProductCard(
                                    apiClient = apiClient,
                                    product = product,
                                    onClick = { onProductClick(product.id) },
                                    modifier = Modifier.weight(1f),
                                    textPrimary = textPrimary,
                                    textSecondary = textSecondary,
                                    textBody = textBody,
                                    glassAlpha = glassAlpha,
                                    glassBorderAlpha = glassBorderAlpha,
                                    dark = dark
                                )
                            }
                            // Fill empty space if odd number
                            if (rowProducts.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    textPrimary: Color,
    textSecondary: Color,
    glassAlpha: Float,
    glassBorderAlpha: Float
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = glassAlpha))
            .border(1.dp, Color.White.copy(alpha = glassBorderAlpha), RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = "Поиск",
                tint = textSecondary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f),
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = textPrimary),
                singleLine = true,
                decorationBox = { innerTextField ->
                    Box {
                        if (query.isEmpty()) {
                            Text(
                                text = "Поиск продуктов...",
                                style = MaterialTheme.typography.bodyLarge,
                                color = textSecondary
                            )
                        }
                        innerTextField()
                    }
                }
            )
        }
    }
}

@Composable
private fun FilterChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    textPrimary: Color,
    textSecondary: Color,
    dark: Boolean
) {
    val backgroundColor = if (isSelected) {
        if (dark) Color.White.copy(alpha = 0.15f) else AuraPalette.BrandMint.copy(alpha = 0.5f)
    } else {
        if (dark) Color.White.copy(alpha = 0.05f) else Color.White.copy(alpha = 0.4f)
    }
    val borderColor = if (isSelected) {
        if (dark) Color.White.copy(alpha = 0.3f) else AuraPalette.BrandMint
    } else {
        if (dark) Color.White.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.4f)
    }
    val textColor = if (isSelected) textPrimary else textSecondary

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
            color = textColor
        )
    }
}

@Composable
private fun ProductCard(
    apiClient: AuraApiClient,
    product: Product,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    textPrimary: Color,
    textSecondary: Color,
    textBody: Color,
    glassAlpha: Float,
    glassBorderAlpha: Float,
    dark: Boolean
) {
    var loadedPhotos by remember(product.id) { mutableStateOf<List<com.aura.core.domain.model.ProductPhoto>>(emptyList()) }

    LaunchedEffect(product.id) {
        loadedPhotos = apiClient.getProductPhotos(product.id)
    }

    val firstPhotoUrl = remember(apiClient, product.id, loadedPhotos, product.imageUrl) {
        val photoId = loadedPhotos.firstOrNull()?.id?.takeIf { it.isNotBlank() }
        when {
            photoId != null -> apiClient.getProductPhotoUrl(product.id, photoId)
            !product.imageUrl.isNullOrBlank() -> product.imageUrl
            else -> null
        }
    }

    val hasImage = firstPhotoUrl != null

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = glassAlpha))
            .border(1.dp, Color.White.copy(alpha = glassBorderAlpha), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Column {
            // Image or placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (dark) Color.White.copy(alpha = 0.05f)
                        else AuraPalette.SurfaceSoftBlue
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (hasImage && firstPhotoUrl != null) {
                    ProductNetworkImage(
                        url = firstPhotoUrl,
                        contentDescription = product.name ?: "Фото продукта",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // Aura logo placeholder
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "AURA",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = AuraPalette.BrandLavender
                        )
                        Icon(
                            imageVector = Icons.Rounded.Spa,
                            contentDescription = null,
                            tint = textSecondary.copy(alpha = 0.5f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Brand
            Text(
                text = product.brand ?: "Бренд",
                style = MaterialTheme.typography.labelMedium,
                color = textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Name
            Text(
                text = product.name ?: "Название",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Match percentage and type
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Match percentage badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (dark) AuraPalette.BrandMint.copy(alpha = 0.2f) else AuraPalette.BrandMint.copy(alpha = 0.3f)
                ) {
                    Text(
                        text = "98%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (dark) AuraPalette.SuccessSoft else AuraPalette.Success,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                // Product type
                if (!product.product_type.isNullOrBlank()) {
                    Text(
                        text = product.product_type,
                        style = MaterialTheme.typography.labelSmall,
                        color = textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(start = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CatalogMeshBackground(dark: Boolean) {
    val lavenderAlpha = if (dark) 0.1f else 0.3f
    val mintAlpha = if (dark) 0.08f else 0.3f

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.size(300.dp).offset(x = (-30).dp, y = (-30).dp)) {
            drawCircle(color = AuraPalette.BrandLavender.copy(alpha = lavenderAlpha))
        }
        Canvas(modifier = Modifier.size(250.dp).align(Alignment.TopEnd).offset(x = 30.dp, y = 50.dp)) {
            drawCircle(color = AuraPalette.BrandMint.copy(alpha = mintAlpha))
        }
        Canvas(modifier = Modifier.size(350.dp).align(Alignment.BottomStart).offset(x = (-30).dp, y = 200.dp)) {
            drawCircle(color = AuraPalette.BrandLavender.copy(alpha = lavenderAlpha))
        }
    }
}
