package com.example.matchmakingtest.ui.screen.game.views

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun getTopAppBarColorDefault() = TopAppBarDefaults.topAppBarColors(
    containerColor = MaterialTheme.colorScheme.background,
    titleContentColor = MaterialTheme.colorScheme.onBackground,
    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
    actionIconContentColor = MaterialTheme.colorScheme.primary
)


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopAppBarDesign(text: String) {
    TopAppBar(
        colors = getTopAppBarColorDefault(),
        title = { Text(text = text) }
    )
}