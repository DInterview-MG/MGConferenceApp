package org.mg.conferenceapp

import android.app.Application
import android.util.Log
import org.mediasoup.droid.MediasoupClient
import org.mg.conferenceapp.media.MediaCommon
import org.webrtc.PeerConnectionFactory

class MGConferenceApp : Application() {

    private val TAG = "MGConferenceApp"

    val mediaCommon = SingleThreadVar<MediaCommon?>(null)

    override fun onCreate() {
        super.onCreate()

        Log.i(TAG, "Application created")

        // Initialize all the native stuff -- only needs to be done once per process.

        MediasoupClient.initialize(applicationContext)

        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(applicationContext)
                .createInitializationOptions())

        mediaCommon.set(MediaCommon(applicationContext))
    }
}