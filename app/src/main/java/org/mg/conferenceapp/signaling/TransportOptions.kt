package org.mg.conferenceapp.signaling

import com.google.gson.JsonElement

class TransportOptions {
    var id: String? = null
    var iceParameters: JsonElement? = null
    var iceCandidates: JsonElement? = null
    var dtlsParameters: JsonElement? = null
}