package com.gobinda.connection.signal

import com.gobinda.connection.api.ConnectionRole
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import org.webrtc.IceCandidate

class SignalManager(database: FirebaseDatabase) {

    private val parentRoomRef: DatabaseReference = database.getReference("rooms")

    private val signalSender = SignalSender(parentRoomRef)
    private val signalReceiver = SignalReceiver(parentRoomRef)

    fun sendOffer(toRoom: String, offerSdp: String) =
        signalSender.sendOffer(toRoom, offerSdp)

    fun sendAnswer(toRoom: String, answerSdp: String) =
        signalSender.sendAnswer(toRoom, answerSdp)

    fun sendIceCandidates(
        toRoom: String,
        myRole: ConnectionRole,
        candidates: List<IceCandidate>
    ) = signalSender.sendIceCandidates(toRoom, myRole, candidates)

    fun receiveOffer(fromRoom: String) =
        signalReceiver.receiveOffer(fromRoom)

    fun receiveAnswer(fromRoom: String) =
        signalReceiver.receiveAnswer(fromRoom)

    fun receiveIceCandidates(
        fromRoom: String,
        myRole: ConnectionRole
    ) = signalReceiver.receiveIceCandidates(fromRoom, myRole)

}