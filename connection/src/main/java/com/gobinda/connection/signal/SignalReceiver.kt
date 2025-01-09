package com.gobinda.connection.signal

import com.gobinda.connection.api.ConnectionMediator
import com.gobinda.connection.api.ConnectionRole
import com.gobinda.connection.log.le
import com.google.firebase.database.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import org.webrtc.*
import kotlin.collections.get

class SignalReceiver(private val mediator: ConnectionMediator) {

    fun receiveOffer(fromRoom: String) = callbackFlow<String?> {
        val offerDataReference = mediator.parentRoomRef.child(fromRoom).child("offer")
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
        awaitClose { offerDataReference.removeEventListener(offerListener) }
    }

    fun receiveAnswer(fromRoom: String) = callbackFlow<String?> {
        val answerDataReference = mediator.parentRoomRef.child(fromRoom).child("answer")
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
        awaitClose { answerDataReference.removeEventListener(answerListener) }
    }

    fun receiveIceCandidate(
        fromRoom: String,
        myRole: ConnectionRole
    ) = callbackFlow<List<IceCandidate>?> {
        val icePath = fromWhereToReceiveIceCandidates(myRole)
        val candidateDataReference = mediator.parentRoomRef.child(fromRoom).child(icePath)
        val candidateListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists() == false) return
                trySend(parseSnapshotIntoCandidates(snapshot))
                close()
            }

            override fun onCancelled(error: DatabaseError) {
                le("receiveIceCandidate: found database error ${error.message}")
                trySend(null)
                close()
            }
        }

        candidateDataReference.addValueEventListener(candidateListener)
        awaitClose { candidateDataReference.removeEventListener(candidateListener) }
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
