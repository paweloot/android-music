package com.paweloot.music

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import org.json.JSONArray

class SongsDataManager(val context: Context) {

    private val SHARED_PREFS_SONGS = "com.paweloot.music.SHARED_PREFS_SONGS"
    private val SONGS_DATA = "SONGS_DATA"
    private val CURRENT_SONG_INDEX = "CURRENT_SONG_INDEX"
    private lateinit var sharedPreferences: SharedPreferences

    fun saveSongsData(songsData: JSONArray) {
        sharedPreferences = context.getSharedPreferences(SHARED_PREFS_SONGS, Context.MODE_PRIVATE)

        sharedPreferences.edit {
            putString(SONGS_DATA, songsData.toString())
            apply()
        }
    }

    fun loadSongsData(): JSONArray {
        sharedPreferences = context.getSharedPreferences(SHARED_PREFS_SONGS, Context.MODE_PRIVATE)

        return JSONArray(sharedPreferences.getString(SONGS_DATA, null))
    }

    fun saveCurrentSongIndex(index: Int) {
        sharedPreferences = context.getSharedPreferences(SHARED_PREFS_SONGS, Context.MODE_PRIVATE)

        sharedPreferences.edit {
            putInt(CURRENT_SONG_INDEX, index)
            apply()
        }
    }

    fun loadCurrentSongIndex(): Int {
        sharedPreferences = context.getSharedPreferences(SHARED_PREFS_SONGS, Context.MODE_PRIVATE)

        return sharedPreferences.getInt(CURRENT_SONG_INDEX, -1)
    }

    fun clearSongsData() {
        sharedPreferences = context.getSharedPreferences(SHARED_PREFS_SONGS, Context.MODE_PRIVATE)

        sharedPreferences.edit {
            clear()
            commit()
        }
    }
}