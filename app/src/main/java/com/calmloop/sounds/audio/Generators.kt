package com.calmloop.sounds.audio

import kotlin.math.exp

/**
 * Toate sunetele sunt generate în timp real, deci se repetă la infinit fără nicio „tăietură"
 * și nu ocupă spațiu în aplicație. Fiecare generator scrie audio stereo intercalat (L, R, L, R...).
 */
abstract class SoundGenerator(protected val sr: Int) {
    protected val rng = Rng(System.nanoTime() xor javaClass.name.hashCode().toLong())
    abstract fun render(out: FloatArray, frames: Int)
}

// ------------------------------------------------------------------ Ploaie

class RainGenerator(sr: Int, private val heavy: Boolean) : SoundGenerator(sr) {
    private val pinkL = Pink(Rng(rng.white().toBits().toLong() + 11))
    private val pinkR = Pink(Rng(rng.white().toBits().toLong() + 23))
    private val hpL = OnePoleHP(sr, if (heavy) 250f else 450f)
    private val hpR = OnePoleHP(sr, if (heavy) 250f else 450f)
    private val lpL = OnePoleLP(sr, if (heavy) 8000f else 5500f)
    private val lpR = OnePoleLP(sr, if (heavy) 8000f else 5500f)
    private val brown = Brown(rng)
    private val rumbleLp = OnePoleLP(sr, 220f)
    private val drops = DropPool(sr, rng, 64)
    private val bigDrops = DropPool(sr, rng, 8)
    private val intensity = SmoothRandom(sr, rng, 0.07f, 0.8f, 1.1f)
    private val dropRate = if (heavy) 320f else 90f
    private val hissGain = if (heavy) 1.6f else 1.0f

    override fun render(out: FloatArray, frames: Int) {
        for (i in 0 until frames) {
            val inten = intensity.next()
            if (rng.uni() < dropRate * inten / sr) {
                drops.spawn(
                    amplitude = rng.range(0.08f, 0.45f),
                    tauSeconds = rng.range(0.003f, 0.016f),
                    pan = rng.uni(),
                    brightHz = rng.range(2500f, 9000f),
                )
            }
            if (heavy && rng.uni() < 3f / sr) {
                // picături mari de pe frunze
                bigDrops.spawn(rng.range(0.4f, 0.8f), rng.range(0.02f, 0.05f), rng.uni(), rng.range(900f, 2200f), 250f)
            }
            drops.process()
            bigDrops.process()
            var l = lpL.process(hpL.process(pinkL.next())) * hissGain * inten + drops.outL + bigDrops.outL
            var r = lpR.process(hpR.process(pinkR.next())) * hissGain * inten + drops.outR + bigDrops.outR
            if (heavy) {
                val rumble = rumbleLp.process(brown.next()) * 0.7f * inten
                l += rumble
                r += rumble * 0.9f
            }
            out[2 * i] = l * 0.8f
            out[2 * i + 1] = r * 0.8f
        }
    }
}

// ------------------------------------------------------------------ Tunete

class ThunderGenerator(sr: Int) : SoundGenerator(sr) {
    private val brown = Brown(rng)
    private val lowLp = OnePoleLP(sr, 160f)
    private val bodyLp = OnePoleLP(sr, 600f)
    private val crackLp = OnePoleLP(sr, 3000f)
    private val flicker = SmoothRandom(sr, rng, 7f, 0.25f, 1f)
    private var countdown = (rng.range(2f, 6f) * sr).toInt()
    private var rumble = 0f
    private var rumbleDecay = 0.9999f
    private var attack = 1f
    private var attackStep = 0f
    private var crack = 0f
    private var crackDecay = 0.999f
    private var panL = 1f
    private var panR = 1f

