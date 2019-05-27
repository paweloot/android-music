package com.paweloot.music

import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Binder
import android.os.IBinder
import java.io.IOException
import android.content.Context
import android.media.AudioFocusRequest


class MusicPlayerService : Service(), MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener,
    MediaPlayer.OnErrorListener, MediaPlayer.OnSeekCompleteListener, MediaPlayer.OnInfoListener,
    MediaPlayer.OnBufferingUpdateListener,
    AudioManager.OnAudioFocusChangeListener {

    private val binder = LocalBinder()
    private val mediaPlayer: MediaPlayer = MediaPlayer()
    private lateinit var audioManager: AudioManager
    private lateinit var songFilePath: String
    private var pausedSongPosition: Int = 0

    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }

    private fun initializeMediaPlayer() {
        mediaPlayer.setOnCompletionListener(this)
        mediaPlayer.setOnPreparedListener(this)
        mediaPlayer.setOnErrorListener(this)
        mediaPlayer.setOnSeekCompleteListener(this)
        mediaPlayer.setOnInfoListener(this)
        mediaPlayer.setOnBufferingUpdateListener(this)

        mediaPlayer.reset()

        val attributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()
        mediaPlayer.setAudioAttributes(attributes)

        try {
            mediaPlayer.setDataSource(songFilePath)
        } catch (e: IOException) {
            e.printStackTrace()
            stopSelf()
        }

        mediaPlayer.prepareAsync()
    }

    private fun playSong() {
        if (!mediaPlayer.isPlaying) {
            mediaPlayer.start()
        }
    }

    private fun stopSong() {
        if (mediaPlayer.isPlaying) {
            mediaPlayer.stop()
        }
    }

    private fun pauseSong() {
        if (mediaPlayer.isPlaying) {
            mediaPlayer.pause()
            pausedSongPosition = mediaPlayer.currentPosition
        }
    }

    private fun resumePlayingSong() {
        if (!mediaPlayer.isPlaying) {
            mediaPlayer.seekTo(pausedSongPosition)
            mediaPlayer.start()
        }
    }

    override fun onCompletion(mediaPlayer: MediaPlayer?) {
        stopSong()
        stopSelf()
    }

    override fun onPrepared(mediaPlayer: MediaPlayer?) {
        playSong()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            songFilePath = intent?.extras?.getString(SONG_FILE_PATH).toString()
        } catch (e: NullPointerException) {
            stopSelf()
        }

        if (!requestAudioFocus()) {
            stopSelf()
        }

        if (songFilePath.isNotEmpty()) {
            initializeMediaPlayer()
        }

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()

        stopSong()
        mediaPlayer.release()
        removeAudioFocus()
    }

    override fun onError(p0: MediaPlayer?, p1: Int, p2: Int): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onSeekComplete(p0: MediaPlayer?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onInfo(p0: MediaPlayer?, p1: Int, p2: Int): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onBufferingUpdate(p0: MediaPlayer?, p1: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onAudioFocusChange(p0: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    inner class LocalBinder : Binder() {
        val service: MusicPlayerService
            get() = this@MusicPlayerService

    }

    private fun requestAudioFocus(): Boolean {
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        val requestResult =
            audioManager.requestAudioFocus(AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).build())

        return requestResult == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    private fun removeAudioFocus(): Boolean {
        return AudioManager.AUDIOFOCUS_REQUEST_GRANTED == audioManager.abandonAudioFocusRequest(
            AudioFocusRequest.Builder(
                AudioManager.AUDIOFOCUS_GAIN
            ).build()
        )
    }
}