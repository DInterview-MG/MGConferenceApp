package org.mg.conferenceapp.signaling

class RequestWithConsumerId(
        peerId: String,
        val consumerId: String)
    : BaseRequest(peerId) {
}