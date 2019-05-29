package com.paweloot.music


import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_playing.view.*


class PlayingFragment(private val songsDataManager: SongsDataManager) : Fragment() {
    private val STATE_PLAYING = 666
    private val STATE_PAUSED = 676
    private var currentState = STATE_PLAYING

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val inflatedView = inflater.inflate(R.layout.fragment_playing, container, false)

        setCurrentSongData(inflatedView)
        setButtonsOnClicks(inflatedView)

        return inflatedView
    }

    private fun setCurrentSongData(view: View) {
        val currentSongIndex = songsDataManager.loadCurrentSongIndex()
        val currentSong = songsDataManager.loadSongsData().getJSONObject(currentSongIndex)

        view.apply {
            playing_title.text = currentSong.getString(TITLE)
            playing_artist.text = currentSong.getString(ARTIST)

            if (currentSong.has(ALBUM_ART)) {
                playing_album_art.setImageDrawable(Drawable.createFromPath(currentSong.getString(ALBUM_ART)))
            } else {
                playing_album_art.setImageResource(R.drawable.ic_android_white)
            }
        }
    }

    private fun setButtonsOnClicks(view: View) {
        setPlayPauseButtonOnClick(view)
        setSkipButtonOnClick(view)
        setPreviousButtonOnClick(view)
    }

    private fun setPlayPauseButtonOnClick(view: View) {
        view.playing_play_pause_btn.setOnClickListener {
            val musicPlayerService = (activity as MainActivity).musicPlayerService

            when (currentState) {
                STATE_PLAYING -> {
                    musicPlayerService.pauseSong()
                    view.playing_play_pause_btn.setBackgroundResource(R.drawable.ic_play_circle_filled_white)
                    currentState = STATE_PAUSED
                }
                STATE_PAUSED -> {
                    musicPlayerService.resumePlayingSong()
                    view.playing_play_pause_btn.setBackgroundResource(R.drawable.ic_pause_circle_filled_white)
                    currentState = STATE_PLAYING
                }
            }

        }
    }

    private fun setSkipButtonOnClick(view: View) {
        view.playing_skip_btn.setOnClickListener {
            val musicPlayerService = (activity as MainActivity).musicPlayerService

            musicPlayerService.skipToNext()
            setCurrentSongData(view)
            currentState = STATE_PLAYING
            view.playing_play_pause_btn.setBackgroundResource(R.drawable.ic_pause_circle_filled_white)
        }
    }

    private fun setPreviousButtonOnClick(view: View) {
        view.playing_previous_btn.setOnClickListener {
            val musicPlayerService = (activity as MainActivity).musicPlayerService

            musicPlayerService.skipToPrevious()
            setCurrentSongData(view)
            currentState = STATE_PLAYING
            view.playing_play_pause_btn.setBackgroundResource(R.drawable.ic_pause_circle_filled_white)
        }
    }
}
