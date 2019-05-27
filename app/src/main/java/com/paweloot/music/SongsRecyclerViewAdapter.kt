package com.paweloot.music

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.fragment_songs_list_item.view.*


class SongsRecyclerViewAdapter(val data: List<Song>, private val onSongClickListener: OnSongClickListener) :
    RecyclerView.Adapter<SongsRecyclerViewAdapter.SongViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val inflatedView = LayoutInflater.from(parent.context)
            .inflate(R.layout.fragment_songs_list_item, parent, false) as View

        return SongViewHolder(inflatedView, onSongClickListener)
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        val song = data[position]

        holder.apply {
            setSongTitle(song.title)
            setSongArtist(song.artist)
        }
    }

    override fun getItemCount(): Int = data.size

    inner class SongViewHolder(private val view: View, private val onSongClickListener: OnSongClickListener) :
        RecyclerView.ViewHolder(view), View.OnClickListener {

        init {
            view.setOnClickListener(this)
        }

        fun setSongTitle(title: String) {
            view.list_item_song_title.text = title
        }

        fun setSongArtist(artist: String) {
            view.list_item_song_artist.text = artist
        }

        override fun onClick(view: View?) {
            onSongClickListener.onSongClick(adapterPosition)
        }
    }

    interface OnSongClickListener {
        fun onSongClick(position: Int)
    }
}
