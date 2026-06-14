package com.example.myrhythmgame.rhythm.timing

import org.junit.Assert.assertEquals
import org.junit.Test

class TimingMapTest {
    @Test
    fun convertsTimeToBeat() {
        val timingMap = TimingMap(bpm = 120.0)

        assertEquals(2.0, timingMap.beatAt(1_000L), 0.0001)
    }

    @Test
    fun convertsBeatToTime() {
        val timingMap = TimingMap(bpm = 120.0)

        assertEquals(1_000L, timingMap.timeAtBeat(2.0))
    }

    @Test
    fun offsetShiftsBeatOrigin() {
        val timingMap = TimingMap(bpm = 120.0, offsetMs = 250L)

        assertEquals(0.0, timingMap.beatAt(250L), 0.0001)
    }
}
