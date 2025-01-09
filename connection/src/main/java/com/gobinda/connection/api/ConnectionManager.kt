package com.gobinda.connection.api

import android.content.Context
import com.gobinda.connection.connector.RemoteDevice
import com.gobinda.connection.picker.RoomPicker
import com.gobinda.connection.signal.SignalReceiver
import com.gobinda.connection.signal.SignalSender
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.flow.Flow
import org.webrtc.IceCandidate

class ConnectionManager(applicationContext: Context) : ConnectionMediator() {
    private val roomPicker = RoomPicker(this)
    private val signalSender = SignalSender(this)
    private val signalReceiver = SignalReceiver(this)

    override val context: Context = applicationContext

    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()
    override val parentRoomRef: DatabaseReference = database.getReference("rooms")
    override val waitingRoomRef: DatabaseReference = database.getReference("waiting")

    override suspend fun pickRoomOrWaitInQueue(myRoomId: String): Flow<String?> {
        return roomPicker.pickOrWait(myRoomId)
    }

    override suspend fun receiveOffer(roomId: String): Flow<String?> {
        return signalReceiver.receiveOffer(roomId)
    }

    override suspend fun receiveAnswer(roomId: String): Flow<String?> {
        return signalReceiver.receiveAnswer(roomId)
    }

    override suspend fun sendOffer(roomId: String, offerSdp: String): Flow<Boolean> {
        return signalSender.sendOffer(roomId, offerSdp)
    }

    override suspend fun sendAnswer(roomId: String, answerSdp: String): Flow<Boolean> {
        return signalSender.sendAnswer(roomId, answerSdp)
    }

    override fun sendIceCandidate(
        toRoom: String,
        myRole: ConnectionRole,
        candidates: List<IceCandidate>
    ): Flow<Boolean> {
        return signalSender.sendIceCandidate(toRoom, myRole, candidates)
    }

    override suspend fun receiveIceCandidates(
        roomId: String, myRole: ConnectionRole
    ): Flow<List<IceCandidate>?> {
        return signalReceiver.receiveIceCandidate(roomId, myRole)
    }

    override fun createRemoteDevice(): RemoteDevice {
        return RemoteDevice(context).initialize()
    }
}