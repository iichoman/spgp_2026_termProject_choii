package com.example.myrhythmgame.rhythm.settings

data class AudioSettings(
    val musicVolume: Float = DEFAULT_MUSIC_VOLUME,
    val effectVolume: Float = DEFAULT_EFFECT_VOLUME,
) {
    fun normalized(): AudioSettings {
        return copy(
            musicVolume = musicVolume.coerceIn(MIN_VOLUME, MAX_VOLUME),
            effectVolume = effectVolume.coerceIn(MIN_VOLUME, MAX_VOLUME),
        )
    }

    companion object {
        const val MIN_VOLUME = 0f
        const val MAX_VOLUME = 1f
        const val DEFAULT_MUSIC_VOLUME = 0.8f
        const val DEFAULT_EFFECT_VOLUME = 1f
    }
}
