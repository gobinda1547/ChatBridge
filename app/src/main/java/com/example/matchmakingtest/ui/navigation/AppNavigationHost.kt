package com.example.matchmakingtest.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.matchmakingtest.ui.screen.join.JoinScreen

@Composable
fun AppNavigationHost() {

    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = ComposeScreen.JoinComposeScreen.route
    ) {
        composable(
            route = ComposeScreen.JoinComposeScreen.route,
            arguments = ComposeScreen.JoinComposeScreen.navArguments,
        ) {
            JoinScreen(navController = navController)
        }

    }
}