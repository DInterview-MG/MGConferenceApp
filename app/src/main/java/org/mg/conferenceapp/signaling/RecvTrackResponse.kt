package org.mg.conferenceapp.signaling

import com.google.gson.JsonElement

class RecvTrackResponse {
    var id: String? = null
    var kind: String? = null
    var producerId: String? = null
    var producerPaused: Boolean? = null
    var rtpParameters: JsonElement? = null
    var type: String? = null
}
