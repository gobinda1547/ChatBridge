package com.gobinda.connection.rtc

import org.webrtc.DataChannel
import org.webrtc.PeerConnection

internal fun initDataChannelForLeader(
    peerConnection: PeerConnection,
    dataChannelObserver: DataChannel.Observer
): DataChannel {
    val dataChannelInit = DataChannel.Init().apply {
        ordered = true  // Ensure ordered delivery
        maxRetransmits = 10 // Optional: Max retransmissions before giving up
    }
    val dataChannel = peerConnection.createDataChannel("data_channel", dataChannelInit)
    dataChannel.registerObserver(dataChannelObserver)
    return dataChannel
}