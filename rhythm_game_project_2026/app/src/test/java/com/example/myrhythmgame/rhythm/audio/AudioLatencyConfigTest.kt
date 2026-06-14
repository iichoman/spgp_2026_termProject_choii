package com.example.myrhythmgame.rhythm.audio

import org.junit.Assert.assertEquals
import org.junit.Test

class AudioLatencyConfigTest {
    @Test
    fun recordingTimeSubtractsInputOffset() {
        val config = AudioLatencyConfig(inputOffsetMs = 32L)

        assertEquals(968L, config.recordingTimeFor(1_000L))
    }

    @Test
    fun recordingTimeDoesNotGoBelowZero() {
        val config = AudioLatencyConfig(inputOffsetMs = 50L)

        assertEquals(0L, config.recordingTimeFor(20L))
    }

    @Test
    fun judgmentTimeAddsOutputOffset() {
        val config = AudioLatencyConfig(outputOffsetMs = 24L)

        assertEquals(1_024L, config.judgmentTimeFor(1_000L))
    }
}
