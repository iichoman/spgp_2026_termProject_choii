package com.example.myrhythmgame.rhythm.audio

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LoopRegionTest {
    @Test
    fun enabledLoopWrapsAtEnd() {
        val loop = LoopRegion(startMs = 1_000L, endMs = 2_000L)

        assertFalse(loop.shouldWrap(1_999L))
        assertTrue(loop.shouldWrap(2_000L))
    }

    @Test
    fun disabledLoopDoesNotWrap() {
        val loop = LoopRegion(startMs = 1_000L, endMs = 2_000L, enabled = false)

        assertFalse(loop.shouldWrap(2_000L))
    }

    @Test(expected = IllegalArgumentException::class)
    fun endMustBeGreaterThanStart() {
        LoopRegion(startMs = 1_000L, endMs = 1_000L)
    }
}
