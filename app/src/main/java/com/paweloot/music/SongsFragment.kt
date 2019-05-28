package com.paweloot.music

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_songs_list.view.*


class SongsFragment : Fragment(), SongsRecyclerViewAdapter.OnSongClickListener {
    private lateinit var viewManager: RecyclerView.LayoutManager
    private lateinit var viewAdapter: SongsRecyclerViewAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_songs_list, container, false)

        viewManager = LinearLayoutManager(context)
        viewAdapter = SongsRecyclerViewAdapter((activity as MainActivity).songsData, this)

        view.songs_recycler_view.layoutManager = viewManager
        view.songs_recycler_view.adapter = viewAdapter

        return view
    }

    override fun onSongClick(position: Int) {
        (activity as MainActivity).playSong(position)
    }
}
