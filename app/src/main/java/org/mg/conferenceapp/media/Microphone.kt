package org.mg.conferenceapp.media

import android.content.Context
import android.media.AudioManager
import android.util.Log
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.MediaConstraints

// Wrapper for the device's microphone
class Microphone(
    context: Context,
    mediaCommon: MediaCommon
) : AutoCloseable {

    val TAG = "Microphone"

    private val audioSource: AudioSource
    val audioTrack: AudioTrack

    init {
        (context.getSystemService(Context.AUDIO_SERVICE) as AudioManager).setSpeakerphoneOn(true)

        audioSource = mediaCommon.peerConnectionFactory.createAudioSource(MediaConstraints())

        audioTrack = mediaCommon.peerConnectionFactory.createAudioTrack("mic", audioSource)
    }

    override fun close() {
        Log.i(TAG, "Shutting down microphone")
        audioTrack.dispose()
        audioSource.dispose()
    }
}