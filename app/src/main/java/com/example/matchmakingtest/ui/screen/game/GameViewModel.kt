package com.example.matchmakingtest.ui.screen.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.matchmakingtest.app.logI
import com.gobinda.connection.api.ConnReqResult
import com.gobinda.connection.api.RemoteConnector
import com.gobinda.connection.api.RemoteDeviceApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GameViewModel @Inject constructor(
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
            dataReceiverChannel.consumeEach { receivedData ->
                _state.update { it.copy(message = it.message + "\nrec:" + String(receivedData)) }
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

        _state.update { it.copy(connectionState = ConnectionState.Connecting) }

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
                            _state.update { it.copy(connectionState = ConnectionState.NotConnected) }
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
            if (remoteDevice?.sendData(message.toByteArray()) == true) {
                _state.update {
                    it.copy(message = it.message + "\nsent:" + message)
                }
            }
        }
    }


//
//    val roomId = ROOM_ID
//
//    fun handleSendOffer() {
//        viewModelScope.launch(Dispatchers.IO) {
//            signalingManager.listenForAnswer(roomId) { answer ->
//                webRTCManager.handleAnswer(answer)
//            }
//        }
//        viewModelScope.launch(Dispatchers.IO) {
//            signalingManager.listenForCandidates(roomId, "r") { candidate ->
//                webRTCManager.handleCandidate(roomId, candidate)
//            }
//        }
//        viewModelScope.launch(Dispatchers.IO) {
//            webRTCManager.createPeerConnection("s")
//            webRTCManager.createOffer(roomId)
//        }
//    }
//
//    fun waitAndSee() {
//        viewModelScope.launch(Dispatchers.IO) {
//            signalingManager.listenForOffer(roomId) { offer ->
//                webRTCManager.createAnswer(roomId, offer)
//            }
//        }
//        viewModelScope.launch(Dispatchers.IO) {
//            signalingManager.listenForCandidates(roomId, "s") { candidate ->
//                webRTCManager.handleCandidate(roomId, candidate)
//            }
//        }
//        viewModelScope.launch(Dispatchers.IO) {
//            webRTCManager.createPeerConnection("r")
//        }
//    }

}