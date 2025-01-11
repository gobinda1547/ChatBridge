package com.gobinda.connection.helper

import com.gobinda.connection.api.ICE_CANDIDATES_GENERATE_TIMEOUT
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import org.webrtc.IceCandidate

internal class IceCollector {

    fun collectCandidates(stateFlow: StateFlow<List<IceCandidate>>) = callbackFlow {
        val candidates = MutableStateFlow<List<IceCandidate>>(emptyList())

        val timerJob = launch {
            delay(ICE_CANDIDATES_GENERATE_TIMEOUT)
            val myCandidates = candidates.value
            trySend(if (myCandidates.isNotEmpty()) myCandidates else null)
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