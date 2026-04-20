package com.aura.feature.product.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import com.aura.core.domain.model.ProductPhoto
import com.aura.core.ui.theme.aura
import com.aura.feature.catalog.decodeBase64ToImageBitmap

@Composable
fun FullscreenMediaOverlay(
    photos: List<ProductPhoto>,
    currentIndex: Int,
    onClose: () -> Unit,
) {
    val tokens = MaterialTheme.aura.product
    var activeIndex by remember(photos, currentIndex) {
        mutableStateOf(currentIndex.coerceIn(0, (photos.size - 1).coerceAtLeast(0)))
    }
    var scale by remember(activeIndex) { mutableStateOf(1f) }
    var offsetX by remember(activeIndex) { mutableStateOf(0f) }
    var offsetY by remember(activeIndex) { mutableStateOf(0f) }
    val photo = photos.getOrNull(activeIndex)
    val bitmap = remember(photo?.data) { photo?.data?.let { decodeBase64ToImageBitmap(it) } }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.aura.product.lavenderSoft.copy(alpha = tokens.alpha90))
            .pointerInput(onClose) {
                detectTapGestures(onTap = { onClose() })
            },
        contentAlignment = Alignment.Center,
    ) {
        IconButton(
            onClick = onClose,
            modifier = Modifier.align(Alignment.TopEnd).padding(tokens.d16),
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Закрыть",
                tint = MaterialTheme.aura.product.textPrimary,
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(tokens.d16)
                .pointerInput(activeIndex, scale, photos.size) {
                    detectHorizontalDragGestures { _, dragAmount ->
                        if (scale > 1.02f) return@detectHorizontalDragGestures
                        if (dragAmount < -28f && activeIndex < photos.lastIndex) {
                            activeIndex += 1
                            scale = 1f
                            offsetX = 0f
                            offsetY = 0f
                        } else if (dragAmount > 28f && activeIndex > 0) {
                            activeIndex -= 1
                            scale = 1f
                            offsetX = 0f
                            offsetY = 0f
                        }
                    }
                }
                .pointerInput(activeIndex) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(1f, 4f)
                        if (scale > 1f) {
                            offsetX += pan.x
                            offsetY += pan.y
                        } else {
                            offsetX = 0f
                            offsetY = 0f
                        }
                    }
                }
                .pointerInput(activeIndex, scale) {
                    detectTapGestures(
                        onTap = {},
                        onDoubleTap = {
                            scale = if (scale > 1f) 1f else 2f
                            if (scale == 1f) {
                                offsetX = 0f
                                offsetY = 0f
                            }
                        },
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = "Фото продукта ${activeIndex + 1}",
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(tokens.d20))
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = offsetX
                            translationY = offsetY
                        },
                    contentScale = ContentScale.Fit,
                )
            } else {
                Text("Не удалось загрузить фото", color = MaterialTheme.aura.product.textPrimary)
            }
        }

        if (photos.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = tokens.d56)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(tokens.d8),
            ) {
                photos.forEachIndexed { index, _ ->
                    Box(
                        modifier = Modifier
                            .size(tokens.d44)
                            .clip(RoundedCornerShape(tokens.d8))
                            .background(if (index == activeIndex) MaterialTheme.aura.product.skySoft.copy(alpha = tokens.alpha90) else MaterialTheme.aura.product.skySoft.copy(alpha = tokens.alpha25))
                            .clickable {
                                activeIndex = index
                                scale = 1f
                                offsetX = 0f
                                offsetY = 0f
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text((index + 1).toString(), color = MaterialTheme.aura.product.textPrimary, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            Text(
                text = "${activeIndex + 1}/${photos.size}",
                color = MaterialTheme.aura.product.textPrimary,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = tokens.d24),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}
