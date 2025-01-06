package com.example.matchmakingtest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.matchmakingtest.ui.navigation.AppNavigationHost
import com.example.matchmakingtest.ui.theme.MatchMakingTestTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MatchMakingTestTheme {
                AppNavigationHost()
            }
        }
    }
}