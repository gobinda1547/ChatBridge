package com.gobinda.connection.internal

internal sealed class ConnectionRole(val key: String) {
    data object Leader : ConnectionRole("leader")
    data object Child : ConnectionRole("child")
}