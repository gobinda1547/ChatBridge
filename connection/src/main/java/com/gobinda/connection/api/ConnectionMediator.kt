package com.gobinda.connection.api

import android.content.Context
import com.google.firebase.database.DatabaseReference
import kotlinx.coroutines.flow.Flow

abstract class ConnectionMediator {

    abstract val context: Context
    abstract val parentRoomRef: DatabaseReference
    abstract val waitingRoomRef: DatabaseReference

    abstract suspend fun pickRoomOrWaitInQueue(myRoomId: String): Flow<String?>
    abstract suspend fun receiveOffer(myRoomId: String): Flow<String?>

}