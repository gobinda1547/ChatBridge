package com.example.matchmakingtest.ui.screen.game

sealed class GameScreenUiAction {
    object TryToConnect: GameScreenUiAction()
    data class SendMessage(val message: String): GameScreenUiAction()
}