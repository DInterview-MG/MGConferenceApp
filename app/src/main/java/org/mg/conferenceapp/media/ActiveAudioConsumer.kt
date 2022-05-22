package org.mg.conferenceapp.media

import org.mediasoup.droid.Device
import org.mediasoup.droid.RecvTransport
import org.mg.conferenceapp.signaling.RemoteTrack
import org.mg.conferenceapp.signaling.SignalingClient
import org.webrtc.MediaStreamTrack

// Consumes and renders an audio stream
class ActiveAudioConsumer(
    signalingClient: SignalingClient,
    mediasoupDevice: Device,
    recvTransport: RecvTransport,
    errorCallback: (String) -> Unit,
    trackId: RemoteTrack
) : ActiveConsumer(signalingClient, mediasoupDevice, recvTransport, errorCallback, trackId) {

    override val TAG = "ActiveAudioConsumer"

    override fun onTrackReady(track: MediaStreamTrack) {
        // Nothing to do here
    }
}