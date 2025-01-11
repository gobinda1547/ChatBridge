package com.gobinda.connection.rtc

import org.webrtc.SdpObserver
import org.webrtc.SessionDescription

abstract class CustomSdpCreateObserver : SdpObserver {

    abstract fun onSuccess(sdp: SessionDescription)
    abstract fun onFailure()

    override fun onCreateSuccess(p0: SessionDescription?) {
        p0?.let { onSuccess(it) } ?: onFailure()
    }

    override fun onSetSuccess() {

    }

    override fun onCreateFailure(p0: String?) {
        onFailure()
    }

    override fun onSetFailure(p0: String?) {

    }
}