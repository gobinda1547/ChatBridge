package com.gobinda.connection.api

sealed class ConnectionRole(val key: String) {
    data object Leader : ConnectionRole("leader")
    data object Child : ConnectionRole("child")
}