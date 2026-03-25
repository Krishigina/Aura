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

val SurfaceLight = Color(0xFFFFFFFF)
val SurfaceDark = Color(0xFF121212)
val BackgroundLight = Color(0xFFF8F9FA)
val BackgroundDark = Color(0xFF0A0A0A)

val OnSurfaceLight = Color(0xFF1A1A1A)
val OnSurfaceDark = Color(0xFFF5F5F5)
val OnBackgroundLight = Color(0xFF1A1A1A)
val OnBackgroundDark = Color(0xFFF5F5F5)

private val LightColorScheme = lightColorScheme(
    primary = MintGreen,
    onPrimary = OnMintGreen,
    primaryContainer = Color(0xFFD1FAE5),
    onPrimaryContainer = Color(0xFF065F46),
    
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
    surfaceVariant = Color(0xFFE8E8E8),
    onSurfaceVariant = Color(0xFF424242),
    
    outline = Color(0xFFBDBDBD),
    outlineVariant = Color(0xFFE0E0E0)
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
