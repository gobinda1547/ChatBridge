package com.example.matchmakingtest.connection

import com.example.matchmakingtest.app.logI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

class WebOfferManager(private val mediator: ConnectionMediator) {

    companion object {
        private const val OFFER_KEY = "offer"
        private const val TIMEOUT_FOR_SHARE_OFFER = 10000L
    }

    fun send(roomId: String, userId: String, offerSdp: String) = callbackFlow {
        val timerJob = launch(Dispatchers.IO) {
            delay(TIMEOUT_FOR_SHARE_OFFER)
            trySend(false)
            close()
        }

        mediator.roomDbRef.child(roomId).child(userId).child(OFFER_KEY).setValue(offerSdp)
            .addOnCompleteListener { taskStatus ->
                timerJob.cancel()
                trySend(taskStatus.isSuccessful)
                close()
            }

        awaitClose()
    }
}