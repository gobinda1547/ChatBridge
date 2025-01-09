package com.gobinda.connection.api

import android.content.Context
import com.gobinda.connection.internal.ConnectionRole
import com.gobinda.connection.internal.li
import com.gobinda.connection.helper.RoomPicker
import com.gobinda.connection.helper.SignalManager
import com.gobinda.connection.internal.le
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull

class ConnectionManager(private val context: Context) {

    companion object {
        private const val PICK_ROOM_TIMEOUT = 20000L
        private const val SEND_OFFER_TIMEOUT = 20000L
        private const val RECEIVE_OFFER_TIMEOUT = 20000L
        private const val SEND_ANSWER_TIMEOUT = 20000L
        private const val RECEIVE_ANSWER_TIMEOUT = 20000L
        private const val SEND_ICE_TIMEOUT = 20000L
        private const val RECEIVE_ICE_TIMEOUT = 20000L
        private const val ICE_CANDIDATES_GENERATE_TIMEOUT = 20000L
    }

    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()

    private val signalManager = SignalManager(database)
    private val roomPicker = RoomPicker(database)

    suspend fun connect(): RemoteDevice? {
        val myRoomId = System.currentTimeMillis().toString()
        val receivedRoomId = roomPicker.pickOrWait(myRoomId, PICK_ROOM_TIMEOUT).first()
        li("myPrefRoom: [$myRoomId], received: [$receivedRoomId]")

        val remoteDevice = when (receivedRoomId) {
            null -> null
            myRoomId -> handleAfterJoiningWaitingList(myRoomId)
            else -> handleAfterPartnerRoomFound(receivedRoomId)
        }
        li("remote device creation successful ? ${remoteDevice != null}")
        return remoteDevice
    }

    private suspend fun handleAfterPartnerRoomFound(partnerRoomId: String): RemoteDevice? {
        val myRole = ConnectionRole.Leader
        val remoteDevice = RemoteDevice(context).initialize()

        val offerSdp = remoteDevice.createOffer().firstOrNull() ?: let {
            le("offer creation failed")
            return null // since offer creation failed
        }
        if (!signalManager.sendOffer(partnerRoomId, offerSdp, SEND_OFFER_TIMEOUT).first()) {
            le("offer sending failed")
            return null // since offer sending failed
        }

        val receivedAnswer =
            signalManager.receiveAnswer(partnerRoomId, RECEIVE_ANSWER_TIMEOUT).first() ?: let {
                le("no answer received")
                return null // since no answer received
            }
        if (remoteDevice.handleAnswer(receivedAnswer).firstOrNull() != true) {
            le("answer could not handled")
            return null // since answer couldn't be handled
        }

        delay(ICE_CANDIDATES_GENERATE_TIMEOUT) // for generating all the ice candidates within 2 seconds
        val myCandidates = remoteDevice.iceCandidates.value
        if (signalManager.sendIceCandidates(partnerRoomId, myRole, myCandidates, SEND_ICE_TIMEOUT)
                .firstOrNull() != true
        ) {
            le("could not send ice candidates")
            return null // since couldn't send ice candidates
        }

        val receivedCandidates =
            signalManager.receiveIceCandidates(partnerRoomId, myRole, RECEIVE_ICE_TIMEOUT)
                .firstOrNull()
        if (receivedCandidates == null || receivedCandidates.isEmpty()) {
            le("no candidates received from remote side")
            return null // since no candidates found
        }
        remoteDevice.handleCandidates(receivedCandidates)

        return remoteDevice
    }

    private suspend fun handleAfterJoiningWaitingList(myRoomId: String): RemoteDevice? {
        val myRole = ConnectionRole.Child
        val remoteDevice = RemoteDevice(context).initialize()

        val receivedOffer =
            signalManager.receiveOffer(myRoomId, RECEIVE_OFFER_TIMEOUT).firstOrNull() ?: let {
                le("offer not received")
                return null // since offer not received
            }
        if (remoteDevice.handleOffer(receivedOffer).firstOrNull() != true) {
            le("could not handled received offer")
            return null // since we couldn't handle received offer
        }

        val answerSdp = remoteDevice.createAnswer().firstOrNull() ?: let {
            le("could not create answer")
            return null // since couldn't create answer
        }
        if (signalManager.sendAnswer(myRoomId, answerSdp, SEND_ANSWER_TIMEOUT)
                .firstOrNull() != true
        ) {
            le("could not send answer")
            return null // since couldn't send answer
        }

        delay(ICE_CANDIDATES_GENERATE_TIMEOUT) // for generating all the ice candidates within 2 seconds
        val myCandidates = remoteDevice.iceCandidates.value
        if (signalManager.sendIceCandidates(myRoomId, myRole, myCandidates, SEND_ICE_TIMEOUT)
                .firstOrNull() != true
        ) {
            le("couldn't send ice candidates")
            return null // since couldn't send ice candidates
        }

        val receivedCandidates =
            signalManager.receiveIceCandidates(myRoomId, myRole, RECEIVE_ICE_TIMEOUT).firstOrNull()
        if (receivedCandidates == null || receivedCandidates.isEmpty()) {
            le("no candidates found from remote")
            return null // since no candidates found
        }
        remoteDevice.handleCandidates(receivedCandidates)

        return remoteDevice
    }
}