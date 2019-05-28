package com.paweloot.music

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import org.json.JSONArray

class SongsDataManager(val context: Context) {

    private val SHARED_PREFS_SONGS = "com.paweloot.music.SHARED_PREFS_SONGS"
    private val SONGS_DATA = "SONGS_DATA"
    private val CURRENT_SONG_INDEX = "CURRENT_SONG_INDEX"

    fun saveSongsData(songsData: JSONArray) {
        getSharedPrefs().edit {
            putString(SONGS_DATA, songsData.toString())
            apply()
        }
    }

    fun loadSongsData(): JSONArray {
        val songsData = getSharedPrefs().getString(SONGS_DATA, null)
        return if (songsData == null) JSONArray() else JSONArray(songsData)
    }

    fun saveCurrentSongIndex(index: Int) {
        getSharedPrefs().edit {
            putInt(CURRENT_SONG_INDEX, index)
            apply()
        }
    }

    fun loadCurrentSongIndex(): Int {
        return getSharedPrefs().getInt(CURRENT_SONG_INDEX, -1)
    }

    fun clearSongsData() {
        getSharedPrefs().edit {
            clear()
            commit()
        }
    }

    private fun getSharedPrefs(): SharedPreferences {
        return context.getSharedPreferences(SHARED_PREFS_SONGS, Context.MODE_PRIVATE)
    }
}