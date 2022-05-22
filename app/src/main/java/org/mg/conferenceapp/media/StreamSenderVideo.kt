package org.mg.conferenceapp.media

import android.content.Context
import org.mediasoup.droid.Device
import org.mg.conferenceapp.Constants
import org.mg.conferenceapp.SingleThreadVar
import org.mg.conferenceapp.Utils
import org.mg.conferenceapp.signaling.SignalingClient
import org.webrtc.MediaStreamTrack

class StreamSenderVideo(
    private val context: Context,
    private val mediaCommon: MediaCommon,
    signalingClient: SignalingClient,
    mediasoupDevice: Device,
    errorCallback: (String) -> Unit
): StreamSender(
    Constants.CAM_VIDEO_MEDIA_TAG, signalingClient, mediasoupDevice, errorCallback
) {
    var camera = SingleThreadVar<Camera?>(null)

    override fun internalCreateTrack(): MediaStreamTrack {
        Utils.checkOnMainThread()

        if(camera.get() == null) {
            camera.set(Camera(context, mediaCommon))
        }

        return camera.get()!!.videoTrack
    }

    override fun close() {
        camera.get()?.close()
        super.close()
    }
}