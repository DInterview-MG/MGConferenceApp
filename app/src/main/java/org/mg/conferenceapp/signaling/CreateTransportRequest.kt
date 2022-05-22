package org.mg.conferenceapp.signaling

class CreateTransportRequest(peerId: String, val direction: String)
    : BaseRequest(peerId) {
}