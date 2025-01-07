package com.example.matchmakingtest.connection

import android.content.Context
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject


class ConnectionManager @Inject constructor(
    @ApplicationContext applicationContext: Context
) : ConnectionMediator() {

    override val offerManager = WebOfferManager(this)
    override val answerManager = WebAnswerManager(this)
    override val candidateFinder = WebIceCandidateFinder(this)
    override val candidateReceiver = WebIceCandidateReceiver(this)
    override val candidateSender = WebIceCandidateSender(this)
    override val peerConnectionCreator = WebPeerConnectionCreator(this)

    override val context: Context = applicationContext

    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()
    override val roomDbRef: DatabaseReference = database.getReference("rooms")

}