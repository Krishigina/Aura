package com.aura.core.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.test.Test
import kotlin.test.assertEquals

class AuraDesignTokensTest {
    @Test
    fun auraHexProducesArgbColorsSafeForAlphaCopy() {
        val color = auraHex(0xFFDBEAFE)

        assertEquals(Color(0xFFDBEAFE), color)
        assertEquals(Color(0x80DBEAFE), color.copy(alpha = 0.5f))
    }

    @Test
    fun chatHeaderAndDockShareGlassTransparency() {
        val tokens = defaultAuraDesignTokens()

        assertEquals(tokens.chat.dockSurfaceAlpha, tokens.chat.headerSurfaceAlpha)
        assertEquals(tokens.chat.dockBorderAlpha, tokens.chat.headerBorderAlpha)
        assertEquals(0.95f, tokens.chat.headerSurfaceAlpha)
        assertEquals(0.75f, tokens.chat.headerBorderAlpha)
        assertEquals(Color.White, tokens.chat.glassSurfaceColor)
        assertEquals(Color.White, tokens.chat.glassBorderColor)
    }

    @Test
    fun chatHeaderUsesAppLogoAndGlowTokens() {
        val tokens = defaultAuraDesignTokens()

        assertEquals(42.dp, tokens.chat.headerLogoSize)
        assertEquals(36.dp, tokens.chat.headerOptionsSize)
        assertEquals(28.dp, tokens.chat.headerGlowElevation)
        assertEquals(
            listOf(Color(0xFFF8AFC4), Color(0xFFD6A4DF), Color(0xFFA8C8FF)),
            tokens.brand.logoGradient,
        )
    }

    @Test
    fun chatMessageAndInputTokensPreserveCurrentVisualScale() {
        val chat = defaultAuraDesignTokens().chat

        assertEquals(0.85f, chat.messageMaxWidthFraction)
        assertEquals(16.dp, chat.aiBubbleCornerRadius)
        assertEquals(2.dp, chat.messageTailRadius)
        assertEquals(15.sp, chat.markdownTextFontSize)
        assertEquals(22.sp, chat.markdownLineHeight)
        assertEquals(32.dp, chat.inputRadius)
        assertEquals(40.dp, chat.sendButtonSize)
    }

    @Test
    fun chatSessionsTokensPreserveCurrentVisualScale() {
        val chat = defaultAuraDesignTokens().chat

        assertEquals(16.dp, chat.sessionsListHorizontalPadding)
        assertEquals(104.dp, chat.sessionsListBottomPadding)
        assertEquals(24.dp, chat.sessionsNewButtonRadius)
        assertEquals(26.dp, chat.sessionRowRadius)
        assertEquals(0.88f, chat.sessionRowSurfaceAlpha)
        assertEquals(999.dp, chat.sessionCountPillRadius)
    }

    @Test
    fun authTokensPreserveCurrentVisualScale() {
        val auth = defaultAuraDesignTokens().auth

        assertEquals(Color(0xFFF4F7FE), auth.backgroundColor)
        assertEquals(32.dp, auth.cardRadius)
        assertEquals(56.dp, auth.submitHeight)
        assertEquals(16.dp, auth.submitRadius)
        assertEquals(48.dp, auth.fieldHeight)
        assertEquals(14.sp, auth.fieldTextFontSize)
    }

    @Test
    fun diagnosticsTokensPreserveCurrentVisualScale() {
        val diagnostics = defaultAuraDesignTokens().diagnostics

        assertEquals(16.dp, diagnostics.screenPadding)
        assertEquals(180.dp, diagnostics.scanCanvasSize)
        assertEquals(100.dp, diagnostics.scanIconSurfaceSize)
        assertEquals(16.dp, diagnostics.metricCardRadius)
        assertEquals(Color(0xFFFFAB40), diagnostics.sensitivityColor)
        assertEquals(48.dp, diagnostics.iotIconSurfaceSize)
    }

    @Test
    fun navigationTokensPreserveCurrentVisualScale() {
        val navigation = defaultAuraDesignTokens().navigation

        assertEquals(Color(0xFFFBCFE8), navigation.selectedMenuColor)
        assertEquals(16.dp, navigation.bottomBarHorizontalPadding)
        assertEquals(24.dp, navigation.bottomBarBottomPadding)
        assertEquals(0.18f, navigation.activeIndicatorDarkAlpha)
        assertEquals(0.45f, navigation.activeIndicatorLightAlpha)
        assertEquals(24.dp, navigation.tabIconSize)
        assertEquals(10.sp, navigation.tabLabelFontSize)
    }

    @Test
    fun profileSettingsTokensPreserveCurrentVisualScale() {
        val settings = defaultAuraDesignTokens().profileSettings

        assertEquals(Color(0xFFF4F7FE), settings.backgroundColor)
        assertEquals(24.dp, settings.contentHorizontalPadding)
        assertEquals(14.dp, settings.contentGap)
        assertEquals(50.dp, settings.buttonHeight)
        assertEquals(16.dp, settings.entryCardRadius)
        assertEquals(52.dp, settings.fieldHeight)
        assertEquals(Color(0xFF0EA5E9), settings.saveButtonContainerColor)
    }

    @Test
    fun recommendationsTokensPreserveCurrentVisualScale() {
        val recommendations = defaultAuraDesignTokens().recommendations

        assertEquals(Color(0xFFF4F7FE), recommendations.backgroundLight)
        assertEquals(Color(0xFFC4B5FD), recommendations.violet)
        assertEquals(20.dp, recommendations.listHorizontalPadding)
        assertEquals(112.dp, recommendations.listBottomPadding)
        assertEquals(54.dp, recommendations.saveButtonHeight)
        assertEquals(190.dp, recommendations.lineCardWidth)
        assertEquals(0.20f, recommendations.lineSelectedAlpha)
    }

    @Test
    fun catalogTokensPreserveCurrentVisualScale() {
        val catalog = defaultAuraDesignTokens().catalog

        assertEquals(Color(0xFFF4F7FE), catalog.backgroundLight)
        assertEquals(20.dp, catalog.screenHorizontalPadding)
        assertEquals(88.dp, catalog.catalogTopOffset)
        assertEquals(16.dp, catalog.cardRadius)
        assertEquals(120.dp, catalog.cardImageHeight)
        assertEquals(Color(0xFFEF4444), catalog.errorColor)
        assertEquals(0.55f, catalog.videoBadgeAlpha)
    }

    @Test
    fun homeTokensPreserveCurrentVisualScale() {
        val home = defaultAuraDesignTokens().home

        assertEquals(Color(0xFFF9A8D4), home.pink)
        assertEquals(24.dp, home.screenHorizontalPadding)
        assertEquals(88.dp, home.toolbarTopOffset)
        assertEquals(32.dp, home.sectionGapLarge)
        assertEquals(80.dp, home.weatherCardWidth)
        assertEquals(160.dp, home.ritualGlowSize)
        assertEquals(200.dp, home.insightCardWidth)
    }

    @Test
    fun productTokensPreserveCurrentVisualScale() {
        val product = defaultAuraDesignTokens().product

        assertEquals(Color(0xFFF472B6), product.pinkSoft)
        assertEquals(Color(0xFFFCFDFF), product.accordionBg)
        assertEquals(20.dp, product.d20)
        assertEquals(188.dp, product.d188)
        assertEquals(286.dp, product.d286)
        assertEquals(0.9f, product.alpha90)
    }
}
