package com.example.matchmakingtest.connection;

import com.example.matchmakingtest.app.logI
import org.webrtc.*
import com.google.firebase.database.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SignalingManager @Inject constructor() {

    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()
    private val signalingRef: DatabaseReference = database.getReference("rooms")

    // Send offer to Firebase
    fun sendOffer(roomId: String, offerSdp: String) {
        logI("sending offer")
        signalingRef.child(roomId).child("offer").setValue(offerSdp)
    }

    // Send answer to Firebase
    fun sendAnswer(roomId: String, answerSdp: String) {
        logI("sending answer")
        signalingRef.child(roomId).child("answer").setValue(answerSdp)
    }

    // Send ICE candidate to Firebase
    fun sendIceCandidateOld(roomId: String, path: String, candidate: IceCandidate) {
        logI("sending ice candidate")
        signalingRef.child(roomId).child("candidate$path").push().setValue(candidate.sdp)
    }

    fun sendIceCandidate(roomId: String, path: String, candidate: IceCandidate) {
        logI("Sending ICE candidate: ${candidate.sdp}")
        val candidateMap = mapOf(
            "sdpMid" to candidate.sdpMid,
            "sdpMLineIndex" to candidate.sdpMLineIndex,
            "candidate" to candidate.sdp
        )
        signalingRef.child(roomId).child("candidate$path").push().setValue(candidateMap)
    }

    // Listen for offer from Firebase
    fun listenForOffer(roomId: String, callback: (String) -> Unit) {
        signalingRef.child(roomId).child("offer").addValueEventListener(
            object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val offer = snapshot.getValue(String::class.java)
                    offer?.let {
                        logI("on offer received $it")
                        callback(it)
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            }
        )
    }

    // Listen for answer from Firebase
    fun listenForAnswer(roomId: String, callback: (String) -> Unit) {
        signalingRef.child(roomId).child("answer")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val answer = snapshot.getValue(String::class.java)
                    answer?.let {
                        logI("on answer received")
                        callback(it)
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    // Listen for ICE candidates from Firebase
    fun listenForCandidatesOld(roomId: String, path: String, callback: (IceCandidate) -> Unit) {
        signalingRef.child(roomId).child("candidate$path")
            .addChildEventListener(object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    val candidateSdp = snapshot.getValue(String::class.java)
                    // Convert candidateSdp to IceCandidate
                    candidateSdp?.let {
                        logI("on candidate received $it")
                        val candidate = IceCandidate("sdpMid", 0, it)
                        callback(candidate)
                    }
                }

                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onChildRemoved(snapshot: DataSnapshot) {}
                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    fun listenForCandidates(roomId: String, path: String, onCandidate: (IceCandidate) -> Unit) {
        signalingRef.child(roomId).child("candidate$path")
            .addChildEventListener(object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    val candidateData = snapshot.value as? Map<*, *> ?: return
                    val sdpMid = candidateData["sdpMid"] as? String
                    val sdpMLineIndex = (candidateData["sdpMLineIndex"] as? Long)?.toInt()
                    val sdp = candidateData["candidate"] as? String

                    if (sdpMid != null && sdpMLineIndex != null && sdp != null) {
                        val candidate = IceCandidate(sdpMid, sdpMLineIndex, sdp)
                        logI("Received ICE candidate: $sdp")
                        onCandidate(candidate)
                    }
                }

                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onChildRemoved(snapshot: DataSnapshot) {}
                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onCancelled(error: DatabaseError) {}
            })
    }
}
