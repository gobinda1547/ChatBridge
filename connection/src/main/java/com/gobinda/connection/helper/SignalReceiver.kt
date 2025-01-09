package com.gobinda.connection.helper

import com.gobinda.connection.internal.ConnectionRole
import com.gobinda.connection.internal.le
import com.google.firebase.database.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import org.webrtc.*
import kotlin.collections.get

internal class SignalReceiver(private val parentRoomRef: DatabaseReference) {

    fun receiveOffer(fromRoom: String, timeout: Long) = callbackFlow<String?> {
        val currentJob = launch {
            delay(timeout)
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
                le("receiveOffer: found database error ${error.message}")
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

    fun receiveAnswer(fromRoom: String, timeout: Long) = callbackFlow<String?> {
        val currentJob = launch {
            delay(timeout)
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
                le("receiveAnswer: found database error ${error.message}")
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
        myRole: ConnectionRole, timeout: Long
    ) = callbackFlow<List<IceCandidate>?> {
        val currentJob = launch {
            delay(timeout)
            trySend(null)
            close()
        }
        val icePath = fromWhereToReceiveIceCandidates(myRole)
        val candidateDataReference = parentRoomRef.child(fromRoom).child(icePath)
        val candidateListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists() == false) return
                trySend(parseSnapshotIntoCandidates(snapshot))
                close()
            }

            override fun onCancelled(error: DatabaseError) {
                le("receiveIceCandidates: found database error ${error.message}")
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
