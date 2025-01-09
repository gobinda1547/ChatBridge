package com.gobinda.connection.helper

import com.gobinda.connection.internal.ConnectionRole
import com.google.firebase.database.DatabaseReference
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import org.webrtc.IceCandidate

internal class SignalSender(private val parentRoomRef: DatabaseReference) {

    fun sendOffer(toRoom: String, offerSdp: String, timeout: Long) = callbackFlow<Boolean> {
        val currentJob = launch {
            delay(timeout)
            trySend(false)
            close()
        }
        parentRoomRef.child(toRoom).child("offer").setValue(offerSdp)
            .addOnCompleteListener { task ->
                trySend(task.isSuccessful)
                close()
            }
        awaitClose {
            currentJob.cancel()
        }
    }

    fun sendAnswer(toRoom: String, answerSdp: String, timeout: Long) = callbackFlow<Boolean> {
        val currentJob = launch {
            delay(timeout)
            trySend(false)
            close()
        }
        parentRoomRef.child(toRoom).child("answer").setValue(answerSdp)
            .addOnCompleteListener { task ->
                trySend(task.isSuccessful)
                close()
            }
        awaitClose {
            currentJob.cancel()
        }
    }

    fun sendIceCandidates(
        toRoom: String,
        myRole: ConnectionRole,
        candidates: List<IceCandidate>,
        timeout: Long
    ) = callbackFlow<Boolean> {

        val currentJob = launch {
            delay(timeout)
            trySend(false)
            close()
        }

        if (candidates.isEmpty()) {
            trySend(false)
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
                trySend(task.isSuccessful)
                close()
            }

        awaitClose {
            currentJob.cancel()
        }
    }
}