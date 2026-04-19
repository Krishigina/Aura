package com.aura.feature.catalog.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.aura.core.ui.components.shimmerOverlay
import com.aura.core.ui.theme.AuraCatalogTokens
import com.aura.core.ui.theme.Lavender
import com.aura.core.ui.theme.MintGreen
import com.aura.core.ui.theme.aura
import com.aura.feature.catalog.decodeBase64ToImageBitmap
import com.aura.feature.catalog.domain.model.CatalogProduct

@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    activeFiltersCount: Int,
    onFilterClick: () -> Unit,
    textPrimary: Color,
    textSecondary: Color,
    glassAlpha: Float,
    glassBorderAlpha: Float,
) {
    val tokens = MaterialTheme.aura.catalog
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(tokens.searchRadius))
            .background(Color.White.copy(alpha = glassAlpha))
            .border(tokens.searchBorderWidth, Color.White.copy(alpha = glassBorderAlpha), RoundedCornerShape(tokens.searchRadius))
            .padding(horizontal = tokens.searchHorizontalPadding, vertical = tokens.searchVerticalPadding),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = "Поиск",
                tint = textSecondary,
                modifier = Modifier.size(tokens.searchIconSize),
            )
            Spacer(modifier = Modifier.width(tokens.searchIconGap))
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
                                color = textSecondary,
                            )
                        }
                        innerTextField()
                    }
                },
            )
            Spacer(modifier = Modifier.width(tokens.filterIconGap))
            IconButton(onClick = onFilterClick, modifier = Modifier.size(tokens.filterButtonSize)) {
                Box(contentAlignment = Alignment.TopEnd) {
                    Icon(
                        imageVector = Icons.Rounded.FilterList,
                        contentDescription = "Фильтры",
                        tint = if (activeFiltersCount > 0) MintGreen else textSecondary,
                        modifier = Modifier.align(Alignment.Center).size(tokens.filterIconSize),
                    )
                    if (activeFiltersCount > 0) {
                        Box(
                            modifier = Modifier
                                .size(tokens.filterBadgeSize)
                                .clip(RoundedCornerShape(tokens.filterBadgeRadius))
                                .background(tokens.errorColor),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = activeFiltersCount.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SkeletonCard(
    modifier: Modifier = Modifier,
    dark: Boolean,
) {
    val tokens = MaterialTheme.aura.catalog
    val placeholder = if (dark) Color.White.copy(alpha = tokens.skeletonPlaceholderDarkAlpha) else tokens.skeletonPlaceholderLight
    val lineShape = RoundedCornerShape(tokens.skeletonLineRadius)
    val cardShape = RoundedCornerShape(tokens.cardRadius)
    val imageShape = RoundedCornerShape(tokens.cardImageRadius)
    val badgeShape = RoundedCornerShape(tokens.decisionBadgeRadius)
    Box(
        modifier = modifier
            .clip(cardShape)
            .fillMaxHeight()
            .heightIn(min = tokens.cardMinHeight)
            .background(Color.White.copy(alpha = if (dark) tokens.skeletonCardDarkAlpha else tokens.skeletonCardLightAlpha))
            .border(tokens.searchBorderWidth, Color.White.copy(alpha = if (dark) tokens.skeletonBorderDarkAlpha else tokens.skeletonBorderLightAlpha), cardShape)
            .padding(tokens.cardPadding),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(tokens.cardImageHeight)
                    .shimmerOverlay(baseColor = placeholder, shape = imageShape),
            )
            Spacer(modifier = Modifier.height(tokens.skeletonImageGap))
            Box(
                modifier = Modifier
                    .fillMaxWidth(tokens.skeletonTitleWidthFraction)
                    .height(tokens.skeletonTitleHeight)
                    .shimmerOverlay(baseColor = placeholder, shape = lineShape),
            )
            Spacer(modifier = Modifier.height(tokens.skeletonLineGap))
            Box(
                modifier = Modifier
                    .fillMaxWidth(tokens.skeletonSubtitleWidthFraction)
                    .height(tokens.skeletonSubtitleHeight)
                    .shimmerOverlay(baseColor = placeholder, shape = lineShape),
            )
            Spacer(modifier = Modifier.height(tokens.skeletonMetaGap))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(tokens.skeletonBadgeSizeWidth, tokens.skeletonBadgeSizeHeight)
                        .shimmerOverlay(baseColor = placeholder, shape = badgeShape),
                )
                Box(
                    modifier = Modifier
                        .size(tokens.skeletonMetaSizeWidth, tokens.skeletonMetaSizeHeight)
                        .shimmerOverlay(baseColor = placeholder, shape = lineShape),
                )
            }
        }
    }
}