    override fun render(out: FloatArray, frames: Int) {
        for (i in 0 until frames) {
            if (--countdown <= 0) {
                countdown = (rng.range(14f, 40f) * sr).toInt()
                rumble = rng.range(0.6f, 1f)
                rumbleDecay = exp(-1f / (rng.range(2.5f, 6f) * sr))
                attack = 0f
                attackStep = 1f / (rng.range(0.05f, 0.6f) * sr)
                crack = if (rng.uni() < 0.45f) rng.range(0.3f, 0.7f) else 0.05f
                crackDecay = exp(-1f / (0.3f * sr))
                val pan = rng.uni()
                panL = 1.2f - pan * 0.6f
                panR = 0.6f + pan * 0.6f
            }
            if (attack < 1f) attack = (attack + attackStep).coerceAtMost(1f)
            val b = brown.next()
            val low = lowLp.process(b)
            val body = bodyLp.process(b)
            val crk = crackLp.process(rng.white()) * crack
            val env = rumble * attack
            val s = (low * 2.6f + body * 0.8f * env) * env * flicker.next() + crk + low * 0.12f
            rumble *= rumbleDecay
            crack *= crackDecay
            out[2 * i] = s * panL
            out[2 * i + 1] = s * panR
        }
    }
}

// ------------------------------------------------------------------ Valuri

class OceanGenerator(sr: Int) : SoundGenerator(sr) {
    private val pinkL = Pink(rng)
    private val pinkR = Pink(Rng(rng.white().toBits().toLong() + 7))
    private val brownL = Brown(rng)
    private val brownR = Brown(Rng(rng.white().toBits().toLong() + 3))
    private val lpL = OnePoleLP(sr, 500f)
    private val lpR = OnePoleLP(sr, 500f)
    private var posL = 0f
    private var posR = 0.35f
    private var lenL = rng.range(8f, 12f)
    private var lenR = rng.range(8f, 12f)
    private var counter = 0

    private fun waveEnv(p: Float): Float {
        return if (p < 0.4f) {
            val x = p / 0.4f
            x * x * (3f - 2f * x)
        } else {
            val x = (p - 0.4f) / 0.6f
            val y = 1f - x
            y * y
        }
    }

    override fun render(out: FloatArray, frames: Int) {
        for (i in 0 until frames) {
            posL += 1f / (lenL * sr)
            posR += 1f / (lenR * sr)
            if (posL >= 1f) {
                posL -= 1f
                lenL = rng.range(8f, 13f)
            }
            if (posR >= 1f) {
                posR -= 1f
                lenR = rng.range(8f, 13f)
            }
            val eL = waveEnv(posL)
            val eR = waveEnv(posR)
            if (counter++ % 32 == 0) {
                lpL.set(200f + eL * eL * 3200f)
                lpR.set(200f + eR * eR * 3200f)
            }
            val sL = lpL.process(pinkL.next() * 1.3f + brownL.next() * 0.5f) * (0.18f + 0.82f * eL)
            val sR = lpR.process(pinkR.next() * 1.3f + brownR.next() * 0.5f) * (0.18f + 0.82f * eR)
            out[2 * i] = (sL * 0.85f + sR * 0.15f) * 1.4f
            out[2 * i + 1] = (sR * 0.85f + sL * 0.15f) * 1.4f
        }
    }
}

// ------------------------------------------------------------------ Vânt

class WindGenerator(sr: Int) : SoundGenerator(sr) {
    private val pinkL = Pink(rng)
    private val pinkR = Pink(Rng(rng.white().toBits().toLong() + 5))
    private val svfL = Svf(sr)
    private val svfR = Svf(sr)
    private val freqL = SmoothRandom(sr, rng, 0.12f, 220f, 900f)
    private val freqR = SmoothRandom(sr, rng, 0.1f, 220f, 900f)
    private val gust = SmoothRandom(sr, rng, 0.18f, 0.25f, 1f)
    private val brown = Brown(rng)
    private val lowLp = OnePoleLP(sr, 300f)
    private var counter = 0

