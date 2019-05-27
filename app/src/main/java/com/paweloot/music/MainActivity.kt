package com.paweloot.music

import android.Manifest
import android.content.pm.PackageManager
import android.database.Cursor
import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem

import kotlinx.android.synthetic.main.activity_main.*
import android.provider.MediaStore
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.content_main.*
import android.provider.MediaStore.Audio


class MainActivity : AppCompatActivity() {
    private val songsList: ArrayList<Song> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }

        checkReadStoragePermission()

        loadSongs()



//        val selection = Audio.Media.IS_MUSIC + " != 0"
//
//        val projection = arrayOf(
//            Audio.Media._ID,
//            Audio.Media.ARTIST,
//            Audio.Media.TITLE,
//            Audio.Media.DATA,
//            Audio.Media.DISPLAY_NAME,
//            Audio.Media.DURATION
//        )
//
//        val cursor: Cursor? = contentResolver.query(
//            Audio.Media.EXTERNAL_CONTENT_URI,
//            projection,
//            selection,
//            null, null
//        )
//
////        cursor = this.managedQuery(
////            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
////            projection,
////            selection,
////            null, null
////        )
//
//        val songs = ArrayList<String>()
//        if (cursor != null) {
//            while (cursor.moveToNext()) {
////                songs.add(
////                    cursor.getString(0) + "||" + cursor.getString(1) + "||" + cursor.getString(2) + "||" + cursor.getString(
////                        3
////                    ) + "||" + cursor.getString(4) + "||" + cursor.getString(5)
////                )
//                songs.add(cursor.getString(3) + "\n")
//            }
//        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
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
