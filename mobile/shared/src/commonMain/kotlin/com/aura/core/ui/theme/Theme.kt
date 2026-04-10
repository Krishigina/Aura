package com.aura.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

val MintGreen = Color(0xFFA7F3D0)
val MintGreenDark = Color(0xFF017D1A)
val OnMintGreen = Color(0xFF1A3D2A)
val PinkAccent = Color(0xFFFB6FE8)
val Lavender = Color(0xFFE0C3FC)
val Primary = Color(0xFF135BEC)
val PrimaryContainer = Color(0xFF4F86F7)

val SurfaceLight = Color(0xFFFFFFFF)
val SurfaceDark = Color(0xFF121212)
val BackgroundLight = Color(0xFFF5F7FA)
val BackgroundDark = Color(0xFF0A0A0A)

val OnSurfaceLight = Color(0xFF1E293B)
val OnSurfaceDark = Color(0xFFF5F5F5)
val OnBackgroundLight = Color(0xFF1E293B)
val OnBackgroundDark = Color(0xFFF5F5F5)

// Card surfaces for light theme
val CardLight = Color(0xFFFFFFFF)
val CardBorderLight = Color(0xFFE2E8F0)
val CardShadowLight = Color(0xFF0F172A).copy(alpha = 0.06f)
val TextSecondaryLight = Color(0xFF64748B)
val TextMutedLight = Color(0xFF94A3B8)

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = Color.White,
    
    secondary = PinkAccent,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFCE7F3),
    onSecondaryContainer = Color(0xFF831843),
    
    tertiary = Lavender,
    onTertiary = Color(0xFF3E1D5C),
    tertiaryContainer = Color(0xFFF3E8FF),
    onTertiaryContainer = Color(0xFF3E1D5C),
    
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF475569),
    
    outline = Color(0xFFE2E8F0),
    outlineVariant = Color(0xFFF1F5F9),
    
    error = Color(0xFFEF4444),
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = MintGreen,
    onPrimary = Color(0xFF003D1A),
    primaryContainer = Color(0xFF005230),
    onPrimaryContainer = MintGreen,
    
    secondary = PinkAccent,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF9D174D),
    onSecondaryContainer = PinkAccent,
    
    tertiary = Lavender,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF4C1D95),
    onTertiaryContainer = Lavender,
    
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = Color(0xFF2A2A2A),
    onSurfaceVariant = Color(0xFFCACACA),
    
    outline = Color(0xFF424242),
    outlineVariant = Color(0xFF2A2A2A)
)

val LocalDarkTheme = compositionLocalOf { false }

@Composable
fun AuraTheme(
    darkTheme: Boolean = AppState.isDarkMode,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AuraTypography,
        content = content
    )
}
