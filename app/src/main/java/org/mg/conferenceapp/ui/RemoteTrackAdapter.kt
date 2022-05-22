package org.mg.conferenceapp.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import org.mg.conferenceapp.R
import org.mg.conferenceapp.Utils
import org.mg.conferenceapp.signaling.RemoteTrack
import org.mg.conferenceapp.signaling.RemoteTrackType

class RemoteTrackAdapter(
    val context: Context,
    val filter: RemoteTrackType) : BaseAdapter() {

    var tracks = arrayListOf<RemoteTrack?>(null)

    val idMap = HashMap<RemoteTrack?, Long>()

    init {
        idMap.put(null, 0)
    }

    fun update(newTracks: List<RemoteTrack>) {
        Utils.MAIN_HANDLER.post {
            tracks = ArrayList()
            tracks.add(null)

            newTracks.filter { track -> track.type() == filter }.forEach { track ->
                tracks.add(track)

                if(!idMap.containsKey(track)) {
                    idMap.put(track, idMap.size.toLong())
                }
            }
            notifyDataSetChanged()
        }
    }

    override fun hasStableIds(): Boolean {
        return true
    }

    override fun getCount(): Int {
        return tracks.size
    }

    override fun getItem(position: Int): Any? {
        return tracks.get(position)
    }

    override fun getItemId(position: Int): Long {
        return idMap.get(tracks.get(position))!!
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {

        val view = convertView
                ?: LayoutInflater.from(context).inflate(
                        R.layout.list_entry,
                        parent,
                        false);

        view as TextView

        view.setText(tracks.get(position)?.toString()
                ?: ("Incoming " + filter.description + " off"))

        return view
    }


}