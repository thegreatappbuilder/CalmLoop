package com.calmloop.sounds.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Process
import java.util.Arrays
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.tanh

/**
 * Mixer audio: amestecă toate sunetele active în timp real, cu fade-in/fade-out lin.
 * Firul audio pornește automat când apare primul sunet și se oprește când nu mai e nimic de redat.
 */
object SoundEngine {
    const val SAMPLE_RATE = 44100
    private const val FRAMES = 1024

    private class Layer(val generator: SoundGenerator) {
        @Volatile var target = 0f
        @Volatile var stopping = false
        var gain = 0f
    }

    private val layers = ConcurrentHashMap<String, Layer>()
    @Volatile private var masterTarget = 1f
    @Volatile private var paused = false
    private var masterGain = 0f
    private var thread: Thread? = null

    fun play(id: String, volume: Float) {
        val existing = layers[id]
        if (existing != null) {
            existing.stopping = false
            existing.target = volume
        } else {
            val layer = Layer(SoundCatalog.create(id, SAMPLE_RATE))
            layer.target = volume
            layers[id] = layer
        }
        ensureThread()
    }

    fun setVolume(id: String, volume: Float) {
        layers[id]?.let { if (!it.stopping) it.target = volume }
    }

    fun stop(id: String) {
        layers[id]?.let {
            it.stopping = true
            it.target = 0f
        }
    }

    fun stopAll() {
        layers.values.forEach {
            it.stopping = true
            it.target = 0f
        }
    }

    fun setPaused(value: Boolean) {
        paused = value
    }

    /** Volumul general (folosit de temporizatorul de somn pentru stingere lentă). */
    fun setMaster(value: Float) {
        masterTarget = value.coerceIn(0f, 1f)
    }

    @Synchronized
    private fun ensureThread() {
        if (thread != null) return
        thread = Thread({ runLoop() }, "CalmLoopAudio").apply { start() }
    }

    @Synchronized
    private fun exitIfIdle(): Boolean {
        if (layers.isEmpty()) {
            thread = null
            return true
        }
        return false
    }

    private fun buildTrack(): AudioTrack {
        val minBuf = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_FLOAT,
        )
        return AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                    .build(),
            )
            .setBufferSizeInBytes(max(minBuf, FRAMES * 2 * 4) * 2)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
    }

    private fun softClip(x: Float): Float {
        val a = abs(x)
        if (a <= 0.8f) return x
        val s = 0.8f + 0.2f * tanh((a - 0.8f) / 0.2f)
        return if (x < 0) -s else s
    }

    private fun runLoop() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
        val track = try {
            buildTrack()
        } catch (e: Exception) {
            synchronized(this) { thread = null }
            return
        }
        val mix = FloatArray(FRAMES * 2)
        val tmp = FloatArray(FRAMES * 2)
        val layerStep = 1f / (SAMPLE_RATE * 1.5f)
        val masterStep = 1f / (SAMPLE_RATE * 0.6f)
        try {
            track.play()
            while (true) {
                if (paused && masterGain <= 0f) {
                    track.pause()
                    while (paused && layers.isNotEmpty()) {
                        layers.entries.removeIf { it.value.stopping }
                        Thread.sleep(100)
                    }
                    if (exitIfIdle()) break
                    track.play()
                }

                Arrays.fill(mix, 0f)
                for ((id, layer) in layers) {
                    if (layer.stopping && layer.gain <= 0f) {
                        layers.remove(id, layer)
                        continue
                    }
                    layer.generator.render(tmp, FRAMES)
                    var g = layer.gain
                    val t = layer.target
                    for (i in 0 until FRAMES) {
                        if (g < t) g = min(t, g + layerStep) else if (g > t) g = max(t, g - layerStep)
                        val gg = g * g
                        mix[2 * i] += tmp[2 * i] * gg
                        mix[2 * i + 1] += tmp[2 * i + 1] * gg
                    }
                    layer.gain = g
                }

                val mt = if (paused) 0f else masterTarget
                var m = masterGain
                for (i in 0 until FRAMES) {
                    if (m < mt) m = min(mt, m + masterStep) else if (m > mt) m = max(mt, m - masterStep)
                    mix[2 * i] = softClip(mix[2 * i] * m)
                    mix[2 * i + 1] = softClip(mix[2 * i + 1] * m)
                }
                masterGain = m

                track.write(mix, 0, mix.size, AudioTrack.WRITE_BLOCKING)
                if (layers.isEmpty() && exitIfIdle()) break
            }
        } catch (_: InterruptedException) {
        } catch (_: Exception) {
        } finally {
            masterGain = 0f
            try {
                track.stop()
            } catch (_: Exception) {
            }
            track.release()
            synchronized(this) { if (thread === Thread.currentThread()) thread = null }
        }
    }
}
