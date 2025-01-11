package com.gobinda.connection.api

import android.content.Context
import com.gobinda.connection.internal.ConnectionRole
import com.gobinda.connection.internal.li
import com.gobinda.connection.rtc.CustomPeerConnectionObserver
import com.gobinda.connection.rtc.initDataChannelForLeader
import com.gobinda.connection.rtc.initPeerConnection
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
import org.webrtc.PeerConnection
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import java.nio.ByteBuffer

internal class RemoteDevice(
    private val context: Context,
    private val myRole: ConnectionRole
) : RemoteDeviceApi {

    private val connectionObserver = object : CustomPeerConnectionObserver() {
        override fun whenIceConnectionChanged(state: PeerConnection.IceConnectionState) {
            li("on ice connection change invoked $state")
            _iceConnState.tryEmit(state)
        }

        override fun whenIceCandidateFound(candidate: IceCandidate) {
            li("on ice candidate")
            _iceCandidates.update { it + candidate }
        }

        override fun whenDataChannelFound(channel: DataChannel) {
            li("on data channel invoked")
            if (myRole == ConnectionRole.Child) {
                dataChannel = channel
                dataChannel?.registerObserver(dataChannelObserver)
            }
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

    private val peerConnection = initPeerConnection(context, connectionObserver)
    private var dataChannel: DataChannel? = when (myRole) {
        ConnectionRole.Leader -> initDataChannelForLeader(peerConnection, dataChannelObserver)
        else -> null
    }

    private val _dataReceiver = MutableSharedFlow<ByteArray>(extraBufferCapacity = Int.MAX_VALUE)
    override val dataReceiver: SharedFlow<ByteArray> = _dataReceiver.asSharedFlow()

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



    fun createOffer() = callbackFlow<String?> {
        peerConnection.createOffer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription) {
                peerConnection.setLocalDescription(object : SdpObserver {
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
        peerConnection.createAnswer(object : SdpObserver {
            override fun onSetFailure(error: String?) {}
            override fun onSetSuccess() {}
            override fun onCreateSuccess(sdp: SessionDescription) {
                peerConnection.setLocalDescription(object : SdpObserver {
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
        peerConnection.setRemoteDescription(object : SdpObserver {
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
        peerConnection.setRemoteDescription(object : SdpObserver {
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
        candidates.forEach { peerConnection.addIceCandidate(it) }
    }

    override suspend fun sendData(byteArray: ByteArray): Boolean {
        li("send data channel state : ${dataChannel?.state()}")
        return withContext(Dispatchers.IO) {
            val buffer = DataChannel.Buffer(ByteBuffer.wrap(byteArray), false)
            dataChannel?.send(buffer) == true
        }
    }
}