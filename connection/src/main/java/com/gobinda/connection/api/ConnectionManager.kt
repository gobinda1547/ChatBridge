package com.gobinda.connection.api

import android.content.Context
import com.gobinda.connection.picker.RoomPicker
import com.gobinda.connection.signal.SignalReceiver
import com.gobinda.connection.signal.SignalSender
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.flow.Flow

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

    override suspend fun receiveOffer(myRoomId: String): Flow<String?> {
        return signalReceiver.receiveOffer(myRoomId)
    }


}