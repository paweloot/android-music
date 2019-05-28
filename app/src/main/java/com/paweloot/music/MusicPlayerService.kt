package com.paweloot.music

import android.R
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.BitmapFactory
import android.graphics.drawable.Icon
import android.media.*
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.MediaSessionManager
import android.os.Binder
import android.os.IBinder
import android.os.RemoteException
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException


class MusicPlayerService : Service(), MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener,
    MediaPlayer.OnErrorListener, MediaPlayer.OnInfoListener,
    MediaPlayer.OnBufferingUpdateListener,
    AudioManager.OnAudioFocusChangeListener {

    private val binder = LocalBinder()
    private val mediaPlayer: MediaPlayer = MediaPlayer()
    private lateinit var audioManager: AudioManager
    private var pausedSongPosition: Int = 0

    private lateinit var songsData: JSONArray
    private lateinit var currentSong: JSONObject
    private var currentSongIndex = -1

    val ACTION_PLAY = "com.paweloot.music.ACTION_PLAY"
    val ACTION_PAUSE = "com.paweloot.music.ACTION_PAUSE"
    val ACTION_PREVIOUS = "com.paweloot.music.ACTION_PREVIOUS"
    val ACTION_NEXT = "com.paweloot.music.ACTION_NEXT"
    val ACTION_STOP = "com.paweloot.music.ACTION_STOP"

    val PLAYING = 0
    val PAUSED = 1

    val REQUEST_CODE_PLAYBACK_ACTION = 668

    private var mediaSessionManager: MediaSessionManager? = null
    private var mediaSession: MediaSession? = null
    private var transportControls: MediaController.TransportControls? = null

    private val NOTIFICATION_ID = 911

    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }

    override fun onCreate() {
        super.onCreate()

        registerBroadcastReceiver()
    }

    private fun initializeMediaPlayer() {
        mediaPlayer.setOnCompletionListener(this)
        mediaPlayer.setOnPreparedListener(this)
        mediaPlayer.setOnErrorListener(this)
        mediaPlayer.setOnInfoListener(this)
        mediaPlayer.setOnBufferingUpdateListener(this)

        mediaPlayer.reset()

        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        mediaPlayer.setAudioAttributes(attributes)

        try {
            mediaPlayer.setDataSource(currentSong.getString(DATA_PATH))
        } catch (e: IOException) {
            e.printStackTrace()
            stopSelf()
        }

        mediaPlayer.prepareAsync()
    }

    private fun initializeMediaSession() {
        if (mediaSessionManager != null) return

        mediaSessionManager = getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        mediaSession = MediaSession(applicationContext, "MusicPlayer")
        transportControls = mediaSession?.controller?.transportControls

        mediaSession?.isActive = true

        updateMetaData()

        mediaSession?.setCallback(object : MediaSession.Callback() {
            override fun onPlay() {
                super.onPlay()
                resumePlayingSong()
                buildNotification(PLAYING)
            }

            override fun onPause() {
                super.onPause()
                pauseSong()
                buildNotification(PAUSED)
            }

            override fun onSkipToNext() {
                super.onSkipToNext()
                skipToNext()
                updateMetaData()
                buildNotification(PLAYING)
            }

            override fun onSkipToPrevious() {
                super.onSkipToPrevious()
//                skipToPrevious()
                updateMetaData()
                buildNotification(PLAYING)
            }

            override fun onStop() {
                super.onStop()
                removeNotification()
                stopSelf()
            }

            override fun onSeekTo(pos: Long) {
                super.onSeekTo(pos)
            }
        })
    }

    private fun skipToNext() {
        currentSongIndex = (currentSongIndex + 1) % songsData.length()
        SongsDataManager(applicationContext).saveCurrentSongIndex(currentSongIndex)
        currentSong = songsData.getJSONObject(currentSongIndex)

        stopSong()
        mediaPlayer.reset()
        initializeMediaPlayer()
    }

    private fun updateMetaData() {
        mediaSession?.setMetadata(
            MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_ARTIST, currentSong.getString(ARTIST))
                .putString(MediaMetadata.METADATA_KEY_TITLE, currentSong.getString(TITLE))
                .putString(MediaMetadata.METADATA_KEY_ALBUM, currentSong.getString(ALBUM))
                .build()
        )
    }

    private fun buildNotification(playbackStatus: Int) {
        var playPauseIconResource = R.drawable.ic_media_pause
        var playPauseAction = playbackAction(ACTION_PAUSE)

        if (playbackStatus == PAUSED) {
            playPauseIconResource = R.drawable.ic_media_play
            playPauseAction = playbackAction(ACTION_PLAY)
        }

        val notification = Notification.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setShowWhen(false)
            .setStyle(
                Notification.MediaStyle()
                    .setMediaSession(mediaSession?.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
            )
            .setColor(getColor(R.color.white))
            .setSmallIcon(R.drawable.stat_sys_headset)
            .setLargeIcon(BitmapFactory.decodeFile(currentSong.getString(ALBUM_ART)))
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .setContentText(currentSong.getString(ARTIST))
            .setContentTitle(currentSong.getString(ALBUM))
            .setSubText(currentSong.getString(TITLE))
            .addAction(createAction(R.drawable.ic_media_previous, "Previous", playbackAction(ACTION_PREVIOUS)))
            .addAction(createAction(playPauseIconResource, "Play/Pause", playPauseAction))
            .addAction(createAction(R.drawable.ic_media_next, "Next", playbackAction(ACTION_NEXT)))
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun createAction(iconResource: Int, title: String, pendingIntent: PendingIntent?): Notification.Action {
        return Notification.Action.Builder(
            Icon.createWithResource(applicationContext, iconResource),
            title,
            pendingIntent
        ).build()
    }

    private fun removeNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
    }

    private fun playbackAction(action: String): PendingIntent? {
        val playbackActionIntent = Intent(this, MusicPlayerService::class.java)

        playbackActionIntent.action = action
        return PendingIntent.getService(this, REQUEST_CODE_PLAYBACK_ACTION, playbackActionIntent, 0)
    }

    private fun handleIncomingActions(playbackAction: Intent?) {
        if (playbackAction == null || playbackAction.action == null) return

        when (playbackAction.action) {
            ACTION_PLAY -> transportControls?.play()
            ACTION_PAUSE -> transportControls?.pause()
            ACTION_NEXT -> transportControls?.skipToNext()
            ACTION_PREVIOUS -> transportControls?.skipToPrevious()
            ACTION_STOP -> transportControls?.stop()
        }
    }

    private val playNewSong = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            currentSongIndex = SongsDataManager(applicationContext).loadCurrentSongIndex()

            if (currentSongIndex != -1 && currentSongIndex < songsData.length()) {
                currentSong = songsData.getJSONObject(currentSongIndex)
            } else {
                stopSelf()
            }

            stopSong()
            mediaPlayer.reset()
            initializeMediaPlayer()
            updateMetaData()
            buildNotification(PLAYING)
        }
    }

    private fun registerBroadcastReceiver() {
        val intentFilter = IntentFilter(BROADCAST_PLAY_NEW_SONG)
        registerReceiver(playNewSong, intentFilter)
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
            val songsDataManager = SongsDataManager(applicationContext)
            songsData = songsDataManager.loadSongsData()
            currentSongIndex = songsDataManager.loadCurrentSongIndex()

            if (currentSongIndex != -1 && currentSongIndex < songsData.length()) {
                currentSong = songsData.getJSONObject(currentSongIndex)
            } else {
                stopSelf()
            }
        } catch (e: NullPointerException) {
            e.printStackTrace()
            stopSelf()
        }

        if (!requestAudioFocus()) {
            stopSelf()
        }

        if (mediaSessionManager == null) {
            try {
                initializeMediaSession()
                initializeMediaPlayer()
            } catch (e: RemoteException) {
                e.printStackTrace()
                stopSelf()
            }

            buildNotification(PLAYING)
        }

        handleIncomingActions(intent)

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()

        stopSong()
        mediaPlayer.release()
        removeAudioFocus()

        removeNotification()

        unregisterReceiver(playNewSong)
        SongsDataManager(applicationContext).clearSongsData()
    }

    override fun onError(p0: MediaPlayer?, p1: Int, p2: Int): Boolean {
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