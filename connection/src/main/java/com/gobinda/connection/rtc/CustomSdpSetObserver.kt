package com.gobinda.connection.rtc

import org.webrtc.SdpObserver
import org.webrtc.SessionDescription

abstract class CustomSdpSetObserver : SdpObserver {

    abstract fun onSuccess()
    abstract fun onFailure()

    override fun onCreateSuccess(p0: SessionDescription?) {

    }

    override fun onSetSuccess() {
        onSuccess()
    }

    override fun onCreateFailure(p0: String?) {

    }

    override fun onSetFailure(p0: String?) {
        onFailure()
    }
}