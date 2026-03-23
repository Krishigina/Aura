package com.aura.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.aura.feature.auth.AuthScreen
import com.aura.feature.home.HomeScreen

object Screen { object Auth { const val route = "auth" } object Home { const val route = "home" } }

@Composable
fun AuraNavigation() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Screen.Auth.route) {
        composable(Screen.Auth.route) {
            AuthScreen(onAuthSuccess = { navController.navigate(Screen.Home.route) })
        }
        composable(Screen.Home.route) {
            HomeScreen()
        }
    }
}