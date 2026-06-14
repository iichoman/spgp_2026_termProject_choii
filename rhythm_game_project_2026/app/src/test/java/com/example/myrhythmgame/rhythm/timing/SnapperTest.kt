package com.example.myrhythmgame.rhythm.timing

import org.junit.Assert.assertEquals
import org.junit.Test

class SnapperTest {
    @Test
    fun snapsToNearestQuarterBeatByDefault() {
        val timingMap = TimingMap(bpm = 120.0)
        val snapper = Snapper(timingMap)

        assertEquals(1.25, snapper.snapBeatFor(630L), 0.0001)
    }

    @Test
    fun convertsSnappedBeatBackToTime() {
        val timingMap = TimingMap(bpm = 120.0)
        val snapper = Snapper(timingMap)

        assertEquals(625L, snapper.snapTimeFor(630L))
    }

    @Test
    fun usesTimingMapSnapDivisions() {
        val timingMap = TimingMap(bpm = 120.0, snapDivisionsPerBeat = 8)
        val snapper = Snapper(timingMap)

        assertEquals(0.125, snapper.beatStep, 0.0001)
        assertEquals(1.25, snapper.snapBeatFor(630L), 0.0001)
    }
}
