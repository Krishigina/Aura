package com.aura.core.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.aura.feature.catalog.CatalogFilterScreen
import com.aura.feature.catalog.CatalogProductFilters
import com.aura.feature.catalog.presentation.CatalogRoute
import com.aura.feature.chat.ChatScreen
import com.aura.feature.chat.ChatSessionsScreen
import com.aura.feature.chat.presentation.ChatNavigationState
import com.aura.feature.diagnostics.DiagnosticsScreen
import com.aura.feature.home.HomeScreen
import com.aura.feature.journal.SkinJournalScreen
import com.aura.feature.product.ProductDetailRoute
import com.aura.feature.profile.screens.ProfileAllParametersScreen
import com.aura.feature.profile.screens.ProfileNotificationsScreen
import com.aura.feature.profile.screens.ProfileRoutineScreen
import com.aura.feature.profile.screens.ProfileScreen
import com.aura.feature.profile.screens.ProfileSettingsScreen
import com.aura.feature.recommendations.RecommendationsRoute
import com.aura.feature.recommendations.SavedRecommendationScreen
import com.aura.feature.survey.AuraSkinSurveyScreen

fun NavGraphBuilder.auraMainGraph(
    navController: NavHostController,
    catalogFilters: CatalogProductFilters,
    onCatalogFiltersChange: (CatalogProductFilters) -> Unit,
    isSurveySkippable: Boolean,
    onSurveySkippableChange: (Boolean) -> Unit,
    chatNavigationState: ChatNavigationState,
    onLogout: () -> Unit,
) {
    composable(Routes.HOME) {
        HomeScreen(
            onNavigateToJournal = { navController.navigate(Routes.SKIN_JOURNAL) { launchSingleTop = true } },
            onNavigateToRecommendations = { navController.navigate(Routes.RECOMMENDATIONS) { launchSingleTop = true } },
            onNavigateToProfileSettings = { navController.navigate(Routes.PROFILE_SETTINGS) { launchSingleTop = true } },
        )
    }
    composable(Routes.CATALOG) {
        CatalogRoute(
            filters = catalogFilters,
            onFilterClick = { navController.navigate(Routes.CATALOG_FILTERS) { launchSingleTop = true } },
            onProductClick = { productId -> navController.navigate(Routes.productDetail(productId.toString())) },
        )
    }
    composable(Routes.CATALOG_FILTERS) {
        CatalogFilterScreen(
            initialFilters = catalogFilters,
            onBack = { navController.popBackStack() },
            onApply = { filters ->
                onCatalogFiltersChange(filters)
                navController.popBackStack()
            },
        )
    }
    composable(Routes.DIAGNOSTICS) {
        DiagnosticsScreen(onBack = { navController.popBackStack() })
    }
    composable(Routes.CHAT) {
        ChatScreen(
            onNavigateToSessions = { navController.navigate(Routes.CHAT_SESSIONS) { launchSingleTop = true } },
            onBack = { navController.popBackStack() },
        )
    }
    composable(Routes.CHAT_SESSIONS) {
        ChatSessionsScreen(
            onBack = { navController.popBackStack() },
            onSessionClick = { sessionId -> navController.navigate(Routes.chatSession(sessionId)) },
            onNewChat = {
                chatNavigationState.clearActiveSession()
                navController.navigate(Routes.CHAT) {
                    popUpTo(Routes.CHAT) { inclusive = true }
                    launchSingleTop = true
                }
            },
        )
    }
    composable(
        route = Routes.CHAT_SESSION,
        arguments = listOf(navArgument("sessionId") { type = NavType.IntType }),
    ) { backStackEntry ->
        ChatScreen(
            initialSessionId = backStackEntry.arguments?.getInt("sessionId"),
            onNavigateToSessions = { navController.navigate(Routes.CHAT_SESSIONS) { launchSingleTop = true } },
            onBack = { navController.popBackStack() },
        )
    }
    composable(Routes.RECOMMENDATIONS) {
        RecommendationsRoute(
            onBack = { navController.popBackStack() },
            onProductOpen = { productId -> navController.navigate(Routes.productDetail(productId.toString())) },
            onOpenSurvey = {
                onSurveySkippableChange(false)
                navController.navigate(Routes.SKIN_SURVEY) { launchSingleTop = true }
            },
        )
    }
    composable(Routes.PROFILE) {
        ProfileScreen(
            onLogout = onLogout,
            onNavigateToAllParameters = { navController.navigate(Routes.PROFILE_PARAMETERS) { launchSingleTop = true } },
            onNavigateToSurvey = {
                onSurveySkippableChange(false)
                navController.navigate(Routes.SKIN_SURVEY) { launchSingleTop = true }
            },
            onNavigateToJournal = { navController.navigate(Routes.SKIN_JOURNAL) { launchSingleTop = true } },
            onNavigateToRoutine = { navController.navigate(Routes.PROFILE_ROUTINE) { launchSingleTop = true } },
            onNavigateToNotifications = { navController.navigate(Routes.PROFILE_NOTIFICATIONS) { launchSingleTop = true } },
            onOpenFavoriteRecommendation = { favoriteId -> navController.navigate(Routes.profileRecommendationFavorite(favoriteId)) { launchSingleTop = true } },
            onNavigateToSettings = { navController.navigate(Routes.PROFILE_SETTINGS) { launchSingleTop = true } },
        )
    }
    composable(Routes.PROFILE_PARAMETERS) {
        ProfileAllParametersScreen(onBack = { navController.popBackStack() })
    }
    composable(Routes.SKIN_JOURNAL) {
        SkinJournalScreen(onBack = { navController.popBackStack() })
    }
    composable(Routes.PROFILE_SETTINGS) {
        ProfileSettingsScreen(
            onBack = { navController.popBackStack() },
            onAccountDeleted = onLogout,
        )
    }
    composable(Routes.PROFILE_ROUTINE) {
        ProfileRoutineScreen(onBack = { navController.popBackStack() })
    }
    composable(Routes.PROFILE_NOTIFICATIONS) {
        ProfileNotificationsScreen(onBack = { navController.popBackStack() })
    }
    composable(
        route = Routes.PROFILE_RECOMMENDATION_FAVORITE,
        arguments = listOf(navArgument("favoriteId") { type = NavType.StringType }),
    ) { backStackEntry ->
        SavedRecommendationScreen(
            favoriteId = backStackEntry.arguments?.getString("favoriteId").orEmpty(),
            onBack = { navController.popBackStack() },
            onProductOpen = { productId -> navController.navigate(Routes.productDetail(productId.toString())) },
        )
    }
    composable(Routes.SKIN_SURVEY) {
        AuraSkinSurveyScreen(
            allowSkip = isSurveySkippable,
            onSkip = {
                navController.navigate(Routes.HOME) {
                    popUpTo(Routes.SKIN_SURVEY) { inclusive = true }
                }
            },
            onBack = { navController.popBackStack() },
            onComplete = {
                navController.navigate(Routes.PROFILE) {
                    popUpTo(Routes.SKIN_SURVEY) { inclusive = true }
                    launchSingleTop = true
                }
            },
            onSensorConnected = {
                navController.navigate(Routes.SKIN_JOURNAL) {
                    popUpTo(Routes.SKIN_SURVEY) { inclusive = true }
                    launchSingleTop = true
                }
            },
        )
    }
    composable(Routes.PRODUCT_DETAIL) { backStackEntry ->
        ProductDetailRoute(
            productId = backStackEntry.arguments?.getString("productId") ?: "",
            onAskAssistant = { navController.navigate(Routes.CHAT) { launchSingleTop = true } },
            onBack = { navController.popBackStack() },
        )
    }
}
