package com.gobinda.connection.api

import android.content.Context
import com.gobinda.connection.internal.li
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import org.webrtc.DataChannel
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import java.nio.ByteBuffer

internal class RemoteDevice(private val context: Context) : RemoteDeviceApi {

    private val _dataReceiver = MutableSharedFlow<ByteArray>(extraBufferCapacity = Int.MAX_VALUE)
    override val dataReceiver: SharedFlow<ByteArray> = _dataReceiver.asSharedFlow()

    private var peerConnection: PeerConnection? = null
    private var dataChannel: DataChannel? = null

    private val _iceCandidates = MutableStateFlow<List<IceCandidate>>(emptyList())
    internal val iceCandidates = _iceCandidates.asStateFlow()

    private val _channelState = MutableStateFlow(false)
    private val _iceConnState = MutableStateFlow<PeerConnection.IceConnectionState?>(null)

    override val isConnected: StateFlow<Boolean> =
        combine(_channelState, _iceConnState) { channelState, iceConnState ->
            val con1 = channelState == true
            val con21 = iceConnState == PeerConnection.IceConnectionState.CONNECTED
            val con22 = iceConnState == PeerConnection.IceConnectionState.COMPLETED
            con1 && (con21 || con22)
        }.stateIn(
            scope = CoroutineScope(Dispatchers.Default), // Provide a coroutine scope
            started = SharingStarted.WhileSubscribed(5000), // Active while there are active subscribers
            initialValue = false // Initial value
        )

    private val connectionObserver = object : PeerConnection.Observer {
        override fun onSignalingChange(state: PeerConnection.SignalingState?) {
            li("on signaling changed $state")
        }

        override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
            li("on ice connection change invoked $state")
            _iceConnState.tryEmit(state)
        }

        override fun onIceConnectionReceivingChange(receiving: Boolean) {
            li("on ice connection receiving change $receiving")
        }

        override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {
            li("on ice gathering change $p0")
        }

        override fun onIceCandidate(candidate: IceCandidate?) {
            candidate?.let { current ->
                li("on ice candidate")
                _iceCandidates.update { it + current }
            }
        }

        override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {
            li("on ice candidate removed callback")
        }

        override fun onAddStream(stream: MediaStream?) {
            li("on add stream callback")
        }

        override fun onRemoveStream(stream: MediaStream?) {
            li("on remove stream callback")
        }

        override fun onDataChannel(channel: DataChannel?) {
            li("on data channel invoked")
        }

        override fun onRenegotiationNeeded() {
            li("on renegotiation needed")
        }
    }

    private val dataChannelObserver = object : DataChannel.Observer {
        override fun onBufferedAmountChange(previousAmount: Long) {
            li("Buffered amount changed: $previousAmount")
        }

        override fun onStateChange() {
            li("Data channel state changed ${dataChannel?.state()}")
            _channelState.tryEmit(dataChannel?.state() == DataChannel.State.OPEN)
        }

        override fun onMessage(buffer: DataChannel.Buffer?) {
            li("onMessage: $buffer")
            val byteBuffer = buffer?.data ?: return
            val bytes = ByteArray(byteBuffer.remaining())
            byteBuffer.get(bytes)
            _dataReceiver.tryEmit(bytes)
        }
    }

    fun initialize(): RemoteDevice {
        val iceServers = listOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
        )
        val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
        }
        val dataChannelInit = DataChannel.Init().apply {
            ordered = true  // Ensure ordered delivery
            maxRetransmits = 10 // Optional: Max retransmissions before giving up
        }

        val factory = getPeerConnectionFactory(context = context)
        peerConnection = factory.createPeerConnection(rtcConfig, connectionObserver)
        dataChannel = peerConnection?.createDataChannel("data_channel", dataChannelInit)
        dataChannel?.registerObserver(dataChannelObserver)
        return this
    }

    fun resetIceCandidates() {
        _iceCandidates.update { emptyList() }
    }

    private fun getPeerConnectionFactory(context: Context): PeerConnectionFactory {
        val options = PeerConnectionFactory.Options()
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(context)
                .createInitializationOptions()
        )
        return PeerConnectionFactory.builder().setOptions(options).createPeerConnectionFactory()
    }

    fun createOffer() = callbackFlow<String?> {
        val currentConnection: PeerConnection = peerConnection ?: let {
            trySend(null)
            close()
            return@callbackFlow
        }
        currentConnection.createOffer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription) {
                currentConnection.setLocalDescription(object : SdpObserver {
                    override fun onCreateSuccess(p0: SessionDescription?) {}
                    override fun onCreateFailure(p0: String?) {}
                    override fun onSetSuccess() {
                        trySend(sdp.description)
                        close()
                    }

                    override fun onSetFailure(p0: String?) {
                        trySend(null)
                        close()
                    }
                }, sdp)
            }

            override fun onCreateFailure(error: String?) {
                trySend(null)
                close()
            }

            override fun onSetFailure(error: String?) {}
            override fun onSetSuccess() {}
        }, MediaConstraints())
        awaitClose()
    }

    fun createAnswer() = callbackFlow<String?> {
        val currentConnection: PeerConnection = peerConnection ?: let {
            trySend(null)
            close()
            return@callbackFlow
        }
        currentConnection.createAnswer(object : SdpObserver {
            override fun onSetFailure(error: String?) {}
            override fun onSetSuccess() {}
            override fun onCreateSuccess(sdp: SessionDescription) {
                currentConnection.setLocalDescription(object : SdpObserver {
                    override fun onCreateSuccess(p0: SessionDescription?) {}
                    override fun onCreateFailure(p0: String?) {}
                    override fun onSetSuccess() {
                        trySend(sdp.description)
                        close()
                    }

                    override fun onSetFailure(p0: String?) {
                        trySend(null)
                        close()
                    }
                }, sdp)
            }

            override fun onCreateFailure(error: String?) {
                trySend(null)
                close()
            }
        }, MediaConstraints())
        awaitClose()
    }

    fun handleOffer(offerSdp: String) = callbackFlow<Boolean> {
        val currentConnection: PeerConnection = peerConnection ?: let {
            trySend(false)
            close()
            return@callbackFlow
        }
        currentConnection.setRemoteDescription(object : SdpObserver {
            override fun onCreateSuccess(p0: SessionDescription?) {}
            override fun onCreateFailure(error: String?) {}
            override fun onSetSuccess() {
                trySend(true)
                close()
            }

            override fun onSetFailure(error: String?) {
                trySend(false)
                close()
            }
        }, SessionDescription(SessionDescription.Type.OFFER, offerSdp))
        awaitClose()
    }

    fun handleAnswer(answerSdp: String) = callbackFlow<Boolean> {
        val currentConnection: PeerConnection = peerConnection ?: let {
            trySend(false)
            close()
            return@callbackFlow
        }
        currentConnection.setRemoteDescription(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription?) {}
            override fun onCreateFailure(error: String?) {}
            override fun onSetSuccess() {
                trySend(true)
                close()
            }

            override fun onSetFailure(error: String?) {
                trySend(false)
                close()
            }
        }, SessionDescription(SessionDescription.Type.ANSWER, answerSdp))
        awaitClose()
    }

    fun handleCandidates(candidates: List<IceCandidate>) {
        candidates.forEach { peerConnection?.addIceCandidate(it) }
    }

    override suspend fun sendData(byteArray: ByteArray): Boolean {
        li("send data channel state : ${dataChannel?.state()}")
        return withContext(Dispatchers.IO) {
            val buffer = DataChannel.Buffer(ByteBuffer.wrap(byteArray), false)
            dataChannel?.send(buffer) == true
        }
    }
}