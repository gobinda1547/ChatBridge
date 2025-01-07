package com.example.matchmakingtest.ui.screen.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.matchmakingtest.connection.SignalingManager
import com.example.matchmakingtest.connection.WebRTCManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GameViewModel @Inject constructor(
    private val signalingManager: SignalingManager,
    private val webRTCManager: WebRTCManager
) : ViewModel() {

    val roomId = "sampleRoom"

    init {
        viewModelScope.launch(Dispatchers.IO) {
            signalingManager.listenForOffer(roomId) { offer ->
                webRTCManager.createAnswer(roomId, offer)
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            signalingManager.listenForAnswer(roomId) { answer ->
                webRTCManager.handleAnswer(answer)
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            signalingManager.listenForCandidates(roomId) { candidate ->
                webRTCManager.handleCandidate(roomId, candidate)
            }
        }
    }

    fun handleSendOffer() {
        viewModelScope.launch(Dispatchers.IO) {
            webRTCManager.createOffer(roomId)
        }
    }

    fun listenForTheAnswerFromFirebase() {

    }

}