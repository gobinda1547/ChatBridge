package com.gobinda.connection.ext

import com.gobinda.connection.api.RemoteDevice
import com.gobinda.connection.rtc.CustomSdpCreateObserver
import com.gobinda.connection.rtc.CustomSdpSetObserver
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import kotlin.collections.forEach

internal fun RemoteDevice.createOffer() = callbackFlow<String?> {
    peerConnection.createOffer(object : CustomSdpCreateObserver() {
        override fun onSuccess(sdp: SessionDescription) {
            peerConnection.setLocalDescription(object : CustomSdpSetObserver() {
                override fun onSuccess() {
                    trySend(sdp.description)
                    close()
                }

                override fun onFailure() {
                    trySend(null)
                    close()
                }
            }, sdp)
        }

        override fun onFailure() {
            trySend(null)
            close()
        }
    }, MediaConstraints())
    awaitClose()
}

internal fun RemoteDevice.createAnswer() = callbackFlow<String?> {
    peerConnection.createAnswer(object : CustomSdpCreateObserver() {
        override fun onSuccess(sdp: SessionDescription) {
            peerConnection.setLocalDescription(object : CustomSdpSetObserver() {
                override fun onSuccess() {
                    trySend(sdp.description)
                    close()
                }

                override fun onFailure() {
                    trySend(null)
                    close()
                }
            }, sdp)
        }

        override fun onFailure() {
            trySend(null)
            close()
        }
    }, MediaConstraints())
    awaitClose()
}

internal fun RemoteDevice.handleOffer(offerSdp: String) = callbackFlow<Boolean> {
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

internal fun RemoteDevice.handleAnswer(answerSdp: String) = callbackFlow<Boolean> {
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

internal fun RemoteDevice.handleCandidates(candidates: List<IceCandidate>) {
    candidates.forEach { peerConnection.addIceCandidate(it) }
}
