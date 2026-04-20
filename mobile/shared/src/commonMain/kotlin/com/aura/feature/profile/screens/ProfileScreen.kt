package com.aura.feature.profile.screens

import androidx.compose.runtime.Composable

@Composable
fun ProfileScreen(
    onLogout: () -> Unit = {},
    onNavigateToAllParameters: () -> Unit = {},
    onNavigateToSurvey: () -> Unit = {},
    onNavigateToJournal: () -> Unit = {},
    onNavigateToRoutine: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onOpenFavoriteRecommendation: (String) -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
) {
    ProfileOverviewScreen(
        onLogout = onLogout,
        onNavigateToAllParameters = onNavigateToAllParameters,
        onNavigateToSurvey = onNavigateToSurvey,
        onNavigateToJournal = onNavigateToJournal,
        onNavigateToRoutine = onNavigateToRoutine,
        onNavigateToNotifications = onNavigateToNotifications,
        onOpenFavoriteRecommendation = onOpenFavoriteRecommendation,
        onNavigateToSettings = onNavigateToSettings,
    )
}
