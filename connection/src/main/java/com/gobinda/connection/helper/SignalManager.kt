package com.gobinda.connection.helper

import com.gobinda.connection.internal.ConnectionRole
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import org.webrtc.IceCandidate

class SignalManager(database: FirebaseDatabase) {

    private val parentRoomRef: DatabaseReference = database.getReference("rooms")

    private val signalSender = SignalSender(parentRoomRef)
    private val signalReceiver = SignalReceiver(parentRoomRef)

    fun sendOffer(toRoom: String, offerSdp: String, timeout: Long) =
        signalSender.sendOffer(toRoom, offerSdp, timeout)

    fun sendAnswer(toRoom: String, answerSdp: String, timeout: Long) =
        signalSender.sendAnswer(toRoom, answerSdp, timeout)

    fun sendIceCandidates(
        toRoom: String,
        myRole: ConnectionRole,
        candidates: List<IceCandidate>, timeout: Long
    ) = signalSender.sendIceCandidates(toRoom, myRole, candidates, timeout)

    fun receiveOffer(fromRoom: String, timeout: Long) =
        signalReceiver.receiveOffer(fromRoom, timeout)

    fun receiveAnswer(fromRoom: String, timeout: Long) =
        signalReceiver.receiveAnswer(fromRoom, timeout)

    fun receiveIceCandidates(
        fromRoom: String,
        myRole: ConnectionRole, timeout: Long
    ) = signalReceiver.receiveIceCandidates(fromRoom, myRole, timeout)

}