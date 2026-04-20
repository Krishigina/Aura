package com.aura.feature.profile.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.aura.core.i18n.StringsRu
import com.aura.core.ui.components.SoftPastelBackground
import com.aura.core.ui.components.SoftPastelVariant
import com.aura.core.ui.components.auraToolbarTopOffset
import com.aura.core.ui.theme.aura
import com.aura.core.ui.theme.auraHex
import com.aura.core.ui.theme.auraTokenAlpha
import com.aura.core.ui.theme.auraTokenDp
import com.aura.core.ui.theme.runtime.AppState
import com.aura.feature.profile.presentation.components.overview.FavoriteRecommendationsSection
import com.aura.feature.profile.presentation.components.overview.MenuListSection
import com.aura.feature.profile.presentation.components.overview.ProfileSection
import com.aura.feature.profile.presentation.components.overview.SkinPassportSection
import com.aura.feature.profile.presentation.favorites.ProfileFavoritesViewModel
import com.aura.feature.profile.presentation.parameters.ProfileParametersViewModel
import com.aura.feature.profile.presentation.settings.ProfileSettingsViewModel
import org.koin.compose.koinInject

@Composable
fun ProfileOverviewScreen(
    onLogout: () -> Unit,
    onNavigateToAllParameters: () -> Unit,
    onNavigateToSurvey: () -> Unit,
    onNavigateToJournal: () -> Unit,
    onNavigateToRoutine: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onOpenFavoriteRecommendation: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    parametersViewModel: ProfileParametersViewModel = koinInject(),
    favoritesViewModel: ProfileFavoritesViewModel = koinInject(),
    settingsViewModel: ProfileSettingsViewModel = koinInject(),
) {
    val favoritesUiState by favoritesViewModel.uiState.collectAsState()
    val settingsUiState by settingsViewModel.uiState.collectAsState()
    val parametersUiState by parametersViewModel.uiState.collectAsState()

    val dark = isSystemInDarkTheme() || AppState.isDarkMode
    val bg = if (dark) auraHex(0xFF0A0A0A) else MaterialTheme.aura.profile.iceBlue
    val glassAlpha = if (dark) 0.08f else 0.45f
    val textPrimary = if (dark) auraHex(0xFFF1F5F9) else MaterialTheme.aura.profile.slate800
    val textBody = if (dark) auraHex(0xFFCBD5E1) else MaterialTheme.aura.profile.slate700
    val textSecondary = if (dark) auraHex(0xFF94A3B8) else MaterialTheme.aura.profile.slate500
    val textMuted = if (dark) auraHex(0xFF64748B) else MaterialTheme.aura.profile.slate400
    val cardBorder = if (dark) Color.White.copy(alpha = auraTokenAlpha(0.1f)) else Color.White.copy(alpha = auraTokenAlpha(0.6f))
    val iconBoxBg = if (dark) Color.White.copy(alpha = auraTokenAlpha(0.1f)) else Color.White

    fun loadFavoriteRecommendations() {
        favoritesViewModel.load()
    }

    LaunchedEffect(Unit) {
        parametersViewModel.refresh()
        loadFavoriteRecommendations()
    }

    Box(modifier = Modifier.fillMaxSize().background(bg)) {
        SoftPastelBackground(dark = dark, variant = SoftPastelVariant.Default)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = auraToolbarTopOffset(auraTokenDp(72f)), bottom = auraTokenDp(120f)),
        ) {
            ProfileSection(
                userName = settingsUiState.user?.name?.takeIf { it.isNotBlank() } ?: StringsRu.Common.userFallback,
                textPrimary = textPrimary,
            )
            Spacer(modifier = Modifier.height(auraTokenDp(32f)))
            SkinPassportSection(
                skinPassportAnswers = parametersUiState.skinPassportAnswers,
                textBody = textBody,
                textSecondary = textSecondary,
                glassAlpha = glassAlpha,
                cardBorder = cardBorder,
                dark = dark,
                onNavigateToAllParameters = onNavigateToAllParameters,
                onNavigateToSurvey = onNavigateToSurvey,
            )
            Spacer(modifier = Modifier.height(auraTokenDp(24f)))
            FavoriteRecommendationsSection(
                favorites = favoritesUiState.favorites,
                loading = favoritesUiState.loading,
                error = favoritesUiState.error,
                textBody = textBody,
                textSecondary = textSecondary,
                glassAlpha = glassAlpha,
                cardBorder = cardBorder,
                dark = dark,
                onOpenFavoriteRecommendation = onOpenFavoriteRecommendation,
            )
            Spacer(modifier = Modifier.height(auraTokenDp(24f)))
            MenuListSection(
                textBody = textBody,
                textMuted = textMuted,
                glassAlpha = glassAlpha,
                cardBorder = cardBorder,
                iconBoxBg = iconBoxBg,
                onLogout = onLogout,
                onNavigateToJournal = onNavigateToJournal,
                onNavigateToRoutine = onNavigateToRoutine,
                onNavigateToNotifications = onNavigateToNotifications,
                onNavigateToSettings = onNavigateToSettings,
            )
        }
    }
}
