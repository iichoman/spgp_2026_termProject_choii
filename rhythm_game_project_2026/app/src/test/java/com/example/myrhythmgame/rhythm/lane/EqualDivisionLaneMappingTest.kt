package com.example.myrhythmgame.rhythm.lane

import org.junit.Assert.assertEquals
import org.junit.Test

class EqualDivisionLaneMappingTest {
    @Test
    fun mapsNormalizedPositionToLane() {
        val mapping = EqualDivisionLaneMapping(laneCount = 6)

        assertEquals(0, mapping.laneOf(0f))
        assertEquals(2, mapping.laneOf(0.40f))
        assertEquals(5, mapping.laneOf(1f))
    }

    @Test
    fun returnsLaneCenter() {
        val mapping = EqualDivisionLaneMapping(laneCount = 6)

        assertEquals(1f / 12f, mapping.centerOfLane(0), 0.0001f)
        assertEquals(11f / 12f, mapping.centerOfLane(5), 0.0001f)
    }

    @Test
    fun snapsPositionToLaneCenter() {
        val mapping = EqualDivisionLaneMapping(laneCount = 6)

        assertEquals(5f / 12f, mapping.snapX(0.40f), 0.0001f)
    }
}