@Composable
fun ProductCard(
    product: CatalogProduct,
    thumbnailData: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    textPrimary: Color,
    textSecondary: Color,
    glassAlpha: Float,
    glassBorderAlpha: Float,
    dark: Boolean,
) {
    val tokens = MaterialTheme.aura.catalog
    val hasImage = !product.photos.isNullOrEmpty() || !product.imageUrl.isNullOrBlank() || !product.thumbnailUrl.isNullOrBlank()
    val previewPhotoData = remember(product.photos, thumbnailData) {
        product.photos.firstOrNull { it.data.isNotBlank() }?.data ?: thumbnailData
    }
    val previewBitmap = remember(previewPhotoData) {
        previewPhotoData?.let { decodeBase64ToImageBitmap(it) }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(tokens.cardRadius))
            .fillMaxHeight()
            .heightIn(min = tokens.cardMinHeight)
            .background(Color.White.copy(alpha = glassAlpha))
            .border(tokens.searchBorderWidth, Color.White.copy(alpha = glassBorderAlpha), RoundedCornerShape(tokens.cardRadius))
            .clickable(onClick = onClick)
            .padding(tokens.cardPadding),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(tokens.cardImageHeight)
                    .clip(RoundedCornerShape(tokens.cardImageRadius))
                    .background(
                        if (dark) {
                            Color.White.copy(alpha = tokens.cardImageDarkAlpha)
                        } else {
                            tokens.cardImageLightColor
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (previewBitmap != null) {
                    Image(
                        bitmap = previewBitmap,
                        contentDescription = product.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else if (hasImage) {
                    Icon(
                        imageVector = Icons.Rounded.Spa,
                        contentDescription = null,
                        tint = textSecondary.copy(alpha = tokens.cardTintAlpha),
                        modifier = Modifier.size(tokens.cardPlaceholderIconLarge),
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "AURA",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Lavender,
                        )
                        Icon(
                            imageVector = Icons.Rounded.Spa,
                            contentDescription = null,
                            tint = textSecondary.copy(alpha = tokens.cardTintAlpha),
                            modifier = Modifier.size(tokens.cardPlaceholderIconSmall),
                        )
                    }
                }
                if (product.hasVideo) {
                    Surface(
                        modifier = Modifier.align(Alignment.TopEnd).padding(tokens.videoBadgeOuterPadding),
                        shape = RoundedCornerShape(tokens.videoBadgeRadius),
                        color = Color.Black.copy(alpha = tokens.videoBadgeAlpha),
                    ) {
                        Text(
                            text = "Видео",
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = tokens.videoBadgeHorizontalPadding, vertical = tokens.videoBadgeVerticalPadding),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(tokens.brandTopGap))

            Text(
                text = product.brand ?: "Бренд",
                style = MaterialTheme.typography.labelMedium,
                color = textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(modifier = Modifier.height(tokens.nameTopGap))

            Text(
                text = product.name ?: "Название",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(modifier = Modifier.height(tokens.metaTopGap))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                product.compatibilityPercent?.let { percent ->
                    Surface(
                        shape = RoundedCornerShape(tokens.decisionBadgeRadius),
                        color = decisionColor(product.decision, dark, tokens).copy(alpha = if (dark) tokens.decisionBadgeDarkAlpha else tokens.decisionBadgeLightAlpha),
                    ) {
                        Text(
                            text = "$percent%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = decisionColor(product.decision, dark, tokens),
                            modifier = Modifier.padding(horizontal = tokens.decisionBadgeHorizontalPadding, vertical = tokens.decisionBadgeVerticalPadding),
                        )
                    }
                }

                if (!product.productType.isNullOrBlank()) {
                    Text(
                        text = product.productType,
                        style = MaterialTheme.typography.labelSmall,
                        color = textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(start = tokens.productTypeStartPadding),
                    )
                }
            }

            product.decision?.let { decision ->
                Spacer(modifier = Modifier.height(tokens.decisionTopGap))
                Text(
                    text = decisionLabel(decision),
                    style = MaterialTheme.typography.labelSmall,
                    color = decisionColor(decision, dark, tokens),
                    fontWeight = FontWeight.Bold,
                )
            }

            product.explanations.firstOrNull()?.let { explanation ->
                Spacer(modifier = Modifier.height(tokens.explanationTopGap))
                Text(
                    text = explanation,
                    style = MaterialTheme.typography.labelSmall,
                    color = textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

fun decisionLabel(decision: String): String = when (decision) {
    "exclude" -> "Исключён"
    "caution" -> "С осторожностью"
    else -> "Подходит"
}

fun decisionColor(decision: String?, dark: Boolean, tokens: AuraCatalogTokens): Color = when (decision) {
    "exclude" -> tokens.errorColor
    "caution" -> tokens.warningColor
    else -> if (dark) tokens.positiveDark else tokens.positiveLight
}
