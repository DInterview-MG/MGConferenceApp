package org.mg.conferenceapp.signaling

import com.google.gson.JsonElement

class RecvTrackRequest(
        peerId: String,
        val mediaPeerId: String,
        val mediaTag: String,
        val rtpCapabilities: JsonElement?)
    : BaseRequest(peerId) {
}
