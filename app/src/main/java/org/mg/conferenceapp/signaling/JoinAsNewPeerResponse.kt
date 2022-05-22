package org.mg.conferenceapp.signaling

import com.google.gson.JsonElement

class JoinAsNewPeerResponse {
    var routerRtpCapabilities: Map<String, JsonElement>? = null
}