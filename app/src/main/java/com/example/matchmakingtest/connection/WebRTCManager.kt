package com.example.matchmakingtest.connection;

import android.content.Context
import com.example.matchmakingtest.app.ROOM_ID
import com.example.matchmakingtest.app.logI
import dagger.hilt.android.qualifiers.ApplicationContext
import org.webrtc.*
import java.nio.ByteBuffer
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
    fun createPeerConnection(role: String): PeerConnection? {
        val iceServers = listOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
        )
        val rtcConfig = PeerConnection.RTCConfiguration(iceServers)
            .apply {
                sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            }
        peerConnection = peerConnectionFactory?.createPeerConnection(
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
                    //logI("on ice candidate callback")
                    candidate?.let { sendCandidateToFirebase(it, role) }
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
        createDataChannel()
        return peerConnection
    }

    private fun sendCandidateToFirebase(candidate: IceCandidate, role: String) {
        //logI("sending candidate to firebase")
        signalingManager.sendIceCandidate(ROOM_ID, role, candidate)
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
                            logI("create offer success - sending offer now")
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
                                    logI("create answer success - sending answer now")
                                    logI("Local SDP: ${peerConnection?.localDescription}")
                                    logI("Remote SDP: ${peerConnection?.remoteDescription}")
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
        //logI("handle candidate invoked")
        peerConnection?.addIceCandidate(candidate)
    }

    fun handleAnswer(answer: String) {
        val remoteAnswer = SessionDescription(SessionDescription.Type.ANSWER, answer)
        peerConnection?.setRemoteDescription(object : SdpObserver {
            override fun onSetSuccess() {
                logI("Remote answer set successfully.")
                logI("Local SDP: ${peerConnection?.localDescription}")
                logI("Remote SDP: ${peerConnection?.remoteDescription}")
            }

            override fun onSetFailure(error: String?) {
                logI("Failed to set remote answer: $error")
            }

            override fun onCreateSuccess(sdp: SessionDescription?) {}
            override fun onCreateFailure(error: String?) {}
        }, remoteAnswer)
    }


    fun createDataChannel() {
        val dataChannelInit = DataChannel.Init().apply {
            // Set options for the data channel
            ordered = true  // Ensure ordered delivery
            maxRetransmits = 10 // Optional: Max retransmissions before giving up
        }

        // Create the data channel
        val dataChannel = peerConnection?.createDataChannel("myDataChannel", dataChannelInit)

        // Set up the data channel observer
        dataChannel?.registerObserver()
    }

    // Helper function to register a DataChannel.Observer
    fun DataChannel.registerObserver() {
        this.registerObserver(object : DataChannel.Observer {
            override fun onBufferedAmountChange(previousAmount: Long) {
                logI("Buffered amount changed: $previousAmount")
            }

            override fun onStateChange() {
                logI("Data channel state changed: ${this@registerObserver.state()}")
            }

            override fun onMessage(buffer: DataChannel.Buffer?) {
                buffer?.let {
                    val data = String(it.data.array())
                    logI("Received message: $data")
                }
            }
        })
    }

    // Example of sending data
    fun sendMessage(dataChannel: DataChannel?, message: String) {
        val buffer = DataChannel.Buffer(
            ByteBuffer.wrap(message.toByteArray()),
            false  // Indicates binary data (false = text)
        )
        dataChannel?.send(buffer)
    }
}
