package org.mg.conferenceapp.media

import org.mediasoup.droid.Device
import org.mediasoup.droid.RecvTransport
import org.mg.conferenceapp.Utils
import org.mg.conferenceapp.signaling.RemoteTrack
import org.mg.conferenceapp.signaling.SignalingClient
import org.webrtc.MediaStreamTrack
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoTrack

// Consumes and renders a video stream
class ActiveVideoConsumer(
    signalingClient: SignalingClient,
    mediasoupDevice: Device,
    recvTransport: RecvTransport,
    errorCallback: (String) -> Unit,
    private val view: SurfaceViewRenderer,
    trackId: RemoteTrack
) : ActiveConsumer(signalingClient, mediasoupDevice, recvTransport, errorCallback, trackId) {

    override val TAG = "ActiveVideoConsumer"

    override fun onTrackReady(track: MediaStreamTrack) {
        (track as VideoTrack).addSink(view)
    }

    override fun close() {

        Utils.checkOnMainThread()

        val track = track.get()

        if(track != null) {
            (track as VideoTrack).removeSink(view)
        }

        super.close()
    }
}