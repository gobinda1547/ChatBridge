package com.example.matchmakingtest.ui.screen.game

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    navController: NavController,
    viewModel: GameViewModel = hiltViewModel()
) {
    val state = viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                titleContentColor = MaterialTheme.colorScheme.onBackground,
                navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                actionIconContentColor = MaterialTheme.colorScheme.primary
            ),
                title = { Text(text = "Simple chat") }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            GameScreenMainContent(state.value, viewModel)
        }
    }
}

@Composable
fun GameScreenMainContent(state: GameScreenState, viewModel: GameViewModel) {
    when (state.connectionState) {
        ConnectionState.Connecting -> GameScreenMainContentConnecting(state)
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
fun GameScreenMainContentConnecting(state: GameScreenState) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = "Connecting")
    }
}

@Composable
fun GameScreenMainContentConnected(state: GameScreenState, viewModel: GameViewModel) {
    val scrollState = rememberScrollState()
    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .border(1.dp, Color.Gray)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(scrollState)
                    .padding(8.dp) // Inner padding
            ) {
                Text(
                    text = state.message,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Input field and button row
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val inputText = remember { mutableStateOf("") }

            TextField(
                value = inputText.value,
                onValueChange = { inputText.value = it },
                label = { Text("Enter text") },
                modifier = Modifier.weight(1f), // TextField takes up remaining space
            )

            Spacer(modifier = Modifier.width(8.dp))

            Button(onClick = { viewModel.sendMessage(inputText.value) }) {
                Text("Submit")
            }
        }
    }
}