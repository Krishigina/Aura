package com.aura.core.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

enum class SoftPastelVariant {
    Default,
    Catalog,
    Auth,
    Home,
    Chat,
    Survey,
    Diagnostics,
    Splash,
}

@Composable
fun SoftPastelBackground(
    modifier: Modifier = Modifier,
    dark: Boolean = false,
    variant: SoftPastelVariant = SoftPastelVariant.Default,
) {
    val baseAlpha = if (dark) 0.12f else 0.28f
    val quietAlpha = if (dark) 0.08f else 0.18f
    val pink = Color(0xFFFBCFE8)
    val rose = Color(0xFFFFE4E6)
    val blue = Color(0xFFBFDBFE)
    val sky = Color(0xFFE0F2FE)
    val mint = Color(0xFFA7F3D0)
    val peach = Color(0xFFFFD8B5)
    val lavender = Color(0xFFE9D5FF)

    val accents = when (variant) {
        SoftPastelVariant.Catalog -> listOf(rose, sky, pink, mint)
        SoftPastelVariant.Auth -> listOf(pink, blue, lavender, peach)
        SoftPastelVariant.Home -> listOf(sky, rose, mint, peach)
        SoftPastelVariant.Chat -> listOf(blue, pink, lavender, mint)
        SoftPastelVariant.Survey -> listOf(rose, blue, mint, lavender)
        SoftPastelVariant.Diagnostics -> listOf(sky, mint, peach, pink)
        SoftPastelVariant.Splash -> listOf(pink, sky, peach, lavender)
        SoftPastelVariant.Default -> listOf(pink, blue, mint, peach)
    }

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize().blur(110.dp)) {
            val width = size.width
            val height = size.height

            fun softCircle(color: Color, center: Offset, radius: Float, alpha: Float) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            color.copy(alpha = alpha),
                            color.copy(alpha = alpha * 0.35f),
                            Color.Transparent,
                        ),
                        center = center,
                        radius = radius,
                    ),
                    radius = radius,
                    center = center,
                )
            }

            softCircle(accents[0], Offset(width * 0.05f, height * 0.08f), width * 0.72f, baseAlpha)
            softCircle(accents[1], Offset(width * 0.96f, height * 0.20f), width * 0.64f, quietAlpha)
            softCircle(accents[2], Offset(width * 0.22f, height * 0.86f), width * 0.70f, quietAlpha)
            softCircle(accents[3], Offset(width * 0.90f, height * 0.94f), width * 0.58f, baseAlpha * 0.72f)
        }
    }
}
