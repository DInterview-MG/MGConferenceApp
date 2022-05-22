package org.mg.conferenceapp.signaling

import com.google.gson.JsonElement

class ConnectTransportRequest(
        peerId: String,
        val transportId: String,
        val dtlsParameters: JsonElement?)
    : BaseRequest(peerId) {
}