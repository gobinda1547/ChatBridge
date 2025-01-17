package com.example.matchmakingtest.connection

import android.content.Context
import com.google.firebase.database.DatabaseReference

abstract class ConnectionMediator {

    abstract val offerManager: WebOfferManager
    abstract val answerManager: WebAnswerManager
    abstract val candidateFinder: WebIceCandidateFinder
    abstract val candidateReceiver: WebIceCandidateReceiver
    abstract val candidateSender: WebIceCandidateSender
    abstract val peerConnectionCreator: WebPeerConnectionCreator

    abstract val context: Context
    abstract val roomDbRef: DatabaseReference
}