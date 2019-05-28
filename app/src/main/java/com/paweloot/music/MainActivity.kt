package com.paweloot.music

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.opengl.Visibility
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.MediaStore
import android.provider.MediaStore.Audio
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.paweloot.music.MusicPlayerService.LocalBinder
import org.json.JSONArray
import org.json.JSONObject
import java.util.*


class MainActivity : AppCompatActivity() {
    val songsData: JSONArray = JSONArray()
    private val albumsData: HashMap<Int, String?> = HashMap()
    private lateinit var musicPlayerService: MusicPlayerService
    private var isServiceBound: Boolean = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder = service as LocalBinder
            musicPlayerService = binder.service
            isServiceBound = true

            Toast.makeText(this@MainActivity, "Service Bound", Toast.LENGTH_SHORT).show()
        }

        override fun onServiceDisconnected(name: ComponentName) {
            isServiceBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        createNotificationChannel()
        checkReadStoragePermission()
        loadSongs()
        loadAlbumCovers()
        joinSongsDataAndAlbumCovers()
    }

    fun playSong(songIndex: Int) {
        val dataManager = SongsDataManager(this)

        if (!isServiceBound) {
            dataManager.saveSongsData(songsData)
            dataManager.saveCurrentSongIndex(songIndex)

            val intent = Intent(this, MusicPlayerService::class.java)

            startForegroundService(intent)
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        } else {
            dataManager.saveCurrentSongIndex(songIndex)

            val broadcast = Intent(BROADCAST_PLAY_NEW_SONG)
            sendBroadcast(broadcast)
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        if (isServiceBound) {
            unbindService(serviceConnection)
            musicPlayerService.stopSelf()
        }
    }


    override fun onSaveInstanceState(outState: Bundle?) {
        outState?.putBoolean(SERVICE_BOUND_STATE, isServiceBound)

        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        super.onRestoreInstanceState(savedInstanceState)

        val savedServiceBoundState = savedInstanceState?.getBoolean(SERVICE_BOUND_STATE)
        isServiceBound = savedServiceBoundState ?: false
    }

    private fun loadSongs() {
        val projection = arrayOf(
            Audio.Media.DATA,
            Audio.Media.TITLE,
            Audio.Media.ALBUM,
            Audio.Media.ARTIST,
            Audio.Media.ALBUM_ID
        )
        val selection = Audio.Media.IS_MUSIC + " != 0"
        val sort = Audio.Media.TITLE + " ASC"

        val cursor =
            contentResolver.query(
                Audio.Media.EXTERNAL_CONTENT_URI,
                projection, selection, null, sort
            )

        if (cursor != null && cursor.count > 0) {
            while (cursor.moveToNext()) {
                val data = cursor.getString(0)
                val title = cursor.getString(1)
                val album = cursor.getString(2)
                val artist = cursor.getString(3)
                val albumId = cursor.getString(4)

                songsData.put(
                    JSONObject(
                        mapOf(
                            DATA_PATH to data,
                            TITLE to title,
                            ALBUM to album,
                            ARTIST to artist,
                            ALBUM_ID to albumId
                        )
                    )
                )
            }
        }

        cursor?.close()
    }

    private fun loadAlbumCovers() {
        val projection = arrayOf(
            Audio.Albums._ID,
            Audio.Albums.ALBUM_ART
        )

        val cursor = contentResolver.query(
            Audio.Albums.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            null
        )

        if (cursor != null && cursor.count > 0) {
            while (cursor.moveToNext()) {
                val albumId = cursor.getString(0)
                val albumArt = cursor.getString(1)

                albumsData[albumId.toInt()] = albumArt
            }
        }

        cursor?.close()
    }

    private fun joinSongsDataAndAlbumCovers() {
        for (i in 0 until songsData.length()) {
            val song = songsData.getJSONObject(i)
            val albumId = song.getString(ALBUM_ID)

            song.put(ALBUM_ART, albumsData[albumId.toInt()])
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.channel_name)
            val descriptionText = getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, name, importance).apply {
                description = descriptionText
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }

            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private val READ_STORAGE_CODE = 666

    private fun checkReadStoragePermission() {
        val permission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
        if (permission != PackageManager.PERMISSION_GRANTED) {
            makeRequest()
        }
    }

    private fun makeRequest() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), READ_STORAGE_CODE
        )
    }
}
