package com.aura.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.aura.feature.auth.AuthScreen
import com.aura.feature.diagnostics.DiagnosticsScreen
import com.aura.feature.home.HomeScreen
import com.aura.feature.product.ProductDetailScreen
import com.aura.feature.profile.ProfileScreen
import com.aura.feature.chat.ChatScreen

object Routes {
    const val AUTH = "auth"
    const val HOME = "home"
    const val DIAGNOSTICS = "diagnostics"
    const val PRODUCT_DETAIL = "product/{productId}"
    const val PROFILE = "profile"
    const val CHAT = "chat"
    
    fun productDetail(productId: String) = "product/$productId"
}

@Composable
fun AuraNavigation() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.AUTH) {
        composable(Routes.AUTH) {
            AuthScreen(
                onAuthSuccess = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.AUTH) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.HOME) {
            HomeScreen(
                onNavigateToProfile = { navController.navigate(Routes.PROFILE) },
                onNavigateToChat = { navController.navigate(Routes.CHAT) },
                onNavigateToDiagnostics = { navController.navigate(Routes.DIAGNOSTICS) }
            )
        }
        composable(Routes.DIAGNOSTICS) {
            DiagnosticsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.PROFILE) {
            ProfileScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.CHAT) {
            ChatScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.PRODUCT_DETAIL) { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId") ?: ""
            ProductDetailScreen(
                productId = productId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
