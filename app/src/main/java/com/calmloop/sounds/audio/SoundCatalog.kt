package com.calmloop.sounds.audio

import androidx.annotation.StringRes
import com.calmloop.sounds.R

enum class Category(@StringRes val title: Int) {
    NATURE(R.string.cat_nature),
    NOISE(R.string.cat_noise),
    MEDITATION(R.string.cat_meditation),
}

data class SoundDef(val id: String, @StringRes val name: Int, val category: Category)

data class Preset(@StringRes val name: Int, val sounds: List<Pair<String, Float>>)

object SoundCatalog {
    val all = listOf(
        SoundDef("rain", R.string.sound_rain, Category.NATURE),
        SoundDef("tropical", R.string.sound_tropical, Category.NATURE),
        SoundDef("thunder", R.string.sound_thunder, Category.NATURE),
        SoundDef("ocean", R.string.sound_ocean, Category.NATURE),
        SoundDef("wind", R.string.sound_wind, Category.NATURE),
        SoundDef("stream", R.string.sound_stream, Category.NATURE),
        SoundDef("fire", R.string.sound_fire, Category.NATURE),
        SoundDef("white", R.string.sound_white, Category.NOISE),
        SoundDef("pink", R.string.sound_pink, Category.NOISE),
        SoundDef("brown", R.string.sound_brown, Category.NOISE),
        SoundDef("meditation", R.string.sound_meditation, Category.MEDITATION),
        SoundDef("bowl", R.string.sound_bowl, Category.MEDITATION),
        SoundDef("binaural", R.string.sound_binaural, Category.MEDITATION),
    )

    val presets = listOf(
        Preset(R.string.preset_rainy_night, listOf("rain" to 0.75f, "thunder" to 0.5f, "brown" to 0.3f)),
        Preset(R.string.preset_jungle, listOf("tropical" to 0.75f, "stream" to 0.4f)),
        Preset(R.string.preset_beach, listOf("ocean" to 0.8f, "wind" to 0.3f)),
        Preset(R.string.preset_zen, listOf("meditation" to 0.7f, "bowl" to 0.55f, "stream" to 0.25f)),
        Preset(R.string.preset_cabin, listOf("fire" to 0.7f, "rain" to 0.5f, "wind" to 0.25f)),
        Preset(R.string.preset_sleep, listOf("brown" to 0.6f, "rain" to 0.45f)),
    )

    fun byId(id: String): SoundDef? = all.firstOrNull { it.id == id }

    fun create(id: String, sampleRate: Int): SoundGenerator = when (id) {
        "rain" -> RainGenerator(sampleRate, heavy = false)
        "tropical" -> RainGenerator(sampleRate, heavy = true)
        "thunder" -> ThunderGenerator(sampleRate)
        "ocean" -> OceanGenerator(sampleRate)
        "wind" -> WindGenerator(sampleRate)
        "stream" -> StreamGenerator(sampleRate)
        "fire" -> FireGenerator(sampleRate)
        "white" -> WhiteNoiseGenerator(sampleRate)
        "pink" -> PinkNoiseGenerator(sampleRate)
        "brown" -> BrownNoiseGenerator(sampleRate)
        "meditation" -> MeditationPadGenerator(sampleRate)
        "bowl" -> SingingBowlGenerator(sampleRate)
        "binaural" -> BinauralGenerator(sampleRate)
        else -> WhiteNoiseGenerator(sampleRate)
    }
}
