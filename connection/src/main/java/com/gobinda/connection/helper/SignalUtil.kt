package com.gobinda.connection.helper

import com.gobinda.connection.api.RemoteDevice
import com.gobinda.connection.internal.ConnectionRole
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

private const val ICE_CANDIDATE_DB_PREFIX = "ice_"

internal fun whereToUploadIceCandidates(myRole: ConnectionRole): String {
    return ICE_CANDIDATE_DB_PREFIX + myRole.key
}

internal fun fromWhereToReceiveIceCandidates(myRole: ConnectionRole): String {
    return ICE_CANDIDATE_DB_PREFIX + when (myRole == ConnectionRole.Leader) {
        true -> ConnectionRole.Child.key
        else -> ConnectionRole.Leader.key
    }
}

internal fun confirmConnectionOrWait(source: RemoteDevice, timeout: Long) = callbackFlow<Boolean> {
    val timerJob = launch {
        delay(timeout)
        trySend(false)
        close()
    }

    val collectionJob = launch {
        source.isConnected.collect { isConnected ->
            if (isConnected) {
                trySend(true)
                close()
            }
        }
    }

    awaitClose {
        timerJob.cancel()
        collectionJob.cancel()
    }
}