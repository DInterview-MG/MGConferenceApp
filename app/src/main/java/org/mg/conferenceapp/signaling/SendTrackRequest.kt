package org.mg.conferenceapp.signaling

import com.google.gson.JsonElement

class SendTrackRequest(
        peerId: String,
        val transportId: String?,
        val appData: JsonElement?,
        val kind: String?,
        val paused: Boolean,
        val rtpParameters: JsonElement?)
    : BaseRequest(peerId) {
}