package com.aura.feature.catalog

import androidx.compose.foundation.Image
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
import com.aura.core.ui.components.SoftPastelBackground
import com.aura.core.ui.components.SoftPastelVariant
import com.aura.core.ui.theme.AppState
import com.aura.core.ui.theme.Lavender
import com.aura.core.ui.theme.MintGreen

@Composable
fun CatalogScreen(
    apiClient: AuraApiClient,
    onProductClick: (Int) -> Unit = {}
) {
    val dark = isSystemInDarkTheme() || AppState.isDarkMode
    val bg = if (dark) Color(0xFF0A0A0A) else Color(0xFFF4F7FE)
    val glassAlpha = if (dark) 0.08f else 0.45f
    val glassBorderAlpha = if (dark) 0.15f else 0.6f
    val textPrimary = if (dark) Color(0xFFF1F5F9) else Color(0xFF1E293B)
    val textSecondary = if (dark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val textBody = if (dark) Color(0xFFCBD5E1) else Color(0xFF334155)

    var products by remember { mutableStateOf<List<Product>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            products = apiClient.getProducts()
            error = null
        } catch (e: Exception) {
            error = e.message ?: "Ошибка загрузки"
            e.printStackTrace()
        } finally {
            isLoading = false
        }
    }

    val filteredProducts = remember(products, searchQuery, selectedFilter) {
        products.filter { product ->
            val matchesSearch = searchQuery.isEmpty() ||
                product.name?.contains(searchQuery, ignoreCase = true) == true ||
                product.brand?.contains(searchQuery, ignoreCase = true) == true
            val matchesFilter = selectedFilter == null ||
                product.product_type == selectedFilter ||
                (product.skin_type?.any { it == selectedFilter } == true) ||
                (product.purpose?.any { it == selectedFilter } == true)
            matchesSearch && matchesFilter
        }
    }

    val filterChips = remember(products) {
        val types = products.mapNotNull { it.product_type }.distinct().filter { it.isNotBlank() }
        types.take(6)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
    ) {
        SoftPastelBackground(dark = dark, variant = SoftPastelVariant.Catalog)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 56.dp, bottom = 100.dp)
        ) {
            Text(
                text = "Каталог",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = textPrimary
            )

            Spacer(modifier = Modifier.height(20.dp))

            SearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                glassAlpha = glassAlpha,
                glassBorderAlpha = glassBorderAlpha
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (filterChips.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    filterChips.forEach { filter ->
                        FilterChip(
                            text = filter,
                            isSelected = selectedFilter == filter,
                            onClick = {
                                selectedFilter = if (selectedFilter == filter) null else filter
                            },
                            textPrimary = textPrimary,
                            textSecondary = textSecondary,
                            dark = dark
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (isLoading) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    repeat(4) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            SkeletonCard(modifier = Modifier.weight(1f), dark = dark)
                            SkeletonCard(modifier = Modifier.weight(1f), dark = dark)
                        }
                    }
                }
            } else if (error != null) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Ошибка: $error",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFEF4444)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Продуктов: ${products.size}",
                            style = MaterialTheme.typography.bodySmall,
                            color = textSecondary
                        )
                    }
                }
            } else if (filteredProducts.isEmpty()) {
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
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    filteredProducts.chunked(2).forEach { rowProducts ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowProducts.forEach { product ->
                                ProductCard(
                                    product = product,
                                    onClick = { onProductClick(product.id) },
                                    modifier = Modifier.weight(1f),
                                    textPrimary = textPrimary,
                                    textSecondary = textSecondary,
                                    glassAlpha = glassAlpha,
                                    glassBorderAlpha = glassBorderAlpha,
                                    dark = dark
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
        if (dark) Color.White.copy(alpha = 0.15f) else MintGreen.copy(alpha = 0.5f)
    } else {
        if (dark) Color.White.copy(alpha = 0.05f) else Color.White.copy(alpha = 0.4f)
    }
    val borderColor = if (isSelected) {
        if (dark) Color.White.copy(alpha = 0.3f) else MintGreen
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
private fun SkeletonCard(
    modifier: Modifier = Modifier,
    dark: Boolean
) {
    val placeholder = if (dark) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = if (dark) 0.05f else 0.5f))
            .border(1.dp, Color.White.copy(alpha = if (dark) 0.1f else 0.4f), RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(placeholder)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .height(12.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(placeholder)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(placeholder)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp, 20.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(placeholder)
                )
                Box(
                    modifier = Modifier
                        .size(50.dp, 14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(placeholder)
                )
            }
        }
    }
}

@Composable
private fun ProductCard(
    product: Product,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    textPrimary: Color,
    textSecondary: Color,
    glassAlpha: Float,
    glassBorderAlpha: Float,
    dark: Boolean
) {
    val hasImage = !product.photos.isNullOrEmpty() || !product.imageUrl.isNullOrBlank()
    val previewPhotoData = remember(product.photos) {
        product.photos?.firstOrNull { it.data.isNotBlank() }?.data
    }
    val previewBitmap = remember(previewPhotoData) {
        previewPhotoData?.let { decodeBase64ToImageBitmap(it) }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = glassAlpha))
            .border(1.dp, Color.White.copy(alpha = glassBorderAlpha), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (dark) Color.White.copy(alpha = 0.05f)
                        else Color(0xFFF1F5F9)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (previewBitmap != null) {
                    Image(
                        bitmap = previewBitmap,
                        contentDescription = product.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else if (hasImage) {
                    Icon(
                        imageVector = Icons.Rounded.Spa,
                        contentDescription = null,
                        tint = textSecondary.copy(alpha = 0.5f),
                        modifier = Modifier.size(40.dp)
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "AURA",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Lavender
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

            Text(
                text = product.brand ?: "Бренд",
                style = MaterialTheme.typography.labelMedium,
                color = textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = product.name ?: "Название",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (dark) MintGreen.copy(alpha = 0.2f) else MintGreen.copy(alpha = 0.3f)
                ) {
                    Text(
                        text = "98%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (dark) Color(0xFF6EE7B7) else Color(0xFF059669),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

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