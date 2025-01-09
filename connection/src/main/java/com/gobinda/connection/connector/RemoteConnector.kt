package com.gobinda.connection.connector

import com.gobinda.connection.api.ConnectionMediator
import com.gobinda.connection.api.ConnectionRole
import com.gobinda.connection.log.li
import kotlinx.coroutines.flow.firstOrNull

class RemoteConnector(private val mediator: ConnectionMediator) {

    suspend fun connect(): RemoteDevice? {
        val myPreferredRoomId = System.currentTimeMillis().toString()
        val receivedRoomId = mediator.pickRoomOrWaitInQueue(myPreferredRoomId).firstOrNull()
        li("myPrefRoom: [$myPreferredRoomId], received: [$receivedRoomId]")

        return when (receivedRoomId) {
            null -> null
            myPreferredRoomId -> handleAfterJoiningWaitingList(myPreferredRoomId)
            else -> handleAfterPartnerRoomFound(receivedRoomId)
        }
    }

    private suspend fun handleAfterPartnerRoomFound(partnerRoomId: String): RemoteDevice? {
        val myRole = ConnectionRole.Leader
        val remoteDevice = mediator.createRemoteDevice()
        val offerSdp = remoteDevice.createOffer().firstOrNull() ?: let {
            return null // since offer creation failed
        }
        if (mediator.sendOffer(partnerRoomId, offerSdp).firstOrNull() != true) {
            return null // since offer sending failed
        }
        val receivedAnswer = mediator.receiveAnswer(partnerRoomId).firstOrNull() ?: let {
            return null // since no answer received
        }
        if (remoteDevice.handleAnswer(receivedAnswer).firstOrNull() != true) {
            return null // since answer couldn't be handled
        }
        val candidates = mediator.receiveIceCandidates(partnerRoomId, myRole).firstOrNull()
        if (candidates == null || candidates.isEmpty()) {
            return null // since no candidates found
        }
        remoteDevice.handleCandidates(candidates)
        return remoteDevice
    }

    private suspend fun handleAfterJoiningWaitingList(myRoomId: String): RemoteDevice? {
        val myRole = ConnectionRole.Child
        val remoteDevice = mediator.createRemoteDevice()
        val receivedOffer = mediator.receiveOffer(myRoomId).firstOrNull() ?: let {
            return null // since offer not received
        }
        if (remoteDevice.handleOffer(receivedOffer).firstOrNull() != true) {
            return null // since we couldn't handle received offer
        }
        val answerSdp = remoteDevice.createAnswer().firstOrNull() ?: let {
            return null // since couldn't create answer
        }
        if (mediator.sendAnswer(myRoomId, answerSdp).firstOrNull() != true) {
            return null // since couldn't send answer
        }
        val candidates = mediator.receiveIceCandidates(myRoomId, myRole).firstOrNull()
        if (candidates == null || candidates.isEmpty()) {
            return null // since no candidates found
        }
        remoteDevice.handleCandidates(candidates)
        return remoteDevice
    }


}