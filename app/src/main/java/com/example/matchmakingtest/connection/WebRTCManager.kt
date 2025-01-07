package com.example.matchmakingtest.connection;

import android.content.Context
import com.example.matchmakingtest.app.logI
import dagger.hilt.android.qualifiers.ApplicationContext
import org.webrtc.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebRTCManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val signalingManager = SignalingManager()
    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    private var localMediaStream: MediaStream? = null


    init {
        // Initialize WebRTC
        initializePeerConnectionFactory()
    }

    private fun initializePeerConnectionFactory() {
        // Initialize PeerConnectionFactory globals
        val options = PeerConnectionFactory.Options()
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(context)
                //.setApplicationContext(context)
                .createInitializationOptions()
        )

        // Create a PeerConnectionFactory instance
        peerConnectionFactory =
            PeerConnectionFactory.builder().setOptions(options).createPeerConnectionFactory()
    }

    // Create PeerConnection and MediaStream
    fun createPeerConnection(): PeerConnection? {
        val rtcConfig = PeerConnection.RTCConfiguration(emptyList()) // Use ICE servers here
        peerConnection = peerConnectionFactory?.createPeerConnection(
            rtcConfig,
            object : PeerConnection.Observer {
                override fun onSignalingChange(state: PeerConnection.SignalingState?) {}
                override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {}
                override fun onIceConnectionReceivingChange(receiving: Boolean) {}
                override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {}

                override fun onIceCandidate(candidate: IceCandidate?) {
                    // Send ICE candidate to remote peer via Firebase
                    candidate?.let { sendCandidateToFirebase(it) }
                }

                override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}
                override fun onAddStream(stream: MediaStream?) {}
                override fun onRemoveStream(stream: MediaStream?) {}
                override fun onDataChannel(channel: DataChannel?) {}
                override fun onRenegotiationNeeded() {}
            })
        return peerConnection
    }

    private fun sendCandidateToFirebase(candidate: IceCandidate) {
        // Send ICE candidate to Firebase Database
    }

    fun createOffer(roomId: String) {
        peerConnection?.createOffer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription?) {
                sdp?.let {
                    // Set the local description
                    peerConnection?.setLocalDescription(object : SdpObserver {
                        override fun onCreateSuccess(p0: SessionDescription?) {}
                        override fun onSetSuccess() {
                            // Send offer to Firebase
                            signalingManager.sendOffer(roomId, it.description)
                        }

                        override fun onSetFailure(p0: String?) {}
                        override fun onCreateFailure(p0: String?) {}
                    }, it)
                }
            }

            override fun onCreateFailure(error: String?) {}
            override fun onSetFailure(error: String?) {}
            override fun onSetSuccess() {}
        }, MediaConstraints())
    }

    fun createAnswer(roomId: String, offerSdp: String) {
        peerConnection?.setRemoteDescription(object : SdpObserver {
            override fun onCreateSuccess(p0: SessionDescription?) {}
            override fun onSetSuccess() {
                // Create answer
                peerConnection?.createAnswer(object : SdpObserver {
                    override fun onCreateSuccess(sdp: SessionDescription?) {
                        sdp?.let {
                            // Set local description
                            peerConnection?.setLocalDescription(object : SdpObserver {
                                override fun onCreateSuccess(p0: SessionDescription?) {}
                                override fun onSetSuccess() {
                                    // Send answer to Firebase
                                    signalingManager.sendAnswer(roomId, it.description)
                                }

                                override fun onSetFailure(p0: String?) {}
                                override fun onCreateFailure(p0: String?) {}
                            }, it)
                        }
                    }

                    override fun onCreateFailure(error: String?) {}
                    override fun onSetFailure(error: String?) {}
                    override fun onSetSuccess() {}
                }, MediaConstraints())
            }

            override fun onSetFailure(error: String?) {}
            override fun onCreateFailure(error: String?) {}
        }, SessionDescription(SessionDescription.Type.OFFER, offerSdp))
    }

    fun handleCandidate(roomId: String, candidate: IceCandidate) {
        peerConnection?.addIceCandidate(candidate)
    }

    fun handleAnswer(answer: String) {
        val remoteAnswer = SessionDescription(SessionDescription.Type.ANSWER, answer)
        peerConnection?.setRemoteDescription(object : SdpObserver {
            override fun onSetSuccess() {
                logI("Remote answer set successfully.")
            }

            override fun onSetFailure(error: String?) {
                logI("Failed to set remote answer: $error")
            }

            override fun onCreateSuccess(sdp: SessionDescription?) {}
            override fun onCreateFailure(error: String?) {}
        }, remoteAnswer)
    }
}
