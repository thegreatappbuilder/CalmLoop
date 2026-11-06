package com.calmloop.sounds.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

data class UserTrack(val uri: String, val name: String)

/** Lista melodiilor adăugate de utilizator (salvată pe telefon). */
object TrackStore {
    private const val PREFS = "tracks"
    private const val KEY = "list"
    private lateinit var appContext: Context

    private val _tracks = MutableStateFlow<List<UserTrack>>(emptyList())
    val tracks: StateFlow<List<UserTrack>> = _tracks.asStateFlow()

    fun init(context: Context) {
        appContext = context.applicationContext
        _tracks.value = load()
    }

    fun add(uris: List<Uri>) {
        if (uris.isEmpty()) return
        val current = _tracks.value.toMutableList()
        for (uri in uris) {
            try {
                appContext.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: Exception) {
            }
            val s = uri.toString()
            if (current.none { it.uri == s }) current.add(UserTrack(s, displayName(uri)))
        }
        _tracks.value = current
        save(current)
    }

    fun remove(track: UserTrack) {
        try {
            appContext.contentResolver.releasePersistableUriPermission(
                Uri.parse(track.uri),
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        } catch (_: Exception) {
        }
        val list = _tracks.value.filterNot { it.uri == track.uri }
        _tracks.value = list
        save(list)
    }

    private fun displayName(uri: Uri): String {
        try {
            appContext.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    val name = c.getString(0)
                    if (!name.isNullOrBlank()) return name.substringBeforeLast('.')
                }
            }
        } catch (_: Exception) {
        }
        return uri.lastPathSegment ?: "Track"
    }

    private fun load(): List<UserTrack> {
        val raw = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map {
                val o = arr.getJSONObject(it)
                UserTrack(o.getString("uri"), o.optString("name", "Track"))
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun save(list: List<UserTrack>) {
        val arr = JSONArray()
        list.forEach { arr.put(JSONObject().put("uri", it.uri).put("name", it.name)) }
        appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, arr.toString()).apply()
    }
}
