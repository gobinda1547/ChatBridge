package com.example.matchmakingtest.ui.screen.game

data class GameScreenState(
    val connectionState: ConnectionState = ConnectionState.NotConnected,
    val messages: List<SingleMessage> = emptyList()
)

data class SingleMessage(
    val text: String,
    val isSentOrReceived: MessageSentOrReceived
)

sealed interface MessageSentOrReceived {
    data object Sent : MessageSentOrReceived
    data object Received : MessageSentOrReceived
    data object SendingFailed : MessageSentOrReceived
}

sealed interface ConnectionState {
    data object NotConnected : ConnectionState
    data object Connecting : ConnectionState
    data object Connected : ConnectionState
}