package com.example.matchmakingtest.ui.screen.game

data class GameScreenState(
    val connectionState: ConnectionState = ConnectionState.NotConnected,
    val message: String = ""
)

sealed interface ConnectionState {
    data object NotConnected : ConnectionState
    data object Connecting : ConnectionState
    data object Connected : ConnectionState
}