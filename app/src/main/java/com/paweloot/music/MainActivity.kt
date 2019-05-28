package com.paweloot.music

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.MediaStore.Audio
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.paweloot.music.MusicPlayerService.LocalBinder
import org.json.JSONArray
import org.json.JSONObject


class MainActivity : AppCompatActivity() {
    val songsData: JSONArray = JSONArray()
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
    }

    fun playSong(songIndex: Int) {
        val dataManager = SongsDataManager(this)

        if (!isServiceBound) {
            dataManager.saveSongsData(songsData)
            dataManager.saveCurrentSongIndex(songIndex)

            val intent = Intent(this, MusicPlayerService::class.java)

            startService(intent)
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
            Audio.Media.ARTIST
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

                songsData.put(
                    JSONObject(
                        mapOf(
                            DATA_PATH to data,
                            TITLE to title,
                            ALBUM to album,
                            ARTIST to artist
                        )
                    )
                )
            }
        }

        cursor?.close()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.channel_name)
            val descriptionText = getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("MusicPlayer", name, importance).apply {
                description = descriptionText
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
