package com.gobinda.connection.api

import android.content.Context
import com.gobinda.connection.helper.IceCollector
import com.gobinda.connection.helper.RoomPicker
import com.gobinda.connection.helper.SignalManager
import com.gobinda.connection.helper.confirmConnectionOrWait
import com.gobinda.connection.internal.ConnectionRole
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull

class RemoteConnector(private val context: Context) {

    companion object {
        private const val PICK_ROOM_TIMEOUT = 5000L
        private const val SEND_OFFER_TIMEOUT = 5000L
        private const val RECEIVE_OFFER_TIMEOUT = 20000L
        private const val SEND_ANSWER_TIMEOUT = 5000L
        private const val RECEIVE_ANSWER_TIMEOUT = 10000L
        private const val SEND_ICE_TIMEOUT = 5000L
        private const val RECEIVE_ICE_TIMEOUT = 5000L
        private const val ICE_CANDIDATES_GENERATE_TIMEOUT = 5000L
        private const val CONNECTION_CONFIRMATION_TIMEOUT = 5000L
    }

    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()

    private val signalManager = SignalManager(database)
    private val roomPicker = RoomPicker(database)
    private val iceCollector = IceCollector()

    suspend fun connect(): ConnReqResult {
        val myRoomId = System.currentTimeMillis().toString()
        val receivedRoomId = roomPicker.pickOrWait(myRoomId, PICK_ROOM_TIMEOUT).first()
        return when (receivedRoomId) {
            null -> ConnReqResult.RoomPickingFailed
            myRoomId -> handleAfterJoiningWaitingList(myRoomId)
            else -> handleAfterPartnerRoomFound(receivedRoomId)
        }
    }

    private suspend fun handleAfterPartnerRoomFound(partnerRoomId: String): ConnReqResult {
        val myRole = ConnectionRole.Leader
        val remoteDevice = RemoteDevice(context).initialize()

        val offerSdp = remoteDevice.createOffer().firstOrNull() ?: let {
            return ConnReqResult.OfferCreationFailed
        }
        if (!signalManager.sendOffer(partnerRoomId, offerSdp, SEND_OFFER_TIMEOUT).first()) {
            return ConnReqResult.SendingOfferFailed
        }
        val receivedAnswer = signalManager.receiveAnswer(
            partnerRoomId,
            RECEIVE_ANSWER_TIMEOUT
        ).first() ?: let {
            return ConnReqResult.ReceivingAnswerFailed
        }
        if (remoteDevice.handleAnswer(receivedAnswer).firstOrNull() != true) {
            return ConnReqResult.HandleAnswerFailed
        }
        val myCandidates = iceCollector.collectCandidates(
            remoteDevice.iceCandidates,
            ICE_CANDIDATES_GENERATE_TIMEOUT
        ).first()
        if (myCandidates.isEmpty()) {
            return ConnReqResult.LocalIceCandidatesNotFound
        }
        val isCandidateSendingSuccessful = signalManager.sendIceCandidates(
            partnerRoomId, myRole, myCandidates, SEND_ICE_TIMEOUT
        ).firstOrNull() == true
        if (isCandidateSendingSuccessful.not()) {
            return ConnReqResult.SendingIceCandidateFailed
        }
        val receivedCandidates = signalManager.receiveIceCandidates(
            partnerRoomId, myRole, RECEIVE_ICE_TIMEOUT
        ).firstOrNull()
        if (receivedCandidates == null || receivedCandidates.isEmpty()) {
            return ConnReqResult.RemoteIceCandidatesNotFound
        }
        remoteDevice.handleCandidates(receivedCandidates)
        if (!confirmConnectionOrWait(remoteDevice, CONNECTION_CONFIRMATION_TIMEOUT).first()) {
            return ConnReqResult.CouldNotConfirmConnection
        }
        return ConnReqResult.Successful(remoteDevice)
    }

    private suspend fun handleAfterJoiningWaitingList(myRoomId: String): ConnReqResult {
        val myRole = ConnectionRole.Child
        val remoteDevice = RemoteDevice(context).initialize()

        val receivedOffer = signalManager.receiveOffer(
            myRoomId, RECEIVE_OFFER_TIMEOUT
        ).firstOrNull() ?: let {
            return ConnReqResult.ReceivingOfferFailed
        }
        if (remoteDevice.handleOffer(receivedOffer).firstOrNull() != true) {
            return ConnReqResult.HandleOfferFailed
        }
        val answerSdp = remoteDevice.createAnswer().firstOrNull() ?: let {
            return ConnReqResult.AnswerCreationFailed
        }
        val isAnswerSendingSuccessful = signalManager.sendAnswer(
            myRoomId, answerSdp, SEND_ANSWER_TIMEOUT
        ).firstOrNull() == true
        if (isAnswerSendingSuccessful.not()) {
            return ConnReqResult.SendingAnswerFailed
        }
        val myCandidates = iceCollector.collectCandidates(
            remoteDevice.iceCandidates,
            ICE_CANDIDATES_GENERATE_TIMEOUT
        ).first()
        if (myCandidates.isEmpty()) {
            return ConnReqResult.LocalIceCandidatesNotFound
        }
        val isIceCandidatesSentSuccessfully = signalManager.sendIceCandidates(
            myRoomId, myRole, myCandidates, SEND_ICE_TIMEOUT
        ).firstOrNull() == true
        if (isIceCandidatesSentSuccessfully.not()) {
            return ConnReqResult.SendingIceCandidateFailed
        }
        val receivedCandidates = signalManager.receiveIceCandidates(
            myRoomId, myRole, RECEIVE_ICE_TIMEOUT
        ).firstOrNull()
        if (receivedCandidates == null || receivedCandidates.isEmpty()) {
            return ConnReqResult.RemoteIceCandidatesNotFound
        }
        remoteDevice.handleCandidates(receivedCandidates)
        if (!confirmConnectionOrWait(remoteDevice, CONNECTION_CONFIRMATION_TIMEOUT).first()) {
            return ConnReqResult.CouldNotConfirmConnection
        }
        return ConnReqResult.Successful(remoteDevice)
    }
}