    override fun render(out: FloatArray, frames: Int) {
        for (i in 0 until frames) {
            val fL = freqL.next()
            val fR = freqR.next()
            val g = gust.next()
            if (counter++ % 32 == 0) {
                svfL.set(fL, 1.4f)
                svfR.set(fR, 1.4f)
            }
            val low = lowLp.process(brown.next()) * 0.5f
            out[2 * i] = (svfL.process(pinkL.next()) * 2.4f * g + low * g)
            out[2 * i + 1] = (svfR.process(pinkR.next()) * 2.4f * g + low * g)
        }
    }
}

// ------------------------------------------------------------------ Pârâu

class StreamGenerator(sr: Int) : SoundGenerator(sr) {
    private val svf1 = Svf(sr)
    private val svf2 = Svf(sr)
    private val f1 = SmoothRandom(sr, rng, 3f, 700f, 2200f)
    private val f2 = SmoothRandom(sr, rng, 4f, 1200f, 3200f)
    private val m1 = SmoothRandom(sr, rng, 14f, 0.15f, 1f)
    private val m2 = SmoothRandom(sr, rng, 19f, 0.15f, 1f)
    private val pink = Pink(rng)
    private val baseLp = OnePoleLP(sr, 1400f)
    private val bubbleFreq = FloatArray(6)
    private val bubbleAmp = FloatArray(6)
    private val bubblePhase = FloatArray(6)
    private val bubbleDecay = exp(-1f / (0.02f * sr))
    private val glide = 1f + 2.5f / sr
    private var counter = 0

    override fun render(out: FloatArray, frames: Int) {
        for (i in 0 until frames) {
            if (counter++ % 32 == 0) {
                svf1.set(f1.next(), 3f)
                svf2.set(f2.next(), 3f)
            } else {
                f1.next()
                f2.next()
            }
            if (rng.uni() < 7f / sr) {
                val slot = (rng.uni() * bubbleAmp.size).toInt().coerceIn(0, bubbleAmp.size - 1)
                bubbleFreq[slot] = rng.range(400f, 1300f)
                bubbleAmp[slot] = rng.range(0.05f, 0.15f)
            }
            var bubbles = 0f
            for (b in bubbleAmp.indices) {
                if (bubbleAmp[b] < 1e-4f) continue
                bubblePhase[b] += bubbleFreq[b] / sr
                if (bubblePhase[b] >= 1f) bubblePhase[b] -= 1f
                bubbles += SineTable.at(bubblePhase[b]) * bubbleAmp[b]
                bubbleAmp[b] *= bubbleDecay
                bubbleFreq[b] *= glide
            }
            val w = rng.white()
            val a = svf1.process(w) * m1.next() * 0.9f
            val b = svf2.process(rng.white()) * m2.next() * 0.7f
            val base = baseLp.process(pink.next()) * 0.6f
            out[2 * i] = a + b * 0.5f + base + bubbles
            out[2 * i + 1] = b + a * 0.5f + base + bubbles
        }
    }
}

// ------------------------------------------------------------------ Foc de tabără

class FireGenerator(sr: Int) : SoundGenerator(sr) {
    private val brown = Brown(rng)
    private val roarLp = OnePoleLP(sr, 380f)
    private val roarMod = SmoothRandom(sr, rng, 1.2f, 0.5f, 1f)
    private val crackleRate = SmoothRandom(sr, rng, 0.4f, 4f, 30f)
    private val crackles = DropPool(sr, rng, 32)
    private val pink = Pink(rng)
    private val hissHp = OnePoleHP(sr, 2500f)

    override fun render(out: FloatArray, frames: Int) {
        for (i in 0 until frames) {
            if (rng.uni() < crackleRate.next() / sr) {
                val n = if (rng.uni() < 0.2f) (2 + rng.uni() * 4).toInt() else 1
                val pan = rng.uni()
                repeat(n) {
                    crackles.spawn(rng.range(0.15f, 0.7f), rng.range(0.0006f, 0.003f), pan, rng.range(3000f, 11000f), 1200f)
                }
            }
            crackles.process()
            val roar = roarLp.process(brown.next()) * 1.4f * roarMod.next()
            val hiss = hissHp.process(pink.next()) * 0.06f
            out[2 * i] = roar + hiss + crackles.outL
            out[2 * i + 1] = roar * 0.95f + hiss + crackles.outR
        }
    }
}

