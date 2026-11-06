package com.calmloop.sounds.audio

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.tan

/** Generator rapid de numere aleatoare (xorshift). */
class Rng(seed: Long = System.nanoTime()) {
    private var s: Long = (seed xor 0x5DEECE66DL) or 1L

    /** Zgomot alb în intervalul [-1, 1). */
    fun white(): Float {
        s = s xor (s shl 13)
        s = s xor (s ushr 7)
        s = s xor (s shl 17)
        return (s ushr 40).toInt() / 8388608f - 1f
    }

    /** Valoare în [0, 1). */
    fun uni(): Float = (white() + 1f) * 0.5f

    fun range(a: Float, b: Float): Float = a + (b - a) * uni()
}

class Pink(private val rng: Rng) {
    private var b0 = 0f
    private var b1 = 0f
    private var b2 = 0f

    fun next(): Float {
        val w = rng.white()
        b0 = 0.99765f * b0 + w * 0.0990460f
        b1 = 0.96300f * b1 + w * 0.2965164f
        b2 = 0.57000f * b2 + w * 1.0526913f
        return (b0 + b1 + b2 + w * 0.1848f) * 0.2f
    }
}

class Brown(private val rng: Rng) {
    private var y = 0f
    fun next(): Float {
        y = (y + 0.02f * rng.white()) / 1.02f
        return y * 3.5f
    }
}

class OnePoleLP(sampleRate: Int, cutoff: Float) {
    private val sr = sampleRate.toFloat()
    private var a = 0f
    var y = 0f
        private set

    init {
        set(cutoff)
    }

    fun set(cutoff: Float) {
        a = 1f - exp(-2f * PI.toFloat() * cutoff / sr)
    }

    fun process(x: Float): Float {
        y += a * (x - y)
        return y
    }
}

class OnePoleHP(sampleRate: Int, cutoff: Float) {
    private val lp = OnePoleLP(sampleRate, cutoff)
    fun process(x: Float): Float = x - lp.process(x)
}

/** Filtru state-variable (topologie TPT), ieșire trece-bandă și trece-jos. */
class Svf(sampleRate: Int) {
    private val sr = sampleRate.toFloat()
    private var a1 = 0f
    private var a2 = 0f
    private var a3 = 0f
    private var k = 1f
    private var ic1 = 0f
    private var ic2 = 0f
    var low = 0f
        private set

    init {
        set(1000f, 1f)
    }

    fun set(cutoff: Float, q: Float) {
        val fc = cutoff.coerceIn(20f, sr * 0.45f)
        val g = tan(PI.toFloat() * fc / sr)
        k = 1f / q
        a1 = 1f / (1f + g * (g + k))
        a2 = g * a1
        a3 = g * a2
    }

    /** Returnează ieșirea trece-bandă. */
    fun process(x: Float): Float {
        val v3 = x - ic2
        val v1 = a1 * ic1 + a2 * v3
        val v2 = ic2 + a2 * ic1 + a3 * v3
        ic1 = 2f * v1 - ic1
        ic2 = 2f * v2 - ic2
        low = v2
        return v1
    }
}

object SineTable {
    private const val SIZE = 4096
    private val table = FloatArray(SIZE + 1) { sin(2.0 * PI * it / SIZE).toFloat() }

    /** phase în [0, 1). */
    fun at(phase: Float): Float {
        val p = phase * SIZE
        val i = p.toInt().coerceIn(0, SIZE - 1)
        val f = p - i
        return table[i] + (table[i + 1] - table[i]) * f
    }
}

class Osc(sampleRate: Int, var freq: Float, startPhase: Float = 0f) {
    private val inv = 1f / sampleRate
    private var phase = startPhase

    fun next(): Float {
        val v = SineTable.at(phase)
        phase += freq * inv
        while (phase >= 1f) phase -= 1f
        return v
    }
}

/** Valoare aleatoare care se schimbă lin în timp (pentru variații naturale). */
class SmoothRandom(
    sampleRate: Int,
    private val rng: Rng,
    rateHz: Float,
    private val min: Float,
    private val max: Float,
) {
    private val step = rateHz / sampleRate
    private var cur = rng.range(min, max)
    private var target = rng.range(min, max)
    private var pos = 0f

    fun next(): Float {
        pos += step
        if (pos >= 1f) {
            pos -= 1f
            cur = target
            target = rng.range(min, max)
        }
        val s = pos * pos * (3f - 2f * pos)
        return cur + (target - cur) * s
    }
}

/**
 * Sunete scurte de impact (picături, pocnituri de foc): zgomot filtrat cu anvelopă exponențială.
 */
class DropPool(private val sampleRate: Int, private val rng: Rng, size: Int = 48) {
    private val amp = FloatArray(size)
    private val decay = FloatArray(size)
    private val panL = FloatArray(size)
    private val panR = FloatArray(size)
    private val lowState = FloatArray(size)
    private val lowA = FloatArray(size)
    private val brightState = FloatArray(size)
    private val brightA = FloatArray(size)
    private val twoPi = 2f * PI.toFloat()

    var outL = 0f
        private set
    var outR = 0f
        private set

    fun spawn(amplitude: Float, tauSeconds: Float, pan: Float, brightHz: Float, lowCutHz: Float = 700f) {
        var slot = -1
        var weakest = Float.MAX_VALUE
        for (i in amp.indices) {
            if (amp[i] < 1e-4f) {
                slot = i
                break
            }
            if (amp[i] < weakest) {
                weakest = amp[i]
                slot = i
            }
        }
        if (slot < 0) return
        amp[slot] = amplitude
        decay[slot] = exp(-1f / (tauSeconds * sampleRate))
        panL[slot] = 1f - pan * 0.7f
        panR[slot] = 0.3f + pan * 0.7f
        lowA[slot] = 1f - exp(-twoPi * lowCutHz / sampleRate)
        brightA[slot] = 1f - exp(-twoPi * brightHz / sampleRate)
        lowState[slot] = 0f
        brightState[slot] = 0f
    }

    fun process() {
        var l = 0f
        var r = 0f
        for (i in amp.indices) {
            val a = amp[i]
            if (a < 1e-4f) continue
            val w = rng.white()
            brightState[i] += brightA[i] * (w - brightState[i])
            lowState[i] += lowA[i] * (brightState[i] - lowState[i])
            val s = (brightState[i] - lowState[i]) * a
            l += s * panL[i]
            r += s * panR[i]
            amp[i] = a * decay[i]
        }
        outL = l
        outR = r
    }
}

/** Ecou stereo simplu cu atenuare — dă senzația de spațiu pentru muzica de meditație. */
class StereoSpace(sampleRate: Int, timeL: Float, timeR: Float, private val feedback: Float, private val mix: Float) {
    private val bufL = FloatArray((sampleRate * timeL).toInt())
    private val bufR = FloatArray((sampleRate * timeR).toInt())
    private var iL = 0
    private var iR = 0
    private val dampL = OnePoleLP(sampleRate, 2500f)
    private val dampR = OnePoleLP(sampleRate, 2500f)
    var outL = 0f
        private set
    var outR = 0f
        private set

    fun process(l: Float, r: Float) {
        val dl = dampL.process(bufL[iL])
        val dr = dampR.process(bufR[iR])
        bufL[iL] = l + dr * feedback
        bufR[iR] = r + dl * feedback
        iL = (iL + 1) % bufL.size
        iR = (iR + 1) % bufR.size
        outL = l + dl * mix
        outR = r + dr * mix
    }
}
