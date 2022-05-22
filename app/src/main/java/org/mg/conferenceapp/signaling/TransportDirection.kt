package org.mg.conferenceapp.signaling

enum class TransportDirection(val protocolName: String) {
    SEND("send"),
    RECEIVE("recv");
}