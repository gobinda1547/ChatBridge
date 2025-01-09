package com.gobinda.connection.helper

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import org.webrtc.IceCandidate

class IceCollector {

    fun collectCandidates(stateFlow: StateFlow<List<IceCandidate>>, timeout: Long) = callbackFlow {
        val candidates = MutableStateFlow<List<IceCandidate>>(emptyList())

        val timerJob = launch {
            delay(timeout)
            trySend(candidates.value)
            close()
        }

        val collectingJob = launch {
            stateFlow.collect { currentCandidates ->
                candidates.emit(currentCandidates)
                if (currentCandidates.size >= 5) {
                    trySend(candidates.value)
                    close()
                }
            }
        }

        awaitClose {
            timerJob.cancel()
            collectingJob.cancel()
        }
    }
}