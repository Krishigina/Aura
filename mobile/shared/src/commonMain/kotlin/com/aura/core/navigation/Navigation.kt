package com.aura.core.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.aura.core.data.api.AuraApiClient
import com.aura.core.domain.model.GeoLocation
import com.aura.core.data.repository.TokenManager
import com.aura.core.ui.theme.AppState
import com.aura.core.ui.theme.AuraPalette
import com.aura.core.ui.theme.auraThemeColors
import com.aura.feature.chat.ChatDocumentAttachment
import com.aura.feature.auth.AuthScreen
import com.aura.feature.catalog.CatalogScreen
import com.aura.feature.chat.ChatScreen
import com.aura.feature.diagnostics.DiagnosticsScreen
import com.aura.feature.home.HomeScreen
import com.aura.feature.profile.ProfileScreen
import com.aura.feature.product.ProductDetailScreen
import com.aura.feature.splash.AuraSplashScreen
import com.aura.feature.survey.AuraSkinSurveyScreen

object Routes {
    const val AUTH = "auth"
    const val HOME = "home"
    const val CATALOG = "catalog"
    const val DIAGNOSTICS = "diagnostics"
    const val PRODUCT_DETAIL = "product/{productId}"
    const val PROFILE = "profile"
    const val CHAT = "chat"
    const val SKIN_SURVEY = "skin-survey"
    
    fun productDetail(productId: String) = "product/$productId"
}

private data class NavTabInfo(val label: String, val icon: ImageVector, val route: String)

private val bottomTabs = listOf(
    NavTabInfo("\u0413\u043B\u0430\u0432\u043D\u0430\u044F", Icons.Rounded.Home, Routes.HOME),
    NavTabInfo("\u0423\u0445\u043E\u0434", Icons.Rounded.Spa, Routes.CATALOG),
    NavTabInfo("\u0410\u043D\u0430\u043B\u0438\u0437", Icons.Rounded.CenterFocusStrong, Routes.CHAT),
    NavTabInfo("\u041F\u0440\u043E\u0444\u0438\u043B\u044C", Icons.Rounded.Person, Routes.PROFILE)
)

@Composable
fun AuraNavigation(
    apiClient: AuraApiClient,
    pickDocument: (suspend () -> ChatDocumentAttachment?)? = null,
    requestUserLocation: (suspend () -> GeoLocation?)? = null,
) {
    val navController = rememberNavController()
    var startDest by remember { mutableStateOf<String?>(null) }
    var shouldAllowSurveySkip by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        if (TokenManager.isLoggedIn()) {
            TokenManager.getToken()?.let { token ->
                runCatching { apiClient.getMe(token) }
                    .onSuccess { TokenManager.setUser(it) }
                    .onFailure {
                        TokenManager.clearToken()
                        startDest = Routes.AUTH
                    }
            }
            if (startDest == null) {
                startDest = Routes.HOME
            }
        } else {
            startDest = Routes.AUTH
        }
    }
    
    if (startDest == null) {
        AuraSplashScreen()
        return
    }
    
    if (startDest == Routes.AUTH) {
        NavHost(navController = navController, startDestination = Routes.AUTH) {
            composable(Routes.AUTH) {
                AuthScreen(
                    onAuthSuccess = { isNewUser ->
                        shouldAllowSurveySkip = isNewUser
                        startDest = if (isNewUser) Routes.SKIN_SURVEY else Routes.HOME
                    },
                    apiClient = apiClient
                )
            }
        }
    } else {
        MainShell(
            navController = navController,
            apiClient = apiClient,
            pickDocument = pickDocument,
            requestUserLocation = requestUserLocation,
            startRoute = if (startDest == Routes.SKIN_SURVEY) Routes.SKIN_SURVEY else Routes.HOME,
            initialSurveySkippable = shouldAllowSurveySkip
        )
    }
}

@Composable
private fun MainShell(
    navController: NavHostController,
    apiClient: AuraApiClient,
    pickDocument: (suspend () -> ChatDocumentAttachment?)? = null,
    requestUserLocation: (suspend () -> GeoLocation?)? = null,
    startRoute: String = Routes.HOME,
    initialSurveySkippable: Boolean = false
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var isSurveySkippable by rememberSaveable { mutableStateOf(initialSurveySkippable) }
    val dark = isSystemInDarkTheme() || AppState.isDarkMode
    val colors = auraThemeColors(dark)
    val textBody = colors.textBody
    val textMuted = colors.textSecondary
    
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = startRoute
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    apiClient = apiClient,
                    requestUserLocation = requestUserLocation,
                    onNavigateToProduct = { id -> navController.navigate(Routes.PRODUCT_DETAIL.replace("{productId}", id)) }
                )
            }
            composable(Routes.CATALOG) {
                CatalogScreen(
                    apiClient = apiClient,
                    onProductClick = { productId -> navController.navigate(Routes.PRODUCT_DETAIL.replace("{productId}", productId.toString())) }
                )
            }
            composable(Routes.DIAGNOSTICS) {
                DiagnosticsScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.CHAT) {
                ChatScreen(
                    apiClient = apiClient,
                    pickDocument = pickDocument,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Routes.PROFILE) {
                ProfileScreen(
                    onBack = { navController.popBackStack() },
                    onNavigateToHome = {
                        selectedTab = 0
                        navController.navigate(Routes.HOME) { popUpTo(Routes.HOME) { inclusive = true } }
                    },
                    onNavigateToSurvey = {
                        isSurveySkippable = false
                        navController.navigate(Routes.SKIN_SURVEY) { launchSingleTop = true }
                    }
                )
            }
            composable(Routes.SKIN_SURVEY) {
                AuraSkinSurveyScreen(
                    apiClient = apiClient,
                    allowSkip = isSurveySkippable,
                    onSkip = {
                        selectedTab = 0
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.HOME) { inclusive = true }
                        }
                    },
                    onBack = { navController.popBackStack() },
                    onComplete = {
                        selectedTab = 0
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.HOME) { inclusive = true }
                        }
                    }
                )
            }
            composable(Routes.PRODUCT_DETAIL) { backStackEntry ->
                val productId = backStackEntry.arguments?.getString("productId") ?: ""
                ProductDetailScreen(
                    productId = productId,
                    apiClient = apiClient,
                    onBack = { navController.popBackStack() }
                )
            }
        }
        
        // Shared Bottom Navigation
        if (currentRoute != Routes.SKIN_SURVEY) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(0.9f)
                    .padding(bottom = 24.dp)
                    .clip(RoundedCornerShape(50))
                    .background(colors.glassSurface)
                    .border(1.dp, colors.glassBorder, RoundedCornerShape(50))
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                bottomTabs.forEachIndexed { index, tab ->
                    val isActive = selectedTab == index
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable {
                                selectedTab = index
                                navController.navigate(tab.route) {
                                    popUpTo(Routes.HOME) { inclusive = false }
                                    launchSingleTop = true
                                }
                            }
                            .padding(4.dp)
                    ) {
                        if (isActive) {
                                Box(
                                    modifier = Modifier
                                    .background(if (dark) Color.White.copy(alpha = 0.12f) else AuraPalette.BrandMint.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                    .padding(6.dp)
                            ) {
                                Icon(tab.icon, contentDescription = tab.label, tint = textBody, modifier = Modifier.size(24.dp))
                            }
                        } else {
                            Icon(tab.icon, contentDescription = tab.label, tint = textMuted, modifier = Modifier.size(24.dp))
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = tab.label,
                            fontSize = 10.sp,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                            color = if (isActive) textBody else textMuted
                        )
                    }
                }
            }
        }
    }
}
