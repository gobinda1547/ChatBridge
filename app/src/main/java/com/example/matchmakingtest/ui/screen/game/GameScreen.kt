package com.example.matchmakingtest.ui.screen.game

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.matchmakingtest.ui.screen.game.views.ChatInputDesign
import com.example.matchmakingtest.ui.screen.game.views.ChatMessagesView
import com.example.matchmakingtest.ui.screen.game.views.TopAppBarDesign

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    navController: NavController,
    viewModel: GameViewModel = hiltViewModel()
) {
    val state = viewModel.state.collectAsStateWithLifecycle()

    Scaffold(topBar = { TopAppBarDesign("Random Chat") }) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .imePadding()
                .fillMaxSize()
        ) {
            GameScreenMainContent(state.value, onUiAction = { handleUiAction(it, viewModel) })
        }
    }
}

private fun handleUiAction(action: GameScreenUiAction, viewModel: GameViewModel) {
    when (action) {
        GameScreenUiAction.TryToConnect -> viewModel.tryToConnect()
        is GameScreenUiAction.SendMessage -> viewModel.sendMessage(action.message)
    }
}

@Composable
fun GameScreenMainContent(state: GameScreenState, onUiAction: (GameScreenUiAction) -> Unit) {
    when (state.connectionState) {
        ConnectionState.Connecting -> GameScreenMainContentConnecting()
        ConnectionState.Connected -> GameScreenMainContentConnected(state, onUiAction)
        ConnectionState.NotConnected -> GameScreenMainContentNotConnected(onUiAction)
    }
}

@Composable
fun GameScreenMainContentNotConnected(onUiAction: (GameScreenUiAction) -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Button(onClick = { onUiAction(GameScreenUiAction.TryToConnect) }) {
            Text(text = "Connect")
        }
    }
}

@Composable
fun GameScreenMainContentConnecting() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = "Connecting")
    }
}

@Composable
fun GameScreenMainContentConnected(
    state: GameScreenState,
    onUiAction: (GameScreenUiAction) -> Unit
) {
    Column {
        ChatMessagesView(modifier = Modifier.weight(1f), state.messages)
        Spacer(modifier = Modifier.size(8.dp))
        ChatInputDesign(bgColor = Color.LightGray, onUiAction)
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewGameScreenMainContentConnected() {
    val sampleState = GameScreenState(
        connectionState = ConnectionState.Connected,
        messages = listOf(
            SingleMessage("Hello", MessageSentOrReceived.Sent),
            SingleMessage("How are you?", MessageSentOrReceived.Received),
            SingleMessage("I'm fine, thank you!", MessageSentOrReceived.Sent)
        )
    )
    GameScreenMainContentConnected(sampleState, onUiAction = {})
}