package com.gobinda.connection.api

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface RemoteDeviceApi {
    val isConnected: StateFlow<Boolean>
    val dataReceiver: SharedFlow<ByteArray>
    suspend fun sendData(byteArray: ByteArray): Boolean
}