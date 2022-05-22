package org.mg.conferenceapp.signaling

import android.annotation.SuppressLint
import android.util.Log
import com.google.gson.JsonElement
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.mg.conferenceapp.Utils
import java.io.IOException
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.*
import java.util.concurrent.atomic.AtomicLong
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSession
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import kotlin.reflect.KClass

class SignalingClient(
    private val peerId: String,
    private val hostname: String,
    private val port: Int) {

    private var mClient = buildClient()

    private val requestIdGenerator = AtomicLong(1)

    private enum class IsRequestBlocking {
        Blocking,
        Nonblocking
    }

    fun joinAsNewPeer(
        successCallback: (JoinAsNewPeerResponse) -> Unit,
        failureCallback: () -> Unit
    ) {
        makeRequest(
            "join-as-new-peer",
            BaseRequest(peerId),
            JoinAsNewPeerResponse::class,
            IsRequestBlocking.Nonblocking,
            successCallback,
            failureCallback
        )
    }

    fun sync(
        successCallback: (SyncResponse) -> Unit,
        failureCallback: () -> Unit
    ) {
        makeRequest(
            "sync",
            BaseRequest(peerId),
            SyncResponse::class,
            IsRequestBlocking.Nonblocking,
            successCallback,
            failureCallback
        )
    }

    fun createTransport(
        direction: TransportDirection,
        successCallback: (CreateTransportResponse) -> Unit,
        failureCallback: () -> Unit
    ) {
        makeRequest(
            "create-transport",
            CreateTransportRequest(peerId, direction.protocolName),
            CreateTransportResponse::class,
            IsRequestBlocking.Nonblocking,
            successCallback,
            failureCallback
        )
    }

    fun connectTransport(
        transportId: String,
        dtlsParameters: JsonElement?,
        successCallback: (ConnectTransportResponse) -> Unit,
        failureCallback: () -> Unit
    ) {
        makeRequest(
            "connect-transport",
            ConnectTransportRequest(peerId, transportId, dtlsParameters),
            ConnectTransportResponse::class,
            IsRequestBlocking.Nonblocking,
            successCallback,
            failureCallback
        )
    }

    fun sendTrack(
        transportId: String?,
        appData: JsonElement?,
        kind: String?,
        paused: Boolean,
        rtpParameters: JsonElement?,
    ): SendTrackResponse? {

        var sendTrackResult: SendTrackResponse? = null

        makeRequest(
            "send-track",
            SendTrackRequest(peerId, transportId, appData, kind, paused, rtpParameters),
            SendTrackResponse::class,
            IsRequestBlocking.Blocking,
            { result -> sendTrackResult = result },
            {}
        )

        return sendTrackResult
    }

    fun recvTrack(
        mediaPeerId: String,
        mediaTag: String,
        rtpCapabilities: JsonElement?,
        successCallback: (RecvTrackResponse) -> Unit,
        failureCallback: () -> Unit
    ) {
        makeRequest(
            "recv-track",
            RecvTrackRequest(peerId, mediaPeerId, mediaTag, rtpCapabilities),
            RecvTrackResponse::class,
            IsRequestBlocking.Nonblocking,
            successCallback,
            failureCallback
        )
    }

    fun resumeConsumer(
        consumerId: String,
        successCallback: (ResumeConsumerResponse) -> Unit,
        failureCallback: () -> Unit
    ) {
        makeRequest(
            "resume-consumer",
            RequestWithConsumerId(peerId, consumerId),
            ResumeConsumerResponse::class,
            IsRequestBlocking.Nonblocking,
            successCallback,
            failureCallback
        )
    }

    fun closeConsumer(consumerId: String) {
        makeRequest(
            "close-consumer",
            RequestWithConsumerId(peerId, consumerId),
            JsonElement::class,
            IsRequestBlocking.Nonblocking,
            { Log.i(TAG, "Successfully closed consumer ($consumerId)") },
            { Log.i(TAG, "Failed to close consumer ($consumerId)") }
        )
    }

    fun leave() {
        makeRequest(
            "leave",
            BaseRequest(peerId),
            JsonElement::class,
            IsRequestBlocking.Nonblocking,
            { Log.i(TAG, "Successfully left room") },
            { Log.i(TAG, "Failed to leave room") }
        )
    }

    private fun buildAddress(cmd: String): String {
        // This is only valid because we control the values of cmd.
        // Untrusted input would need to be escaped here, using a URIBuilder
        return String.format(
            Locale.US,
            "https://%s:%d/signaling/%s",
            hostname,
            port,
            cmd
        )
    }

    private fun <E: Any> makeRequest(
        cmd: String,
        postData: BaseRequest,
        responseType: KClass<E>,
        isBlocking: IsRequestBlocking,
        successCallback: (E) -> Unit,
        failureCallback: () -> Unit
    ) {
        val jsonPostData = Utils.GSON.toJson(postData)

        val url = buildAddress(cmd)

        val requestId = requestIdGenerator.getAndIncrement()

        Log.i(TAG, "[#$requestId] Making request to $url ($jsonPostData)")

        val requestBody = jsonPostData.toRequestBody(JSON_MEDIA_TYPE)

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        val callback = object : Callback {
            override fun onResponse(
                call: Call,
                response: Response
            ) {
                try {
                    val body = response.body
                        ?: throw IOException("[#$requestId] No body in response")

                    val bodyString = body.string()

                    Log.i(TAG, "[#$requestId] Raw response: $bodyString")

                    val responseParsed = Utils.GSON.fromJson(bodyString, responseType.java)

                    successCallback(responseParsed)

                } catch (e: IOException) {
                    Log.e(TAG, "[#$requestId] Got exception getting response body", e)
                    failureCallback()
                }
            }

            override fun onFailure(
                call: Call,
                e: IOException
            ) {
                Log.e(TAG, "[#$requestId] Failed to send request", e)
                failureCallback()
            }
        }

        val call = mClient.newCall(request)

        if(isBlocking == IsRequestBlocking.Nonblocking) {
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Utils.MAIN_HANDLER.post { callback.onFailure(call, e) }
                }

                override fun onResponse(call: Call, response: Response) {
                    Utils.MAIN_HANDLER.post { callback.onResponse(call, response) }
                }
            })

        } else {
            try {
                callback.onResponse(call, call.execute())
            } catch(e: IOException) {
                callback.onFailure(call, e)
            }
        }
    }

    companion object {
        private const val TAG = "SignalingClientRawApi"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        // Build a client accepting any SSL certificate
        private fun buildClient(): OkHttpClient {

            @SuppressLint("CustomX509TrustManager")
            val trustManager: X509TrustManager = object : X509TrustManager {
                @SuppressLint("TrustAllX509TrustManager")
                override fun checkClientTrusted(
                    chain: Array<X509Certificate>,
                    authType: String
                ) {
                    // Accept anything
                }

                @SuppressLint("TrustAllX509TrustManager")
                override fun checkServerTrusted(
                    chain: Array<X509Certificate>,
                    authType: String
                ) {
                    // Accept anything
                }

                override fun getAcceptedIssuers(): Array<X509Certificate> {
                    return emptyArray()
                }
            }

            val sslContext = SSLContext.getInstance("SSL")

            sslContext.init(
                null, arrayOf<TrustManager>(trustManager),
                SecureRandom())

            val sslSocketFactory = sslContext.socketFactory

            return OkHttpClient.Builder()
                .sslSocketFactory(sslSocketFactory, trustManager)
                .hostnameVerifier { _: String?, _: SSLSession? -> true }
                .build()
        }
    }
}