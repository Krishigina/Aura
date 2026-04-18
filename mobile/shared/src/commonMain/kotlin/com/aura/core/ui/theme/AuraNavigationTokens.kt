package com.aura.core.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class AuraNavigationTokens(
    val selectedMenuColor: Color,
    val textBodyDark: Color,
    val textBodyLight: Color,
    val textMutedDark: Color,
    val textMutedLight: Color,
    val bottomBarHorizontalPadding: Dp,
    val bottomBarBottomPadding: Dp,
    val bottomBarContentHorizontalPadding: Dp,
    val bottomBarContentVerticalPadding: Dp,
    val tabPadding: Dp,
    val activeIndicatorDarkAlpha: Float,
    val activeIndicatorLightAlpha: Float,
    val activeIndicatorRadius: Dp,
    val activeIndicatorPadding: Dp,
    val tabIconSize: Dp,
    val tabLabelTopGap: Dp,
    val tabLabelFontSize: TextUnit,
)

fun defaultAuraNavigationTokens() = AuraNavigationTokens(
    selectedMenuColor = Color(0xFFFBCFE8),
    textBodyDark = Color(0xFFCBD5E1),
    textBodyLight = Color(0xFF334155),
    textMutedDark = Color(0xFF94A3B8),
    textMutedLight = Color(0xFF64748B),
    bottomBarHorizontalPadding = 16.dp,
    bottomBarBottomPadding = 24.dp,
    bottomBarContentHorizontalPadding = 8.dp,
    bottomBarContentVerticalPadding = 8.dp,
    tabPadding = 4.dp,
    activeIndicatorDarkAlpha = 0.18f,
    activeIndicatorLightAlpha = 0.45f,
    activeIndicatorRadius = 12.dp,
    activeIndicatorPadding = 6.dp,
    tabIconSize = 24.dp,
    tabLabelTopGap = 2.dp,
    tabLabelFontSize = 10.sp,
)
