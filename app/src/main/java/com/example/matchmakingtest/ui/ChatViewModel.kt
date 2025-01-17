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
            onConnectionRequestResult(connectionStatus)
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

    private fun onConnectionRequestResult(connectionStatus: ConnReqResult) {

        if (connectionStatus !is ConnReqResult.Successful) {
            // since connection request failed, so we will update the state and
            // just return, we can also send a toast message here but not necessary
            _state.update { it.copy(connectionState = ConnectionState.NotConnected) }
            return
        }

        // since above condition is false that means we are connected now
        // so we have to set data receiver and connection state change receiver
        // we also have to store the remote device reference for sending data
        remoteDevice = connectionStatus.remoteDevice

        // setting up the data receiver
        dataReceiverJob = CoroutineScope(Dispatchers.IO).launch(Dispatchers.IO) {
            connectionStatus.remoteDevice.dataReceiver.collect {
                dataReceiverChannel.trySend(it)
            }
        }

        // setting up the connection state receiver
        connectionStateHandlerJob = CoroutineScope(Dispatchers.IO).launch(Dispatchers.IO) {
            connectionStatus.remoteDevice.isConnected.collect { isConnected ->
                if (isConnected.not()) {
                    handleOnDisconnected()
                }
            }
        }

        // finally we can update the state to let the ui layer know that we are connected
        _state.update { it.copy(connectionState = ConnectionState.Connected) }
    }

    private fun handleOnDisconnected() {
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