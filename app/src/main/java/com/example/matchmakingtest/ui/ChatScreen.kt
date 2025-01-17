package com.example.matchmakingtest.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.isImeVisible
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.matchmakingtest.ui.models.ConnectionState
import com.example.matchmakingtest.ui.models.GameScreenState
import com.example.matchmakingtest.ui.models.GameScreenUiAction
import com.example.matchmakingtest.ui.views.ChatInputDesign
import com.example.matchmakingtest.ui.views.ChatMessagesView
import com.example.matchmakingtest.ui.views.TopAppBarDesign

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ChatScreen(viewModel: ChatViewModel = hiltViewModel()) {
    val state = viewModel.state.collectAsStateWithLifecycle()
    val imeHeight = with(LocalDensity.current) { WindowInsets.ime.getBottom(this).toDp() }

    Scaffold(topBar = { TopAppBarDesign("Random Chatting") }) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    bottom = if (WindowInsets.isImeVisible) imeHeight else innerPadding.calculateBottomPadding(),
                    start = innerPadding.calculateStartPadding(LayoutDirection.Ltr),
                    end = innerPadding.calculateEndPadding(LayoutDirection.Ltr),
                    top = innerPadding.calculateTopPadding()
                )
        ) {
            GameScreenMainContent(
                state.value,
                onUiAction = { handleUiAction(it, viewModel) }
            )
        }
    }
}

private fun handleUiAction(action: GameScreenUiAction, viewModel: ChatViewModel) {
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