// ------------------------------------------------------------------ Zgomote

class WhiteNoiseGenerator(sr: Int) : SoundGenerator(sr) {
    private val r2 = Rng(rng.white().toBits().toLong() + 99)
    override fun render(out: FloatArray, frames: Int) {
        for (i in 0 until frames) {
            out[2 * i] = rng.white() * 0.22f
            out[2 * i + 1] = r2.white() * 0.22f
        }
    }
}

class PinkNoiseGenerator(sr: Int) : SoundGenerator(sr) {
    private val l = Pink(rng)
    private val r = Pink(Rng(rng.white().toBits().toLong() + 17))
    override fun render(out: FloatArray, frames: Int) {
        for (i in 0 until frames) {
            out[2 * i] = l.next() * 0.9f
            out[2 * i + 1] = r.next() * 0.9f
        }
    }
}

class BrownNoiseGenerator(sr: Int) : SoundGenerator(sr) {
    private val l = Brown(rng)
    private val r = Brown(Rng(rng.white().toBits().toLong() + 31))
    override fun render(out: FloatArray, frames: Int) {
        for (i in 0 until frames) {
            out[2 * i] = l.next() * 0.8f
            out[2 * i + 1] = r.next() * 0.8f
        }
    }
}

// ------------------------------------------------------------------ Muzică de meditație

/** Acorduri ambientale lente care alternează între La major și Fa# minor. */
class MeditationPadGenerator(sr: Int) : SoundGenerator(sr) {
    private val chordA = floatArrayOf(110f, 164.81f, 220f, 277.18f, 329.63f)
    private val chordB = floatArrayOf(92.5f, 138.59f, 185f, 220f, 277.18f)
    private val oscA = chordA.map { Osc(sr, it, rng.uni()) to Osc(sr, it * 1.0015f, rng.uni()) }
    private val oscB = chordB.map { Osc(sr, it, rng.uni()) to Osc(sr, it * 1.0015f, rng.uni()) }
    private val octA = chordA.map { Osc(sr, it * 2f, rng.uni()) }
    private val octB = chordB.map { Osc(sr, it * 2f, rng.uni()) }
    private val ampA = chordA.map { SmoothRandom(sr, rng, rng.range(0.05f, 0.12f), 0.25f, 1f) }
    private val ampB = chordB.map { SmoothRandom(sr, rng, rng.range(0.05f, 0.12f), 0.25f, 1f) }
    private val morph = Osc(sr, 1f / 48f, 0f)
    private val lpL = OnePoleLP(sr, 1800f)
    private val lpR = OnePoleLP(sr, 1800f)
    private val space = StereoSpace(sr, 0.43f, 0.61f, 0.55f, 0.45f)

    override fun render(out: FloatArray, frames: Int) {
        for (i in 0 until frames) {
            val m = morph.next() * 0.5f + 0.5f
            val wa = m
            val wb = 1f - m
            var l = 0f
            var r = 0f
            for (v in chordA.indices) {
                val a = ampA[v].next() * wa
                val (o1, o2) = oscA[v]
                val oct = octA[v].next() * 0.25f
                val panR = v / 4f
                l += (o1.next() + oct) * a * (1f - panR * 0.6f)
                r += (o2.next() + oct) * a * (0.4f + panR * 0.6f)
            }
            for (v in chordB.indices) {
                val a = ampB[v].next() * wb
                val (o1, o2) = oscB[v]
                val oct = octB[v].next() * 0.25f
                val panR = 1f - v / 4f
                l += (o1.next() + oct) * a * (1f - panR * 0.6f)
                r += (o2.next() + oct) * a * (0.4f + panR * 0.6f)
            }
            space.process(lpL.process(l * 0.11f), lpR.process(r * 0.11f))
            out[2 * i] = space.outL
            out[2 * i + 1] = space.outR
        }
    }
}

