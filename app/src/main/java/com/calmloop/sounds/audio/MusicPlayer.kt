package com.calmloop.sounds.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri

/** Redă în buclă o melodie aleasă de utilizator, în paralel cu sunetele generate. */
object MusicPlayer {
    private var player: MediaPlayer? = null
    private var prepared = false
    private var paused = false
    private var volume = 0.7f
    private var master = 1f

    var onError: (() -> Unit)? = null

    fun play(context: Context, uri: Uri, vol: Float): Boolean {
        stop()
        volume = vol
        paused = false
        return try {
            player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build(),
                )
                setDataSource(context.applicationContext, uri)
                isLooping = true
                setOnPreparedListener { mp ->
                    prepared = true
                    applyVolume()
                    if (!paused) mp.start()
                }
                setOnErrorListener { _, _, _ ->
                    stop()
                    onError?.invoke()
                    true
                }
                prepareAsync()
            }
            true
        } catch (e: Exception) {
            stop()
            false
        }
    }

    fun stop() {
        try {
            player?.release()
        } catch (_: Exception) {
        }
        player = null
        prepared = false
    }

    fun pause() {
        paused = true
        val p = player ?: return
        if (prepared && p.isPlaying) p.pause()
    }

    fun resume() {
        paused = false
        val p = player ?: return
        if (prepared && !p.isPlaying) p.start()
    }

    fun setVolume(v: Float) {
        volume = v
        applyVolume()
    }

    fun setMaster(v: Float) {
        master = v
        applyVolume()
    }

    private fun applyVolume() {
        val g = volume * volume * master
        try {
            player?.setVolume(g, g)
        } catch (_: Exception) {
        }
    }
}
