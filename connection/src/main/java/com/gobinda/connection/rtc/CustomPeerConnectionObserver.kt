package com.gobinda.connection.rtc

import org.webrtc.DataChannel
import org.webrtc.IceCandidate
import org.webrtc.MediaStream
import org.webrtc.PeerConnection

internal abstract class CustomPeerConnectionObserver : PeerConnection.Observer {

    abstract fun whenIceConnectionChanged(state: PeerConnection.IceConnectionState)
    abstract fun whenIceCandidateFound(candidate: IceCandidate)
    abstract fun whenDataChannelFound(channel: DataChannel)

    override fun onSignalingChange(p0: PeerConnection.SignalingState?) {

    }

    override fun onIceConnectionChange(p0: PeerConnection.IceConnectionState?) {
        p0?.let { whenIceConnectionChanged(it) }
    }

    override fun onIceConnectionReceivingChange(p0: Boolean) {

    }

    override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {

    }

    override fun onIceCandidate(p0: IceCandidate?) {
        p0?.let { whenIceCandidateFound(it) }
    }

    override fun onIceCandidatesRemoved(p0: Array<out IceCandidate?>?) {

    }

    override fun onAddStream(p0: MediaStream?) {

    }

    override fun onRemoveStream(p0: MediaStream?) {

    }

    override fun onDataChannel(p0: DataChannel?) {
        p0?.let { whenDataChannelFound(it) }
    }

    override fun onRenegotiationNeeded() {

    }
}