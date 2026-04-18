package com.aura.core.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class AuraSplashTokens(
    val baseColor: Color,
    val topGradientColor: Color,
    val bottomGradientColor: Color,
    val primaryGlowColor: Color,
    val secondaryGlowColor: Color,
    val mistColor: Color,
    val logoSweepColor: Color,
    val logoMarkColor: Color,
    val logoSize: Dp,
)

fun defaultAuraSplashTokens() = AuraSplashTokens(
    baseColor = Color.White,
    topGradientColor = Color(0xFFFCE9F1),
    bottomGradientColor = Color(0xFFEAF1FF),
    primaryGlowColor = Color(0xFFF8AFC4).copy(alpha = 0.72f),
    secondaryGlowColor = Color(0xFFD6A4DF).copy(alpha = 0.62f),
    mistColor = Color(0xFFA8C8FF).copy(alpha = 0.28f),
    logoSweepColor = Color(0xFFF8FDFF),
    logoMarkColor = Color.White,
    logoSize = 136.dp,
)
