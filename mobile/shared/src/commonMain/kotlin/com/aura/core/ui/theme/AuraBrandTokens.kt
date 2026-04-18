package com.aura.core.ui.theme

import androidx.compose.ui.graphics.Color

data class AuraBrandTokens(
    val logoGradient: List<Color>,
    val chatHeaderGlowGradient: List<Color>,
    val chatHeaderGlowAmbient: Color,
    val chatHeaderGlowSpot: Color,
)

fun defaultAuraBrandTokens() = AuraBrandTokens(
    logoGradient = listOf(
        Color(0xFFF8AFC4),
        Color(0xFFD6A4DF),
        Color(0xFFA8C8FF),
    ),
    chatHeaderGlowGradient = listOf(
        Color(0xFFF8AFC4).copy(alpha = 0.24f),
        Color(0xFFD6A4DF).copy(alpha = 0.22f),
        Color(0xFFA8C8FF).copy(alpha = 0.24f),
    ),
    chatHeaderGlowAmbient = Color(0xFFD6A4DF).copy(alpha = 0.32f),
    chatHeaderGlowSpot = Color(0xFFA8C8FF).copy(alpha = 0.34f),
)
