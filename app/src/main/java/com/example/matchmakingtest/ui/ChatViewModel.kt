package com.example.matchmakingtest.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.matchmakingtest.app.logI
import com.example.matchmakingtest.ui.models.ConnectionState
import com.example.matchmakingtest.ui.models.GameScreenState
import com.example.matchmakingtest.ui.models.MessageSentOrReceived
import com.example.matchmakingtest.ui.models.SingleMessage
import com.gobinda.connection.api.ConnReqResult
import com.gobinda.connection.api.RemoteConnector
import com.gobinda.connection.api.RemoteDeviceApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val remoteConnector: RemoteConnector
) : ViewModel() {

    private val _state = MutableStateFlow(GameScreenState())
    val state = _state.asStateFlow()

    private var remoteDevice: RemoteDeviceApi? = null

    private val dataReceiverChannel = Channel<ByteArray>(capacity = Channel.UNLIMITED)

    private var dataReceiverJob: Job? = null
    private var connectionStateHandlerJob: Job? = null

    init {
        viewModelScope.launch(Dispatchers.IO) {
            dataReceiverChannel.consumeEach { recData ->
                val now = SingleMessage(String(recData), MessageSentOrReceived.Received)
                _state.update { it.copy(messages = it.messages + now) }
            }
        }
    }

    fun tryToConnect() {
        if (state.value.connectionState != ConnectionState.NotConnected) {
            return
        }

        dataReceiverJob?.cancel()
        dataReceiverJob = null

        connectionStateHandlerJob?.cancel()
        connectionStateHandlerJob = null

        _state.update {
            it.copy(
                connectionState = ConnectionState.Connecting,
                messages = emptyList()
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            val connectionStatus = remoteConnector.connect()
            logI("connection status received: $connectionStatus")
            if (connectionStatus is ConnReqResult.Successful) {
                remoteDevice = connectionStatus.remoteDevice
                dataReceiverJob = CoroutineScope(Dispatchers.IO).launch(Dispatchers.IO) {
                    connectionStatus.remoteDevice.dataReceiver.collect {
                        dataReceiverChannel.trySend(it)
                    }
                }
                connectionStateHandlerJob = CoroutineScope(Dispatchers.IO).launch(Dispatchers.IO) {
                    connectionStatus.remoteDevice.isConnected.collect { isConnected ->
                        if (isConnected.not()) {
                            _state.update {
                                it.copy(
                                    connectionState = ConnectionState.NotConnected,
                                    messages = emptyList()
                                )
                            }
                            remoteDevice = null
                            dataReceiverJob?.cancel()
                            connectionStateHandlerJob?.cancel()
                        }
                    }
                }
                _state.update { it.copy(connectionState = ConnectionState.Connected) }
                return@launch
            }
            _state.update { it.copy(connectionState = ConnectionState.NotConnected) }
        }
    }

    fun sendMessage(message: String) {
        if (message.trim().isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            val sendingStatus = when (remoteDevice?.sendData(message.toByteArray())) {
                true -> MessageSentOrReceived.Sent
                else -> MessageSentOrReceived.SendingFailed
            }
            val now = SingleMessage(message, sendingStatus)
            _state.update { it.copy(messages = it.messages + now) }
        }
    }
}