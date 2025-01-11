package com.gobinda.connection.api

import android.content.Context
import com.gobinda.connection.ext.createAnswer
import com.gobinda.connection.ext.createOffer
import com.gobinda.connection.ext.handleAnswer
import com.gobinda.connection.ext.handleCandidates
import com.gobinda.connection.ext.handleOffer
import com.gobinda.connection.helper.IceCollector
import com.gobinda.connection.helper.RoomPicker
import com.gobinda.connection.helper.SignalManager
import com.gobinda.connection.helper.confirmConnectionOrWait
import com.gobinda.connection.internal.ConnectionRole
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.flow.first

class RemoteConnector(private val context: Context) {

    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()

    private val signalManager = SignalManager(database)
    private val roomPicker = RoomPicker(database)
    private val iceCollector = IceCollector()

    suspend fun connect(): ConnReqResult {
        val myRoomId = System.currentTimeMillis().toString()
        val receivedRoomId = roomPicker.pickOrWait(myRoomId).first() ?: let {
            return ConnReqResult.RoomPickingFailed
        }
        return when (receivedRoomId) {
            myRoomId -> handleAfterJoiningWaitingList(myRoomId)
            else -> handleAfterPartnerRoomFound(receivedRoomId)
        }
    }

    private suspend fun handleAfterPartnerRoomFound(partnerRoomId: String): ConnReqResult {
        val myRole = ConnectionRole.Leader
        val remoteDevice = RemoteDevice(context, myRole)

        val offerSdp = remoteDevice.createOffer().first() ?: let {
            return ConnReqResult.OfferCreationFailed
        }
        signalManager.sendOffer(partnerRoomId, offerSdp).first() ?: let {
            return ConnReqResult.SendingOfferFailed
        }
        val receivedAnswer = signalManager.receiveAnswer(partnerRoomId).first() ?: let {
            return ConnReqResult.ReceivingAnswerFailed
        }
        remoteDevice.handleAnswer(receivedAnswer).first() ?: let {
            return ConnReqResult.HandleAnswerFailed
        }
        return handleAfterInitialHandshake(remoteDevice, partnerRoomId, myRole)
    }

    private suspend fun handleAfterJoiningWaitingList(myRoomId: String): ConnReqResult {
        val myRole = ConnectionRole.Child
        val remoteDevice = RemoteDevice(context, myRole)

        val receivedOffer = signalManager.receiveOffer(myRoomId).first() ?: let {
            return ConnReqResult.ReceivingOfferFailed
        }
        remoteDevice.handleOffer(receivedOffer).first() ?: let {
            return ConnReqResult.HandleOfferFailed
        }
        val answerSdp = remoteDevice.createAnswer().first() ?: let {
            return ConnReqResult.AnswerCreationFailed
        }
        signalManager.sendAnswer(myRoomId, answerSdp).first() ?: let {
            return ConnReqResult.SendingAnswerFailed
        }
        return handleAfterInitialHandshake(remoteDevice, myRoomId, myRole)
    }

    private suspend fun handleAfterInitialHandshake(
        remoteDevice: RemoteDevice,
        roomId: String,
        myRole: ConnectionRole
    ): ConnReqResult {
        val myIces = iceCollector.collectCandidates(remoteDevice.iceCandidates).first() ?: let {
            return ConnReqResult.LocalIceCandidatesNotFound
        }
        signalManager.sendIceCandidates(roomId, myRole, myIces).first() ?: let {
            return ConnReqResult.SendingIceCandidateFailed
        }
        val remoteIces = signalManager.receiveIceCandidates(roomId, myRole).first() ?: let {
            return ConnReqResult.RemoteIceCandidatesNotFound
        }
        remoteDevice.handleCandidates(remoteIces)
        confirmConnectionOrWait(remoteDevice).first() ?: let {
            return ConnReqResult.CouldNotConfirmConnection
        }
        return ConnReqResult.Successful(remoteDevice)
    }
}