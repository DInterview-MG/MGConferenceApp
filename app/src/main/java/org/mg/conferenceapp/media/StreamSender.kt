package org.mg.conferenceapp.media

import android.util.Log
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import org.mediasoup.droid.Device
import org.mediasoup.droid.Producer
import org.mediasoup.droid.SendTransport
import org.mediasoup.droid.Transport
import org.mg.conferenceapp.Utils
import org.mg.conferenceapp.signaling.SignalingClient
import org.mg.conferenceapp.signaling.TransportDirection
import org.webrtc.MediaStreamTrack
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

abstract class StreamSender(
    mediaTag: String,
    signalingClient: SignalingClient,
    mediasoupDevice: Device,
    errorCallback: (String) -> Unit
) : AutoCloseable {

    open val TAG = "StreamSender"

    private val sendTransport = AtomicReference<SendTransport>()
    private val producer = AtomicReference<Producer>()

    abstract fun internalCreateTrack(): MediaStreamTrack

    init {
        Utils.checkOnMainThread()
        signalingClient.createTransport(
            TransportDirection.SEND,
            { result ->

                Log.i(TAG, "Created send transport: " + Utils.GSON.toJson(result.transportOptions))

                val sendTransportListener = object : SendTransport.Listener {
                    override fun onConnect(transport: Transport?, dtlsParameters: String?) {
                        Log.i(TAG, "Stream $mediaTag: onConnect")
                        signalingClient.connectTransport(
                            result.transportOptions?.id ?: "",
                            Utils.GSON.fromJson(dtlsParameters, JsonElement::class.java),
                            { result -> Log.i(TAG, "Transport connect result: " + result.connected) },
                            { errorCallback("Failed to connect transport for $mediaTag") }
                        )
                    }

                    override fun onConnectionStateChange(
                        transport: Transport?,
                        connectionState: String?
                    ) {
                        Log.i(TAG, "Stream $mediaTag: onConnectionStateChange ($connectionState)")
                    }

                    override fun onProduce(
                        transport: Transport?,
                        kind: String?,
                        rtpParameters: String?,
                        appData: String?
                    ): String {
                        Log.i(TAG, "Stream $mediaTag: onProduce ($appData)")

                        val appDataObj = Utils.GSON.fromJson(
                            appData,
                            JsonObject::class.java) ?: JsonObject()

                        appDataObj.add("mediaTag", JsonPrimitive(mediaTag))

                        val sendTrackResult = signalingClient.sendTrack(
                            transport?.id,
                            appDataObj,
                            kind,
                            false,
                            Utils.GSON.fromJson(rtpParameters, JsonElement::class.java)
                        )

                        return sendTrackResult?.id!!
                    }
                }

                sendTransport.set(mediasoupDevice.createSendTransport(
                    sendTransportListener,
                    result.transportOptions?.id,
                    Utils.GSON.toJson(result.transportOptions?.iceParameters),
                    Utils.GSON.toJson(result.transportOptions?.iceCandidates),
                    Utils.GSON.toJson(result.transportOptions?.dtlsParameters)
                ))

                val producerListener = object : Producer.Listener {
                    override fun onTransportClose(producer: Producer?) {
                        Log.i(TAG, "Stream $mediaTag: onTransportClose")
                    }
                }

                // The subclass is responsible for cleanup
                val track = internalCreateTrack()

                thread {
                    producer.set(sendTransport.get().produce(
                        producerListener,
                        track,
                        null,
                        null
                    ))
                }
            },
            {
                errorCallback("Stream $mediaTag: Failed to create send transport")
            })
    }

    override fun close() {
        // See readme: Currently this could run before the setup has finished
        producer.get()?.close()
        sendTransport.get()?.close()
    }
}