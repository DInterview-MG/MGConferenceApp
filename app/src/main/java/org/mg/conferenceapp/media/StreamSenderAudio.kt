package org.mg.conferenceapp.media

import android.content.Context
import org.mediasoup.droid.Device
import org.mg.conferenceapp.Constants
import org.mg.conferenceapp.SingleThreadVar
import org.mg.conferenceapp.Utils
import org.mg.conferenceapp.signaling.SignalingClient
import org.webrtc.MediaStreamTrack

class StreamSenderAudio(
    private val context: Context,
    private val mediaCommon: MediaCommon,
    signalingClient: SignalingClient,
    mediasoupDevice: Device,
    errorCallback: (String) -> Unit
): StreamSender(
    Constants.CAM_AUDIO_MEDIA_TAG, signalingClient, mediasoupDevice, errorCallback
) {
    var mic = SingleThreadVar<Microphone?>(null)

    override fun internalCreateTrack(): MediaStreamTrack {
        Utils.checkOnMainThread()

        if(mic.get() == null) {
            mic.set(Microphone(context, mediaCommon))
        }

        return mic.get()!!.audioTrack
    }

    override fun close() {
        mic.get()?.close()
        super.close()
    }
}