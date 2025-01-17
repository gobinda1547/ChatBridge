package com.gobinda.connection.rtc

import android.content.Context
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory

private fun getPeerConnectionFactory(context: Context): PeerConnectionFactory {
    val options = PeerConnectionFactory.Options()
    PeerConnectionFactory.initialize(
        PeerConnectionFactory.InitializationOptions.builder(context)
            .createInitializationOptions()
    )
    return PeerConnectionFactory.builder().setOptions(options).createPeerConnectionFactory()
}

internal fun initPeerConnection(
    context: Context,
    observer: CustomPeerConnectionObserver
): PeerConnection {
    val iceServers = listOf(
        PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
    )
    val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
        sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
    }

    val factory = getPeerConnectionFactory(context = context)
    val peerConnection = factory.createPeerConnection(rtcConfig, observer)
    return peerConnection ?: throw Exception("Peer connection creation failed")
}