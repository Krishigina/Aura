package com.aura.core.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp

private data class ShimmerSpec(
    val durationMillis: Int,
    val highlightBlendFactor: Float,
    val highlightAlphaFactor: Float,
    val frostTint: Color,
    val frostTintBlendFactor: Float,
    val edgeBlendFactor: Float,
    val innerBlendFactor: Float,
    val coreBlendFactor: Float,
    val outerAlphaFactor: Float,
    val innerAlphaFactor: Float,
    val coreAlphaFactor: Float,
)

private val DefaultShimmerSpec = ShimmerSpec(
    durationMillis = 1600,
    highlightBlendFactor = 0.3f,
    highlightAlphaFactor = 1.22f,
    frostTint = Color(0xFFDFF6FF),
    frostTintBlendFactor = 0.38f,
    edgeBlendFactor = 0.4f,
    innerBlendFactor = 0.7f,
    coreBlendFactor = 0.9f,
    outerAlphaFactor = 1.06f,
    innerAlphaFactor = 1.14f,
    coreAlphaFactor = 1.24f,
)

fun Modifier.shimmerOverlay(
    baseColor: Color,
    shape: Shape,
    highlightColor: Color = Color.Unspecified,
    durationMillis: Int = DefaultShimmerSpec.durationMillis,
): Modifier = composed {
    val spec = DefaultShimmerSpec.copy(durationMillis = durationMillis)
    val transition = rememberInfiniteTransition(label = "shimmer")
    val progress = transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = spec.durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerProgress",
    )

    clip(shape).drawWithCache {
        val width = size.width
        val height = size.height
        val startX = width * (progress.value - 1f)
        val endX = startX + width
        val glossyHighlight = if (highlightColor == Color.Unspecified) {
            lerp(
                lerp(baseColor, spec.frostTint, spec.frostTintBlendFactor),
                Color.White,
                spec.highlightBlendFactor,
            ).copy(alpha = (baseColor.alpha * spec.highlightAlphaFactor).coerceIn(0f, 1f))
        } else {
            highlightColor
        }
        val brush = Brush.linearGradient(
            colors = listOf(
                baseColor,
                lerp(baseColor, glossyHighlight, spec.edgeBlendFactor)
                    .copy(alpha = (baseColor.alpha * spec.outerAlphaFactor).coerceIn(0f, 1f)),
                lerp(baseColor, glossyHighlight, spec.innerBlendFactor)
                    .copy(alpha = (baseColor.alpha * spec.innerAlphaFactor).coerceIn(0f, 1f)),
                lerp(baseColor, glossyHighlight, spec.coreBlendFactor)
                    .copy(alpha = (baseColor.alpha * spec.coreAlphaFactor).coerceIn(0f, 1f)),
                glossyHighlight,
                lerp(baseColor, glossyHighlight, spec.coreBlendFactor * 0.92f)
                    .copy(alpha = (baseColor.alpha * spec.coreAlphaFactor).coerceIn(0f, 1f)),
                lerp(baseColor, glossyHighlight, spec.innerBlendFactor * 0.9f)
                    .copy(alpha = (baseColor.alpha * spec.innerAlphaFactor).coerceIn(0f, 1f)),
                lerp(baseColor, glossyHighlight, spec.edgeBlendFactor * 0.82f)
                    .copy(alpha = (baseColor.alpha * spec.outerAlphaFactor).coerceIn(0f, 1f)),
                baseColor,
            ),
            start = Offset(startX, 0f),
            end = Offset(endX, height),
        )

        onDrawBehind {
            drawRect(brush = brush)
        }
    }
}
