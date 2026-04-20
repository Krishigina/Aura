package com.aura.feature.product.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import com.aura.core.domain.model.Product
import com.aura.core.ui.theme.Lavender
import com.aura.core.ui.theme.aura
import com.aura.feature.catalog.decodeBase64ToImageBitmap

@Composable
fun MediaZone(product: Product, onImageTap: (Int) -> Unit) {
    val tokens = MaterialTheme.aura.product
    val photos = product.photos.orEmpty()
    val previewPhotos = photos.take(6)
    val hasVideo = product.video != null || product.hasVideo
    var activeHeroIndex by remember(product.id, photos.size) { mutableStateOf(0) }
    val heroPhoto = photos.getOrNull(activeHeroIndex)
    val heroBitmap = remember(heroPhoto?.data) { heroPhoto?.data?.let { decodeBase64ToImageBitmap(it) } }

    LaunchedEffect(photos.size) {
        if (photos.isEmpty()) {
            activeHeroIndex = 0
        } else {
            activeHeroIndex = activeHeroIndex.coerceIn(0, photos.lastIndex)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(tokens.d10)) {
        if (photos.isEmpty() && !hasVideo) {
            Box(
                modifier = Modifier.fillMaxWidth().height(tokens.d190).clip(RoundedCornerShape(tokens.d24)).background(Lavender.copy(alpha = tokens.alpha18)),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("AURA", color = Lavender, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineSmall)
                    Text("Фото продукта пока не загружены", color = MaterialTheme.aura.product.textMuted)
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(tokens.d286)
                    .clip(RoundedCornerShape(tokens.d24))
                    .background(MaterialTheme.aura.product.skySoft.copy(alpha = tokens.alpha18))
                    .clickable { onImageTap(activeHeroIndex) },
                contentAlignment = Alignment.Center,
            ) {
                if (heroBitmap != null) {
                    Image(
                        bitmap = heroBitmap,
                        contentDescription = "Фото продукта ${activeHeroIndex + 1}",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Text("Фото ${activeHeroIndex + 1}", color = MaterialTheme.aura.product.textMuted)
                }
            }

            if (photos.size > 1) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    repeat(photos.size) { index ->
                        Box(
                            modifier = Modifier
                                .padding(horizontal = tokens.d4)
                                .size(if (index == activeHeroIndex) tokens.d8 else tokens.d6)
                                .clip(RoundedCornerShape(tokens.d52))
                                .background(
                                    if (index == activeHeroIndex) {
                                        MaterialTheme.aura.product.textPrimary
                                    } else {
                                        MaterialTheme.aura.product.textMuted.copy(alpha = tokens.alpha35)
                                    },
                                ),
                        )
                    }
                }
            }

            if (photos.size > 1 || hasVideo) {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(tokens.d12),
                ) {
                    previewPhotos.forEachIndexed { index, photo ->
                        val bitmap = remember(photo.data) { decodeBase64ToImageBitmap(photo.data) }
                        Box(
                            modifier = Modifier
                                .size(width = tokens.d92, height = tokens.d92)
                                .clip(RoundedCornerShape(tokens.d16))
                                .background(
                                    if (index == activeHeroIndex) {
                                        MaterialTheme.aura.product.lavenderSoft.copy(alpha = tokens.alpha34)
                                    } else {
                                        MaterialTheme.aura.product.skySoft.copy(alpha = tokens.alpha18)
                                    },
                                )
                                .clickable { activeHeroIndex = index },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap,
                                    contentDescription = "Превью фото ${index + 1}",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                )
                            } else {
                                Text("${index + 1}", color = MaterialTheme.aura.product.textMuted)
                            }
                        }
                    }
                    if (photos.size > previewPhotos.size) {
                        Box(
                            modifier = Modifier
                                .size(width = tokens.d92, height = tokens.d92)
                                .clip(RoundedCornerShape(tokens.d16))
                                .background(MaterialTheme.aura.product.lavenderSoft.copy(alpha = tokens.alpha25))
                                .clickable { onImageTap(previewPhotos.size) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "+${photos.size - previewPhotos.size}",
                                color = MaterialTheme.aura.product.textPrimary,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                    }
                    if (hasVideo) {
                        Box(
                            modifier = Modifier
                                .size(width = tokens.d120, height = tokens.d92)
                                .clip(RoundedCornerShape(tokens.d16))
                                .background(MaterialTheme.aura.product.lavenderSoft.copy(alpha = tokens.alpha25)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(tokens.d4),
                            ) {
                                Icon(
                                    Icons.Rounded.PlayArrow,
                                    contentDescription = null,
                                    tint = MaterialTheme.aura.product.pinkSoft,
                                    modifier = Modifier.size(tokens.d28),
                                )
                                Text("Видео", color = MaterialTheme.aura.product.textPrimary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
