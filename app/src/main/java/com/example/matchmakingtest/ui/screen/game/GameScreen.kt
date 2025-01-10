package com.example.matchmakingtest.ui.screen.game

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController

@Composable
fun GameScreen(
    navController: NavController,
    viewModel: GameViewModel = hiltViewModel()
) {
    val state = viewModel.state.collectAsStateWithLifecycle()
    GameScreenMainContent(state.value, viewModel)
}

@Composable
fun GameScreenMainContent(state: GameScreenState, viewModel: GameViewModel) {
    when (state.connectionState) {
        ConnectionState.Connecting -> GameScreenMainContentNotConnecting(state)
        ConnectionState.Connected -> GameScreenMainContentConnected(state, viewModel)
        ConnectionState.NotConnected -> GameScreenMainContentNotConnected(state, viewModel)
    }
}

@Composable
fun GameScreenMainContentNotConnected(state: GameScreenState, viewModel: GameViewModel) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Button(onClick = { viewModel.tryToConnect() }) {
            Text(text = "Connect")
        }
    }
}

@Composable
fun GameScreenMainContentNotConnecting(state: GameScreenState) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = "Connecting")
    }
}

@Composable
fun GameScreenMainContentConnected(state: GameScreenState, viewModel: GameViewModel) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column {
            Text(text = state.message)
            Button(onClick = { viewModel.sendMessage("1") }) {
                Text(text = "send message")
            }
        }
    }
}