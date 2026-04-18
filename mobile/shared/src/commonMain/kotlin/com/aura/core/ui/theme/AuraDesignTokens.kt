package com.aura.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class AuraDesignTokens(
    val brand: AuraBrandTokens,
    val chat: AuraChatTokens,
    val auth: AuraAuthTokens,
    val diagnostics: AuraDiagnosticsTokens,
    val survey: AuraSurveyTokens,
    val splash: AuraSplashTokens,
    val navigation: AuraNavigationTokens,
    val profileSettings: AuraProfileSettingsTokens,
    val recommendations: AuraRecommendationsTokens,
    val catalog: AuraCatalogTokens,
    val home: AuraHomeTokens,
    val product: AuraProductTokens,
    val journal: AuraJournalTokens,
    val profile: AuraProfileTokens,
)

fun defaultAuraDesignTokens() = AuraDesignTokens(
    brand = defaultAuraBrandTokens(),
    chat = defaultAuraChatTokens(),
    auth = defaultAuraAuthTokens(),
    diagnostics = defaultAuraDiagnosticsTokens(),
    survey = defaultAuraSurveyTokens(),
    splash = defaultAuraSplashTokens(),
    navigation = defaultAuraNavigationTokens(),
    profileSettings = defaultAuraProfileSettingsTokens(),
    recommendations = defaultAuraRecommendationsTokens(),
    catalog = defaultAuraCatalogTokens(),
    home = defaultAuraHomeTokens(),
    product = defaultAuraProductTokens(),
    journal = defaultAuraJournalTokens(),
    profile = defaultAuraProfileTokens(),
)

val LocalAuraDesignTokens = staticCompositionLocalOf { defaultAuraDesignTokens() }

val MaterialTheme.aura: AuraDesignTokens
    @Composable
    @ReadOnlyComposable
    get() = LocalAuraDesignTokens.current

fun auraHex(value: Long): Color = Color(value.toInt())

fun auraTokenColor(value: Long): Color = auraHex(value)

fun auraTokenDp(value: Float): Dp = value.dp

fun auraTokenSp(value: Float): TextUnit = value.sp

fun auraTokenAlpha(value: Float): Float = value
