package com.example.matchmakingtest.connection

import com.example.matchmakingtest.app.logI
import org.webrtc.DataChannel
import org.webrtc.IceCandidate
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory

class WebPeerConnectionCreator(private val mediator: ConnectionMediator) {

    private val peerConnectionFactory: PeerConnectionFactory

    init {
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(mediator.context)
                .createInitializationOptions()
        )
        val options = PeerConnectionFactory.Options()
        peerConnectionFactory = PeerConnectionFactory.builder().setOptions(options)
            .createPeerConnectionFactory()
    }

    fun createPeerConnection(userId: String): PeerConnection? {
        val iceServers = listOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
        )
        val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
        }
        val peerConnection = peerConnectionFactory.createPeerConnection(
            rtcConfig,
            object : PeerConnection.Observer {
                override fun onSignalingChange(state: PeerConnection.SignalingState?) {
                    logI("on signaling changed $state")
                }

                override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
                    logI("on ice connection change invoked $state")
                }

                override fun onIceConnectionReceivingChange(receiving: Boolean) {
                    logI("on ice connection receiving change $receiving")
                }

                override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {
                    logI("on ice gathering change $p0")
                }

                override fun onIceCandidate(candidate: IceCandidate?) {
                    // Send ICE candidate to remote peer via Firebase
                    logI("on ice candidate callback")
                    //candidate?.let { sendCandidateToFirebase(it, role) }
                }

                override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {
                    logI("on ice candidate removed callback")
                }

                override fun onAddStream(stream: MediaStream?) {
                    logI("on add stream callback")
                }

                override fun onRemoveStream(stream: MediaStream?) {
                    logI("on remove stream callback")
                }

                override fun onDataChannel(channel: DataChannel?) {
                    logI("on data channel invoked")
                }

                override fun onRenegotiationNeeded() {
                    logI("on renegotiation needed")
                }
            })
        //createDataChannel()
        return peerConnection
    }
}