/** Boluri tibetane lovite la intervale aleatoare. */
class SingingBowlGenerator(sr: Int) : SoundGenerator(sr) {
    private val ratios = floatArrayOf(1f, 2.76f, 5.4f, 8.93f)
    private val partAmp = floatArrayOf(1f, 0.45f, 0.22f, 0.1f)
    private val partTau = floatArrayOf(10f, 6f, 3f, 1.5f)
    private val notes = floatArrayOf(196f, 220f, 261.63f, 293.66f, 329.63f)
    private val maxStrikes = 4
    private val phaseL = Array(maxStrikes) { FloatArray(4) }
    private val phaseR = Array(maxStrikes) { FloatArray(4) }
    private val freq = Array(maxStrikes) { FloatArray(4) }
    private val env = Array(maxStrikes) { FloatArray(4) }
    private val decay = Array(maxStrikes) { FloatArray(4) }
    private val attack = FloatArray(maxStrikes) { 1f }
    private val beat = FloatArray(maxStrikes)
    private var next = 0
    private var countdown = (0.4f * sr).toInt()
    private val attackStep = 1f / (0.006f * sr)
    private val space = StereoSpace(sr, 0.37f, 0.53f, 0.6f, 0.5f)

    private fun strike() {
        val s = next
        next = (next + 1) % maxStrikes
        val base = notes[(rng.uni() * notes.size).toInt().coerceIn(0, notes.size - 1)]
        val amp = rng.range(0.5f, 0.9f)
        for (p in 0 until 4) {
            freq[s][p] = base * ratios[p]
            env[s][p] = amp * partAmp[p]
            decay[s][p] = exp(-1f / (partTau[p] * sr))
        }
        beat[s] = rng.range(0.4f, 1.6f)
        attack[s] = 0f
    }

    override fun render(out: FloatArray, frames: Int) {
        for (i in 0 until frames) {
            if (--countdown <= 0) {
                strike()
                countdown = (rng.range(9f, 16f) * sr).toInt()
            }
            var l = 0f
            var r = 0f
            for (s in 0 until maxStrikes) {
                if (env[s][0] < 1e-4f) continue
                if (attack[s] < 1f) attack[s] = (attack[s] + attackStep).coerceAtMost(1f)
                for (p in 0 until 4) {
                    val e = env[s][p] * attack[s]
                    if (e < 1e-5f) continue
                    val f = freq[s][p]
                    phaseL[s][p] += f / sr
                    phaseR[s][p] += (f + beat[s]) / sr
                    if (phaseL[s][p] >= 1f) phaseL[s][p] -= 1f
                    if (phaseR[s][p] >= 1f) phaseR[s][p] -= 1f
                    l += SineTable.at(phaseL[s][p]) * e
                    r += SineTable.at(phaseR[s][p]) * e
                    env[s][p] *= decay[s][p]
                }
            }
            space.process(l * 0.28f, r * 0.28f)
            out[2 * i] = space.outL
            out[2 * i + 1] = space.outR
        }
    }
}

/** Ritm binaural theta (6 Hz) — se aude corect doar cu căști. */
class BinauralGenerator(sr: Int) : SoundGenerator(sr) {
    private val left = Osc(sr, 200f)
    private val right = Osc(sr, 206f)
    private val pink = Pink(rng)
    private val lp = OnePoleLP(sr, 700f)

    override fun render(out: FloatArray, frames: Int) {
        for (i in 0 until frames) {
            val bed = lp.process(pink.next()) * 0.35f
            out[2 * i] = left.next() * 0.22f + bed
            out[2 * i + 1] = right.next() * 0.22f + bed
        }
    }
}
