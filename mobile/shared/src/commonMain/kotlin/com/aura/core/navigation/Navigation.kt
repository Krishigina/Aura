package com.aura.core.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.aura.core.data.repository.journal.SkinJournalStore
import com.aura.core.ui.theme.aura
import com.aura.core.ui.theme.runtime.AppState
import com.aura.feature.auth.AuthScreen
import com.aura.feature.auth.presentation.AuthSessionViewModel
import com.aura.feature.catalog.CatalogProductFilters
import com.aura.feature.chat.presentation.ChatNavigationState
import org.koin.compose.koinInject

object Routes {
    const val AUTH = "auth"
    const val HOME = "home"
    const val RECOMMENDATIONS = "recommendations"
    const val CATALOG = "catalog"
    const val CATALOG_FILTERS = "catalog-filters"
    const val DIAGNOSTICS = "diagnostics"
    const val PRODUCT_DETAIL = "product/{productId}"
    const val PROFILE = "profile"
    const val PROFILE_PARAMETERS = "profile-parameters"
    const val PROFILE_SETTINGS = "profile-settings"
    const val PROFILE_ROUTINE = "profile-routine"
    const val PROFILE_NOTIFICATIONS = "profile-notifications"
    const val PROFILE_RECOMMENDATION_FAVORITE = "profile-recommendation/{favoriteId}"
    const val CHAT = "chat"
    const val CHAT_SESSIONS = "chat_sessions"
    const val CHAT_SESSION = "chat/{sessionId}"
    const val SKIN_SURVEY = "skin-survey"
    const val SKIN_JOURNAL = "skin-journal"

    fun productDetail(productId: String) = "product/$productId"
    fun chatSession(sessionId: Int) = "chat/$sessionId"
    fun profileRecommendationFavorite(favoriteId: String) = "profile-recommendation/$favoriteId"
}

@Composable
fun AuraNavigation(sessionViewModel: AuthSessionViewModel = koinInject()) {
    val navController = rememberNavController()
    val sessionState by sessionViewModel.uiState.collectAsState()
    var startDest by remember { mutableStateOf<String?>(null) }
    var shouldAllowSurveySkip by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        sessionViewModel.load()
    }

    LaunchedEffect(sessionState.isReady, sessionState.isAuthenticated) {
        if (!sessionState.isReady) return@LaunchedEffect
        startDest = if (sessionState.isAuthenticated) Routes.HOME else Routes.AUTH
        AppState.isNavigationReady = true
    }

    if (startDest == null) {
        return
    }

    LaunchedEffect(startDest) {
        if (startDest != Routes.HOME) return@LaunchedEffect
        sessionViewModel.validate()
    }

    if (startDest == Routes.AUTH) {
        NavHost(navController = navController, startDestination = Routes.AUTH) {
            composable(Routes.AUTH) {
                AuthScreen(
                    onAuthSuccess = { isNewUser ->
                        sessionViewModel.markAuthenticated()
                        shouldAllowSurveySkip = isNewUser
                        startDest = if (isNewUser) Routes.SKIN_SURVEY else Routes.HOME
                    },
                )
            }
        }
    } else {
        MainShell(
            navController = navController,
            startRoute = if (startDest == Routes.SKIN_SURVEY) Routes.SKIN_SURVEY else Routes.HOME,
            initialSurveySkippable = shouldAllowSurveySkip,
            onLogout = {
                sessionViewModel.clear()
                SkinJournalStore.clear()
                startDest = Routes.AUTH
            },
        )
    }
}

@Composable
private fun MainShell(
    navController: NavHostController,
    startRoute: String = Routes.HOME,
    initialSurveySkippable: Boolean = false,
    onLogout: () -> Unit,
) {
    var isSurveySkippable by remember { mutableStateOf(initialSurveySkippable) }
    var catalogFilters by remember { mutableStateOf(CatalogProductFilters()) }
    val chatNavigationState: ChatNavigationState = koinInject()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val density = LocalDensity.current
    val keyboardVisible = WindowInsets.ime.getBottom(density) > 0
    val hideBottomNavForIme = keyboardVisible && (currentRoute == Routes.CHAT || currentRoute == Routes.CHAT_SESSION)
    val dark = isSystemInDarkTheme() || AppState.isDarkMode
    val navTokens = MaterialTheme.aura.navigation
    val textBody = if (dark) navTokens.textBodyDark else navTokens.textBodyLight
    val textMuted = if (dark) navTokens.textMutedDark else navTokens.textMutedLight

    val selectedTab = when (currentRoute) {
        Routes.HOME, Routes.RECOMMENDATIONS -> 0
        Routes.CATALOG, Routes.CATALOG_FILTERS, Routes.PRODUCT_DETAIL -> 1
        Routes.CHAT, Routes.CHAT_SESSIONS, Routes.CHAT_SESSION -> 2
        Routes.PROFILE, Routes.PROFILE_PARAMETERS, Routes.PROFILE_SETTINGS, Routes.PROFILE_ROUTINE, Routes.PROFILE_NOTIFICATIONS, Routes.PROFILE_RECOMMENDATION_FAVORITE, Routes.SKIN_JOURNAL -> 3
        else -> 0
    }

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = startRoute,
        ) {
            auraMainGraph(
                navController = navController,
                catalogFilters = catalogFilters,
                onCatalogFiltersChange = { catalogFilters = it },
                isSurveySkippable = isSurveySkippable,
                onSurveySkippableChange = { isSurveySkippable = it },
                chatNavigationState = chatNavigationState,
                onLogout = onLogout,
            )
        }

        val shouldShowBottomNav = !hideBottomNavForIme &&
            currentRoute != Routes.SKIN_SURVEY &&
            currentRoute != Routes.PROFILE_PARAMETERS &&
            currentRoute != Routes.PROFILE_SETTINGS &&
            currentRoute != Routes.PROFILE_ROUTINE &&
            currentRoute != Routes.PROFILE_NOTIFICATIONS &&
            currentRoute != Routes.PROFILE_RECOMMENDATION_FAVORITE &&
            currentRoute != Routes.CHAT_SESSIONS &&
            currentRoute != Routes.CATALOG_FILTERS &&
            currentRoute != Routes.SKIN_JOURNAL

        AnimatedVisibility(
            visible = shouldShowBottomNav,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = fadeIn(animationSpec = tween(140)) + slideInVertically(animationSpec = tween(180)) { it / 2 },
            exit = fadeOut(animationSpec = tween(120)) + slideOutVertically(animationSpec = tween(140)) { it / 2 },
        ) {
            AuraBottomNavigationBar(
                selectedTab = selectedTab,
                dark = dark,
                textBody = textBody,
                textMuted = textMuted,
                onNavigate = { route ->
                    navController.navigate(route) {
                        popUpTo(Routes.HOME) { inclusive = false }
                        launchSingleTop = true
                    }
                },
            )
        }
    }
}
