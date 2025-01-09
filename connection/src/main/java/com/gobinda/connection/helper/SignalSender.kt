package com.gobinda.connection.helper

import com.gobinda.connection.internal.ConnectionRole
import com.google.firebase.database.DatabaseReference
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import org.webrtc.IceCandidate

class SignalSender(private val parentRoomRef: DatabaseReference) {

    fun sendOffer(toRoom: String, offerSdp: String) = callbackFlow<Boolean> {
        parentRoomRef.child(toRoom).child("offer").setValue(offerSdp)
            .addOnCompleteListener { task ->
                trySend(task.isSuccessful)
                close()
            }
        awaitClose()
    }

    fun sendAnswer(toRoom: String, answerSdp: String) = callbackFlow<Boolean> {
        parentRoomRef.child(toRoom).child("answer").setValue(answerSdp)
            .addOnCompleteListener { task ->
                trySend(task.isSuccessful)
                close()
            }
        awaitClose()
    }

    fun sendIceCandidates(
        toRoom: String,
        myRole: ConnectionRole,
        candidates: List<IceCandidate>
    ) = callbackFlow<Boolean> {

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

        awaitClose()
    }
}