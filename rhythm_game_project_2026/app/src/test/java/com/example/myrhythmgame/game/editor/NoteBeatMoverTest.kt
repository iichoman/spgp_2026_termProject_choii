package com.example.myrhythmgame.game.editor

import com.example.myrhythmgame.rhythm.chart.HoldNote
import com.example.myrhythmgame.rhythm.chart.TapNote
import com.example.myrhythmgame.rhythm.timing.TimingMap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteBeatMoverTest {
    private val timingMap = TimingMap(bpm = 120.0)
    private val mover = NoteBeatMover(timingMap)

    @Test
    fun movesTapNoteByBeatDelta() {
        val note = TapNote(
            id = 1L,
            rawStartTimeMs = 1_000L,
            snappedStartBeat = 2.0,
            normalizedX = 0.5f,
        )

        assertTrue(mover.move(note, 0.25))
        assertEquals(2.25, note.snappedStartBeat ?: -1.0, 0.0001)
        assertEquals(1_125L, note.rawStartTimeMs)
    }

    @Test
    fun movesHoldStartAndEndTogether() {
        val note = HoldNote(
            id = 1L,
            rawStartTimeMs = 1_000L,
            snappedStartBeat = 2.0,
            normalizedX = 0.5f,
            rawEndTimeMs = 2_000L,
            snappedEndBeat = 4.0,
        )

        assertTrue(mover.move(note, -0.5))
        assertEquals(1.5, note.snappedStartBeat ?: -1.0, 0.0001)
        assertEquals(3.5, note.snappedEndBeat ?: -1.0, 0.0001)
        assertEquals(1_000L, note.durationMs)
    }

    @Test
    fun doesNotMoveBeforeFirstBeat() {
        val note = TapNote(
            id = 1L,
            rawStartTimeMs = 0L,
            snappedStartBeat = 0.0,
            normalizedX = 0.5f,
        )

        assertFalse(mover.move(note, -0.25))
        assertEquals(0.0, note.snappedStartBeat ?: -1.0, 0.0001)
    }
}
