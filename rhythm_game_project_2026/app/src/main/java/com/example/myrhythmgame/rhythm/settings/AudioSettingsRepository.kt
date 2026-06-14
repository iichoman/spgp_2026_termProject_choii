package com.example.myrhythmgame.rhythm.settings

import android.content.Context

class AudioSettingsRepository(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    fun load(): AudioSettings {
        return AudioSettings(
            musicVolume = preferences.getFloat(KEY_MUSIC_VOLUME, AudioSettings.DEFAULT_MUSIC_VOLUME),
            effectVolume = preferences.getFloat(KEY_EFFECT_VOLUME, AudioSettings.DEFAULT_EFFECT_VOLUME),
        ).normalized()
    }

    fun save(settings: AudioSettings) {
        val normalized = settings.normalized()
        preferences.edit()
            .putFloat(KEY_MUSIC_VOLUME, normalized.musicVolume)
            .putFloat(KEY_EFFECT_VOLUME, normalized.effectVolume)
            .apply()
    }

    companion object {
        private const val PREFERENCES_NAME = "audio_settings"
        private const val KEY_MUSIC_VOLUME = "music_volume"
        private const val KEY_EFFECT_VOLUME = "effect_volume"
    }
}
