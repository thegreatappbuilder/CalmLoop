package com.calmloop.sounds.playback

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.ContextCompat
import com.calmloop.sounds.audio.MusicPlayer
import com.calmloop.sounds.audio.SoundCatalog
import com.calmloop.sounds.audio.SoundEngine
import com.calmloop.sounds.data.UserTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PlaybackState(
    val sounds: Map<String, Float> = emptyMap(),
    val track: UserTrack? = null,
    val trackVolume: Float = 0.7f,
    val paused: Boolean = false,
    val timerEndsAt: Long? = null,
    val trackError: Boolean = false,
) {
    val isActive: Boolean get() = sounds.isNotEmpty() || track != null
}

/** Punctul central care controlează tot ce se aude în aplicație. */
object PlaybackController {
    const val DEFAULT_VOLUME = 0.7f
    private const val FADE_SECONDS = 30

    private lateinit var app: Context
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var timerJob: Job? = null

    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    fun init(context: Context) {
        app = context.applicationContext
        MusicPlayer.onError = {
            _state.update { it.copy(track = null, trackError = true) }
        }
    }

    fun toggleSound(id: String) {
        if (_state.value.sounds.containsKey(id)) stopSound(id) else playSound(id, DEFAULT_VOLUME)
    }

    fun playSound(id: String, volume: Float) {
        if (SoundCatalog.byId(id) == null) return
        resumeIfPaused()
        SoundEngine.play(id, volume)
        _state.update { it.copy(sounds = it.sounds + (id to volume)) }
        ensureService()
    }

    fun stopSound(id: String) {
        SoundEngine.stop(id)
        _state.update { it.copy(sounds = it.sounds - id) }
        afterChange()
    }

    fun setSoundVolume(id: String, volume: Float) {
        SoundEngine.setVolume(id, volume)
        _state.update { if (it.sounds.containsKey(id)) it.copy(sounds = it.sounds + (id to volume)) else it }
    }

    fun applyPreset(sounds: List<Pair<String, Float>>) {
        val wanted = sounds.map { it.first }.toSet()
        _state.value.sounds.keys.filter { it !in wanted }.forEach { SoundEngine.stop(it) }
        resumeIfPaused()
        sounds.forEach { (id, v) -> SoundEngine.play(id, v) }
        _state.update { it.copy(sounds = sounds.toMap()) }
        ensureService()
    }

    fun playTrack(track: UserTrack) {
        resumeIfPaused()
        val vol = _state.value.trackVolume
        val ok = MusicPlayer.play(app, Uri.parse(track.uri), vol)
        _state.update { it.copy(track = if (ok) track else null, trackError = !ok) }
        if (ok) ensureService()
    }

    fun stopTrack() {
        MusicPlayer.stop()
        _state.update { it.copy(track = null) }
        afterChange()
    }

    fun setTrackVolume(volume: Float) {
        MusicPlayer.setVolume(volume)
        _state.update { it.copy(trackVolume = volume) }
    }

    fun clearTrackError() {
        _state.update { it.copy(trackError = false) }
    }

    fun togglePause() {
        val p = !_state.value.paused
        SoundEngine.setPaused(p)
        if (p) MusicPlayer.pause() else MusicPlayer.resume()
        _state.update { it.copy(paused = p) }
    }

    fun stopAll() {
        timerJob?.cancel()
        timerJob = null
        SoundEngine.stopAll()
        SoundEngine.setPaused(false)
        SoundEngine.setMaster(1f)
        MusicPlayer.stop()
        MusicPlayer.setMaster(1f)
        _state.update { PlaybackState(trackVolume = it.trackVolume) }
    }

    /** Temporizator de somn: după [minutes] minute sunetele se sting lent și se opresc. */
    fun setTimer(minutes: Int?) {
        timerJob?.cancel()
        timerJob = null
        SoundEngine.setMaster(1f)
        MusicPlayer.setMaster(1f)
        if (minutes == null || minutes <= 0) {
            _state.update { it.copy(timerEndsAt = null) }
            return
        }
        val totalMs = minutes * 60_000L
        _state.update { it.copy(timerEndsAt = System.currentTimeMillis() + totalMs) }
        timerJob = scope.launch {
            delay((totalMs - FADE_SECONDS * 1000L).coerceAtLeast(0L))
            for (step in 1..FADE_SECONDS) {
                val m = 1f - step / FADE_SECONDS.toFloat()
                SoundEngine.setMaster(m)
                MusicPlayer.setMaster(m)
                delay(1000)
            }
            stopAll()
        }
    }

    private fun resumeIfPaused() {
        if (_state.value.paused) {
            SoundEngine.setPaused(false)
            MusicPlayer.resume()
            _state.update { it.copy(paused = false) }
        }
    }

    private fun afterChange() {
        if (!_state.value.isActive) {
            timerJob?.cancel()
            timerJob = null
            SoundEngine.setPaused(false)
            SoundEngine.setMaster(1f)
            MusicPlayer.setMaster(1f)
            _state.update { it.copy(timerEndsAt = null, paused = false) }
        }
    }

    private fun ensureService() {
        if (PlaybackService.isRunning) return
        try {
            ContextCompat.startForegroundService(app, Intent(app, PlaybackService::class.java))
        } catch (_: Exception) {
            // fără serviciu, sunetul merge în continuare cât timp aplicația e deschisă
        }
    }
}
