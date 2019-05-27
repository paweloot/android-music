package com.paweloot.music

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.IBinder
import android.provider.MediaStore.Audio
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.paweloot.music.MusicPlayerService.LocalBinder


class MainActivity : AppCompatActivity() {
    val songsList: ArrayList<Song> = ArrayList()
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

        checkReadStoragePermission()
        loadSongs()
    }

    fun playSong(songIndex: Int) {
        if (!isServiceBound) {
            val intent = Intent(this, MusicPlayerService::class.java)
            intent.putExtra(SONG_FILE_PATH, songsList[songIndex].dataPath)

            startService(intent)
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        } else {

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

                songsList.add(Song(data, title, album, artist))
            }
        }

        cursor?.close()
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
