package com.gobinda.connection.api

import android.content.Context
import com.gobinda.connection.connector.RemoteDevice
import com.google.firebase.database.DatabaseReference
import kotlinx.coroutines.flow.Flow
import org.webrtc.IceCandidate

abstract class ConnectionMediator {

    abstract val context: Context
    abstract val parentRoomRef: DatabaseReference
    abstract val waitingRoomRef: DatabaseReference

    abstract suspend fun pickRoomOrWaitInQueue(myRoomId: String): Flow<String?>
    abstract suspend fun receiveOffer(roomId: String): Flow<String?>
    abstract suspend fun receiveAnswer(roomId: String): Flow<String?>
    abstract suspend fun sendOffer(roomId: String, offerSdp: String): Flow<Boolean>
    abstract suspend fun sendAnswer(roomId: String, answerSdp: String): Flow<Boolean>

    abstract suspend fun receiveIceCandidates(roomId: String, myRole: ConnectionRole): Flow<List<IceCandidate>?>
    abstract fun createRemoteDevice(): RemoteDevice

}