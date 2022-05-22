package org.mg.conferenceapp.media

import android.content.Context
import org.mg.conferenceapp.Utils
import org.webrtc.PeerConnectionFactory
import org.webrtc.audio.JavaAudioDeviceModule

class MediaCommon(context: Context) {

    val audioDeviceModule = JavaAudioDeviceModule.builder(context).createAudioDeviceModule();

    val peerConnectionFactory = PeerConnectionFactory.builder()
            .setAudioDeviceModule(audioDeviceModule)
            .createPeerConnectionFactory()

    init {
        Utils.checkOnMainThread()
    }
}