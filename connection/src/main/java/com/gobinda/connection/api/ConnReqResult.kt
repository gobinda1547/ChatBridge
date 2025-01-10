package com.gobinda.connection.api

sealed interface ConnReqResult {
    data class Successful(val remoteDevice: RemoteDeviceApi) : ConnReqResult
    data object RoomPickingFailed : ConnReqResult
    data object OfferCreationFailed : ConnReqResult
    data object AnswerCreationFailed : ConnReqResult
    data object SendingOfferFailed : ConnReqResult
    data object SendingAnswerFailed : ConnReqResult
    data object ReceivingOfferFailed : ConnReqResult
    data object ReceivingAnswerFailed : ConnReqResult
    data object HandleAnswerFailed : ConnReqResult
    data object HandleOfferFailed : ConnReqResult
    data object LocalIceCandidatesNotFound : ConnReqResult
    data object RemoteIceCandidatesNotFound : ConnReqResult
    data object SendingIceCandidateFailed : ConnReqResult
    data object CouldNotConfirmConnection : ConnReqResult
}