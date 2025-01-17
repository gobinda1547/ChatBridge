package com.example.matchmakingtest.ui.screen.game.models

sealed class GameScreenUiAction {
    object TryToConnect: GameScreenUiAction()
    data class SendMessage(val message: String): GameScreenUiAction()
}