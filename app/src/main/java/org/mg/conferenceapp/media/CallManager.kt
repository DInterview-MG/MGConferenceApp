package org.mg.conferenceapp.media

import android.content.Context
import android.util.Log
import com.google.gson.JsonElement
import org.mediasoup.droid.Device
import org.mediasoup.droid.RecvTransport
import org.mediasoup.droid.Transport
import org.mg.conferenceapp.MGConferenceApp
import org.mg.conferenceapp.SingleThreadVar
import org.mg.conferenceapp.Utils
import org.mg.conferenceapp.signaling.JoinAsNewPeerResponse
import org.mg.conferenceapp.signaling.RemoteTrack
import org.mg.conferenceapp.signaling.SignalingClient
import org.mg.conferenceapp.signaling.SyncResponse
import org.mg.conferenceapp.signaling.TransportDirection
import org.webrtc.SurfaceViewRenderer
import java.util.*

// Main logic for session lifecycle. Created when we want to join the room.
class CallManager(
    private val context: Context,
    hostname: String,
    port: Int,
    val errorCallback: (String) -> Unit,
    val remoteTracksCallback: (List<RemoteTrack>) -> Unit
) : AutoCloseable {

    private val TAG = "CallManager"

    private val running = SingleThreadVar(true)

    private val mediasoupDevice: Device = Device()
    private val mediaCommon = (context.applicationContext as MGConferenceApp).mediaCommon.get()!!

    private val peerId = UUID.randomUUID().toString()

    private val signalingClient: SignalingClient

    private val streamSenderAudio = SingleThreadVar<StreamSenderAudio?>(null)
    private val streamSenderVideo = SingleThreadVar<StreamSenderVideo?>(null)

    private val remoteTracks = SingleThreadVar<List<RemoteTrack>>(emptyList())

    private val recvTransport = SingleThreadVar<RecvTransport?>(null)

    private val activeVideoTrack = SingleThreadVar<ActiveVideoConsumer?>(null)
    private val activeAudioTrack = SingleThreadVar<ActiveAudioConsumer?>(null)

    init {
        Utils.checkOnMainThread()

        Log.i(TAG, "Peer ID is $peerId")

        signalingClient = SignalingClient(peerId, hostname, port)

        signalingClient.joinAsNewPeer(
            { result -> onRoomJoined(result) },
            { errorCallback("Failed to join room") }
        )
    }

    private fun onRoomJoined(
        joinResult: JoinAsNewPeerResponse
    ) {
        Utils.checkOnMainThread()

        sendSync()

        Log.i(TAG, "Joined as peer. Loading mediasoup device with params "
                + Utils.GSON.toJson(joinResult.routerRtpCapabilities))

        mediasoupDevice.load(Utils.GSON.toJson(joinResult.routerRtpCapabilities))

        Log.i(TAG, "Loaded: " + mediasoupDevice.isLoaded)

        createRecvTransport()

        streamSenderAudio.set(StreamSenderAudio(
            context,
            mediaCommon,
            signalingClient,
            mediasoupDevice,
            errorCallback
        ))

        streamSenderVideo.set(StreamSenderVideo(
            context,
            mediaCommon,
            signalingClient,
            mediasoupDevice,
            errorCallback
        ))
    }

    private fun createRecvTransport() {

        Utils.checkOnMainThread()

        signalingClient.createTransport(
            TransportDirection.RECEIVE,
            { result ->

                Utils.checkOnMainThread()

                Log.i(TAG, "Created receive transport: " + Utils.GSON.toJson(result.transportOptions))

                val recvTransportListener = object : RecvTransport.Listener {
                    override fun onConnect(transport: Transport?, dtlsParameters: String?) {
                        Log.i(TAG, "Recv transport: onConnect")
                        signalingClient.connectTransport(
                            result.transportOptions?.id ?: "",
                            Utils.GSON.fromJson(dtlsParameters, JsonElement::class.java),
                            { result -> Log.i(TAG, "Transport connect result: " + result.connected) },
                            { errorCallback("Failed to connect recv transport") }
                        )
                    }

                    override fun onConnectionStateChange(
                        transport: Transport?,
                        connectionState: String?
                    ) {
                        Log.i(TAG, "Recv transport: onConnectionStateChange ($connectionState)")
                    }
                }

                recvTransport.set(mediasoupDevice.createRecvTransport(
                    recvTransportListener,
                    result.transportOptions?.id,
                    Utils.GSON.toJson(result.transportOptions?.iceParameters),
                    Utils.GSON.toJson(result.transportOptions?.iceCandidates),
                    Utils.GSON.toJson(result.transportOptions?.dtlsParameters)
                ))
            },
            {
                errorCallback("Recv transport: Failed to create send transport")
            })
    }

    fun recvVideoTrack(
        trackId: RemoteTrack?,
        view: SurfaceViewRenderer
    ) {
        Utils.checkOnMainThread()

        activeVideoTrack.get()?.close()

        if(trackId != null) {
            activeVideoTrack.set(
                ActiveVideoConsumer(
                    signalingClient,
                    mediasoupDevice,
                    recvTransport.get()!!,
                    errorCallback,
                    view,
                    trackId
                )
            )
        } else {
            activeVideoTrack.set(null)
        }
    }

    fun recvAudioTrack(trackId: RemoteTrack?) {
        Utils.checkOnMainThread()

        activeAudioTrack.get()?.close()

        if(trackId != null) {
            activeAudioTrack.set(
                ActiveAudioConsumer(
                    signalingClient,
                    mediasoupDevice,
                    recvTransport.get()!!,
                    errorCallback,
                    trackId
                )
            )
        } else {
            activeAudioTrack.set(null)
        }
    }

    // While the call is running, will repeatedly sync with the signaling server
    private fun sendSync() {

        Utils.checkOnMainThread()

        signalingClient.sync(
            {response ->

                Utils.checkOnMainThread()

                Log.i(TAG, "Got sync response")
                if(running.get()) {
                    handleSyncUpdate(response)
                    Utils.MAIN_HANDLER.postDelayed({ sendSync() }, 1000)
                }
            },
            {
                errorCallback("Sync failed!")
                Utils.MAIN_HANDLER.postDelayed({ sendSync() }, 1000)
            }
        )
    }

    // Updates remote track list
    private fun handleSyncUpdate(data: SyncResponse) {

        Utils.checkOnMainThread()

        val tracks = ArrayList<RemoteTrack>()

        for((peerName, peerDetails) in data.peers.orEmpty()) {
            for(track in peerDetails.media?.keys.orEmpty()) {
                tracks.add(RemoteTrack(peerName, track, peerId))
            }
        }

        tracks.sort()

        if(remoteTracks.get() != tracks) {

            remoteTracks.set(Collections.unmodifiableList(tracks))

            Log.i(TAG, "Got updated track list: "
                    + tracks.joinToString { name -> name.toString() })

            remoteTracksCallback(remoteTracks.get())
        }
    }

    override fun close() {

        Utils.checkOnMainThread()

        // See readme: this could be called before setup is complete

        Log.i(TAG, "Closing CallManager")

        activeVideoTrack.get()?.close()
        activeAudioTrack.get()?.close()

        streamSenderVideo.get()?.close()
        streamSenderAudio.get()?.close()

        running.set(false)

        signalingClient.leave()
    }
}