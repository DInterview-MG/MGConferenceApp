package org.mg.conferenceapp.signaling

import org.mg.conferenceapp.Constants

class RemoteTrack(
    val peerName: String,
    val trackName: String,
    private val ownPeerName: String
) : Comparable<RemoteTrack> {

    fun type(): RemoteTrackType? {

        return when(trackName) {
            Constants.CAM_VIDEO_MEDIA_TAG -> RemoteTrackType.VIDEO
            Constants.CAM_AUDIO_MEDIA_TAG -> RemoteTrackType.AUDIO
            Constants.SCREEN_VIDEO_MEDIA_TAG -> RemoteTrackType.VIDEO
            else -> null
        }
    }

    override fun compareTo(other: RemoteTrack): Int {

        val cmp = peerName.compareTo(other.peerName)

        if(cmp != 0) {
            return cmp
        }

        return trackName.compareTo(other.trackName)
    }

    override fun toString(): String {
        return String.format(
                "%s (%s)",
                trackName,
                if(peerName == ownPeerName) {
                    "me"
                } else {
                    peerName
                })
    }

    override fun equals(other: Any?): Boolean {
        if(this === other) return true
        if(other !is RemoteTrack) return false

        return peerName == other.peerName && trackName == other.trackName
    }

    override fun hashCode(): Int {
        return 31 * peerName.hashCode() + trackName.hashCode()
    }
}