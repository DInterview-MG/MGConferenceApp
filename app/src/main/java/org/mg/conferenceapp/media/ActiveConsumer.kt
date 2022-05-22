package org.mg.conferenceapp.media

import android.util.Log
import com.google.gson.JsonElement
import org.mediasoup.droid.Consumer
import org.mediasoup.droid.Device
import org.mediasoup.droid.RecvTransport
import org.mg.conferenceapp.SingleThreadVar
import org.mg.conferenceapp.Utils
import org.mg.conferenceapp.signaling.RemoteTrack
import org.mg.conferenceapp.signaling.SignalingClient
import org.webrtc.MediaStreamTrack

// Base logic for ActiveVideoConsumer and ActiveAudioConsumer
abstract class ActiveConsumer(
    private val signalingClient: SignalingClient,
    mediasoupDevice: Device,
    recvTransport: RecvTransport,
    errorCallback: (String) -> Unit,
    trackId: RemoteTrack
) : AutoCloseable {

    open val TAG = "ActiveConsumer"

    val track = SingleThreadVar<MediaStreamTrack?>(null)
    val consumer = SingleThreadVar<Consumer?>(null)

    init {
        Utils.checkOnMainThread()

        signalingClient.recvTrack(
            trackId.peerName,
            trackId.trackName,
            Utils.GSON.fromJson(mediasoupDevice.rtpCapabilities, JsonElement::class.java),
            { recvTrackResult ->

                Utils.checkOnMainThread()

                Log.i(TAG, "recvTrack success: " + Utils.GSON.toJson(recvTrackResult))

                val consumerListener = object : Consumer.Listener {
                    override fun onTransportClose(consumer: Consumer?) {
                        Log.i(TAG, "Consumer listener: onTransportClose");
                    }
                }

                val consumer = recvTransport.consume(
                    consumerListener,
                    recvTrackResult.id,
                    recvTrackResult.producerId,
                    recvTrackResult.kind,
                    Utils.GSON.toJson(recvTrackResult.rtpParameters)
                )

                signalingClient.resumeConsumer(
                    recvTrackResult.id!!,
                    { result -> Log.i(TAG, "Consumer resumed: " + result.resumed) },
                    {}
                )

                this.track.set(consumer.track)
                this.consumer.set(consumer)

                onTrackReady(consumer.track)
            },
            { errorCallback("recv-track request failed") }
        )
    }

    abstract fun onTrackReady(track: MediaStreamTrack);

    override fun close() {

        Utils.checkOnMainThread()

        // See readme: this may run before the setup is complete

        track.get()?.dispose()

        val consumer = consumer.get()

        if(consumer != null) {
            signalingClient.closeConsumer(consumer.id)
            consumer.close()
        }
    }
}