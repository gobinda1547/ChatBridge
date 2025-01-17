package com.gobinda.connection.helper

import com.gobinda.connection.api.RECEIVE_ANSWER_TIMEOUT
import com.gobinda.connection.api.RECEIVE_ICE_TIMEOUT
import com.gobinda.connection.api.RECEIVE_OFFER_TIMEOUT
import com.gobinda.connection.internal.ConnectionRole
import com.google.firebase.database.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import org.webrtc.*
import kotlin.collections.get

internal class SignalReceiver(private val parentRoomRef: DatabaseReference) {

    fun receiveOffer(fromRoom: String) = callbackFlow<String?> {
        val currentJob = launch {
            delay(RECEIVE_OFFER_TIMEOUT)
            trySend(null)
            close()
        }
        val offerDataReference = parentRoomRef.child(fromRoom).child("offer")
        val offerListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists() == false) return
                snapshot.getValue(String::class.java)?.let { actualOffer ->
                    trySend(actualOffer)
                    close()
                } ?: let {
                    trySend(null)
                    close()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(null)
                close()
            }
        }

        offerDataReference.addValueEventListener(offerListener)
        awaitClose {
            currentJob.cancel()
            offerDataReference.removeEventListener(offerListener)
        }
    }

    fun receiveAnswer(fromRoom: String) = callbackFlow<String?> {
        val currentJob = launch {
            delay(RECEIVE_ANSWER_TIMEOUT)
            trySend(null)
            close()
        }
        val answerDataReference = parentRoomRef.child(fromRoom).child("answer")
        val answerListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists() == false) return
                snapshot.getValue(String::class.java)?.let { actualAnswer ->
                    trySend(actualAnswer)
                    close()
                } ?: let {
                    trySend(null)
                    close()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(null)
                close()
            }
        }

        answerDataReference.addValueEventListener(answerListener)
        awaitClose {
            currentJob.cancel()
            answerDataReference.removeEventListener(answerListener)
        }
    }

    fun receiveIceCandidates(
        fromRoom: String,
        myRole: ConnectionRole
    ) = callbackFlow<List<IceCandidate>?> {
        val currentJob = launch {
            delay(RECEIVE_ICE_TIMEOUT)
            trySend(null)
            close()
        }
        val icePath = fromWhereToReceiveIceCandidates(myRole)
        val candidateDataReference = parentRoomRef.child(fromRoom).child(icePath)
        val candidateListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists() == false) return
                val partnerIces = parseSnapshotIntoCandidates(snapshot) ?: emptyList()
                trySend(if (partnerIces.isNotEmpty()) partnerIces else null)
                close()
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(null)
                close()
            }
        }

        candidateDataReference.addValueEventListener(candidateListener)
        awaitClose {
            currentJob.cancel()
            candidateDataReference.removeEventListener(candidateListener)
        }
    }

    private fun parseSnapshotIntoCandidates(snapshot: DataSnapshot): List<IceCandidate>? {
        val candidateMapList = snapshot.value as? List<*> ?: return null
        return candidateMapList.mapNotNull { candidateMapRaw ->
            val candidateMap = candidateMapRaw as? Map<*, *> ?: return@mapNotNull null
            val sdpMid = candidateMap["sdpMid"] as? String
            val sdpMLineIndex = (candidateMap["sdpMLineIndex"] as? Long)?.toInt()
            val sdp = candidateMap["candidate"] as? String
            if (sdpMid != null && sdpMLineIndex != null && sdp != null) {
                IceCandidate(sdpMid, sdpMLineIndex, sdp)
            } else {
                null // Skip invalid entries
            }
        }
    }

}
