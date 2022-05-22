package org.mg.conferenceapp.signaling

import com.google.gson.JsonElement

class SyncResponse {
    var activeSpeaker: Map<String, JsonElement>? = null
    var peers: Map<String, Peer>?  = null
}