package com.gobinda.connection.connector

import com.gobinda.connection.api.ConnectionMediator
import com.gobinda.connection.log.li
import kotlinx.coroutines.flow.firstOrNull

class RemoteConnector(private val mediator: ConnectionMediator) {

    suspend fun connect(): RemoteDevice? {
        val myPreferredRoomId = System.currentTimeMillis().toString()
        val receivedRoomId = mediator.pickRoomOrWaitInQueue(myPreferredRoomId).firstOrNull()
        li("myPrefRoom: [$myPreferredRoomId], received: [$receivedRoomId]")

        if (receivedRoomId == null) {
            // since partner not found and also my information upload failed
            // we will return indicating that connection not possible
            // the caller may try again, that's up to upper layer
            return null
        }
        if (receivedRoomId == myPreferredRoomId) {
            // that means we are in the waiting list
            // so we will wait for the offer from a leader
            return handleAfterJoiningWaitingList(myPreferredRoomId)
        }
        // since above cases are failed that means I am a leader now
        // and the room id I have received contains my partner or child's room id
        return handleAfterPartnerRoomFound(receivedRoomId)
    }

    private suspend fun handleAfterPartnerRoomFound(partnerRoomId: String): RemoteDevice? {
        return null
    }

    private suspend fun handleAfterJoiningWaitingList(myPreferredRoomId: String): RemoteDevice? {
        val receivedOffer = mediator.receiveOffer(myPreferredRoomId).firstOrNull()
        li("received offer: [$receivedOffer]")

        if (receivedOffer == null) {
            // since we couldn't receive any offer so connection is not possible
            // returning null indicating that new connection
            return null
        }

        mediator.send
    }


}