package com.example.myrhythmgame.rhythm.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class AudioSettingsTest {
    @Test
    fun normalizedClampsVolumesToSupportedRange() {
        val settings = AudioSettings(
            musicVolume = -0.5f,
            effectVolume = 1.5f,
        ).normalized()

        assertEquals(0f, settings.musicVolume)
        assertEquals(1f, settings.effectVolume)
    }
}
