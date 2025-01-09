package com.gobinda.connection.signal

import com.gobinda.connection.api.ConnectionRole

private const val ICE_CANDIDATE_DB_PREFIX = "ice_"

fun whereToUploadIceCandidates(myRole: ConnectionRole): String {
    return ICE_CANDIDATE_DB_PREFIX + myRole.key
}

fun fromWhereToReceiveIceCandidates(myRole: ConnectionRole): String {
    return ICE_CANDIDATE_DB_PREFIX + when (myRole == ConnectionRole.Leader) {
        true -> ConnectionRole.Child.key
        else -> ConnectionRole.Leader.key
    }
}