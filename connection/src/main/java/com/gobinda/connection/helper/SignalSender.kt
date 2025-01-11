package com.gobinda.connection.helper

import com.gobinda.connection.api.SEND_ANSWER_TIMEOUT
import com.gobinda.connection.api.SEND_ICE_TIMEOUT
import com.gobinda.connection.api.SEND_OFFER_TIMEOUT
import com.gobinda.connection.internal.ConnectionRole
import com.google.firebase.database.DatabaseReference
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import org.webrtc.IceCandidate

internal class SignalSender(private val parentRoomRef: DatabaseReference) {

    fun sendOffer(toRoom: String, offerSdp: String) = callbackFlow<Any?> {
        val currentJob = launch {
            delay(SEND_OFFER_TIMEOUT)
            trySend(null)
            close()
        }
        parentRoomRef.child(toRoom).child("offer").setValue(offerSdp)
            .addOnCompleteListener { task ->
                trySend(if (task.isSuccessful) Any() else null)
                close()
            }
        awaitClose {
            currentJob.cancel()
        }
    }

    fun sendAnswer(toRoom: String, answerSdp: String) = callbackFlow<Any?> {
        val currentJob = launch {
            delay(SEND_ANSWER_TIMEOUT)
            trySend(null)
            close()
        }
        parentRoomRef.child(toRoom).child("answer").setValue(answerSdp)
            .addOnCompleteListener { task ->
                trySend(if (task.isSuccessful) Any() else null)
                close()
            }
        awaitClose {
            currentJob.cancel()
        }
    }

    fun sendIceCandidates(
        toRoom: String,
        myRole: ConnectionRole,
        candidates: List<IceCandidate>
    ) = callbackFlow<Any?> {

        val currentJob = launch {
            delay(SEND_ICE_TIMEOUT)
            trySend(null)
            close()
        }

        if (candidates.isEmpty()) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val icePath = whereToUploadIceCandidates(myRole)
        val candidateMapList = candidates.map { candidate ->
            mapOf(
                "sdpMid" to candidate.sdpMid,
                "sdpMLineIndex" to candidate.sdpMLineIndex,
                "candidate" to candidate.sdp
            )
        }

        parentRoomRef.child(toRoom).child(icePath).setValue(candidateMapList)
            .addOnCompleteListener { task ->
                trySend(if (task.isSuccessful) Any() else null)
                close()
            }

        awaitClose {
            currentJob.cancel()
        }
    }
}