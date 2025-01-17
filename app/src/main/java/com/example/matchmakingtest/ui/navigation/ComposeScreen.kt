package com.example.matchmakingtest.ui.navigation

import androidx.navigation.NamedNavArgument

sealed class ComposeScreen(
    val route: String,
    val navArguments: List<NamedNavArgument> = emptyList()
) {

    data object JoinComposeScreen : ComposeScreen("loadingScreen")

    data object GameComposeScreen : ComposeScreen("onboardingScreen")

}