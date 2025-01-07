package com.example.matchmakingtest.connection;

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
        signalingRef.child(roomId).child("offer").setValue(offerSdp)
    }

    // Send answer to Firebase
    fun sendAnswer(roomId: String, answerSdp: String) {
        signalingRef.child(roomId).child("answer").setValue(answerSdp)
    }

    // Send ICE candidate to Firebase
    fun sendIceCandidate(roomId: String, candidate: IceCandidate) {
        signalingRef.child(roomId).child("candidate").push().setValue(candidate.sdp)
    }

    // Listen for offer from Firebase
    fun listenForOffer(roomId: String, callback: (String) -> Unit) {
        signalingRef.child(roomId).child("offer").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val offer = snapshot.getValue(String::class.java)
                offer?.let { callback(it) }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    // Listen for answer from Firebase
    fun listenForAnswer(roomId: String, callback: (String) -> Unit) {
        signalingRef.child(roomId).child("answer").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val answer = snapshot.getValue(String::class.java)
                answer?.let { callback(it) }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    // Listen for ICE candidates from Firebase
    fun listenForCandidates(roomId: String, callback: (IceCandidate) -> Unit) {
        signalingRef.child(roomId).child("candidate").addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val candidateSdp = snapshot.getValue(String::class.java)
                // Convert candidateSdp to IceCandidate
                candidateSdp?.let {
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
}
