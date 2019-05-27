package com.paweloot.music

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_songs_list.*
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
        viewAdapter = SongsRecyclerViewAdapter((activity as MainActivity).songsList, this)

        view.songs_recycler_view.layoutManager = viewManager
        view.songs_recycler_view.adapter = viewAdapter

        return view
    }

    override fun onSongClick(position: Int) {
//        Toast.makeText(context, "Clicked on $position!", Toast.LENGTH_SHORT).show()
        (activity as MainActivity).playSong(position)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

    }

    override fun onDetach() {
        super.onDetach()

    }
}
