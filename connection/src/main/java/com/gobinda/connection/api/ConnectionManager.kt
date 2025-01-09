package com.gobinda.connection.api

import android.content.Context
import com.gobinda.connection.connector.RemoteDevice
import com.gobinda.connection.log.li
import com.gobinda.connection.picker.RoomPicker
import com.gobinda.connection.signal.SignalManager
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull

class ConnectionManager(private val context: Context) {

    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()

    private val signalManager = SignalManager(database)
    private val roomPicker = RoomPicker(database)

    suspend fun connect(): RemoteDevice? {
        val myPreferredRoomId = System.currentTimeMillis().toString()
        val receivedRoomId = roomPicker.pickOrWait(myPreferredRoomId).firstOrNull()
        li("myPrefRoom: [$myPreferredRoomId], received: [$receivedRoomId]")

        val remoteDevice = when (receivedRoomId) {
            null -> null
            myPreferredRoomId -> handleAfterJoiningWaitingList(myPreferredRoomId)
            else -> handleAfterPartnerRoomFound(receivedRoomId)
        }
        li("remote device creation successful ? ${remoteDevice != null}")
        return remoteDevice
    }

    private suspend fun handleAfterPartnerRoomFound(partnerRoomId: String): RemoteDevice? {
        val myRole = ConnectionRole.Leader
        val remoteDevice = RemoteDevice(context).initialize()

        val offerSdp = remoteDevice.createOffer().firstOrNull() ?: let {
            return null // since offer creation failed
        }
        if (signalManager.sendOffer(partnerRoomId, offerSdp).firstOrNull() != true) {
            return null // since offer sending failed
        }

        val receivedAnswer = signalManager.receiveAnswer(partnerRoomId).firstOrNull() ?: let {
            return null // since no answer received
        }
        if (remoteDevice.handleAnswer(receivedAnswer).firstOrNull() != true) {
            return null // since answer couldn't be handled
        }

        delay(2000) // for generating all the ice candidates within 2 seconds
        val myCandidates = remoteDevice.iceCandidates.value
        if (signalManager.sendIceCandidates(partnerRoomId, myRole, myCandidates)
                .firstOrNull() != true
        ) {
            return null // since couldn't send ice candidates
        }

        val receivedCandidates =
            signalManager.receiveIceCandidates(partnerRoomId, myRole).firstOrNull()
        if (receivedCandidates == null || receivedCandidates.isEmpty()) {
            return null // since no candidates found
        }
        remoteDevice.handleCandidates(receivedCandidates)

        return remoteDevice
    }

    private suspend fun handleAfterJoiningWaitingList(myRoomId: String): RemoteDevice? {
        val myRole = ConnectionRole.Child
        val remoteDevice = RemoteDevice(context).initialize()

        val receivedOffer = signalManager.receiveOffer(myRoomId).firstOrNull() ?: let {
            return null // since offer not received
        }
        if (remoteDevice.handleOffer(receivedOffer).firstOrNull() != true) {
            return null // since we couldn't handle received offer
        }

        val answerSdp = remoteDevice.createAnswer().firstOrNull() ?: let {
            return null // since couldn't create answer
        }
        if (signalManager.sendAnswer(myRoomId, answerSdp).firstOrNull() != true) {
            return null // since couldn't send answer
        }

        delay(2000) // for generating all the ice candidates within 2 seconds
        val myCandidates = remoteDevice.iceCandidates.value
        if (signalManager.sendIceCandidates(myRoomId, myRole, myCandidates).firstOrNull() != true) {
            return null // since couldn't send ice candidates
        }

        val receivedCandidates = signalManager.receiveIceCandidates(myRoomId, myRole).firstOrNull()
        if (receivedCandidates == null || receivedCandidates.isEmpty()) {
            return null // since no candidates found
        }
        remoteDevice.handleCandidates(receivedCandidates)

        return remoteDevice
    }
}