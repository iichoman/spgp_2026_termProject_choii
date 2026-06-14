package com.example.myrhythmgame.rhythm.input

import com.example.myrhythmgame.rhythm.chart.Chart
import com.example.myrhythmgame.rhythm.chart.HoldNote
import com.example.myrhythmgame.rhythm.chart.NoteType
import com.example.myrhythmgame.rhythm.chart.SongMetadata
import com.example.myrhythmgame.rhythm.chart.TapNote
import com.example.myrhythmgame.rhythm.lane.EqualDivisionLaneMapping
import com.example.myrhythmgame.rhythm.timing.Snapper
import com.example.myrhythmgame.rhythm.timing.TimingMap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class TouchRecorderTest {
    @Test
    fun downAndShortUpEventCreatesTapNote() {
        val chart = Chart(SongMetadata(title = "test"), TimingMap(bpm = 120.0))
        val recorder = TouchRecorder(chart, Snapper(chart.timing))

        val pendingNote = recorder.record(
            RecordedTouchEvent(
                pointerId = 0,
                action = TouchAction.Down,
                audioTimeMs = 630L,
                normalizedX = 0.4f,
                normalizedY = 0.8f,
            )
        )
        val note = recorder.record(
            RecordedTouchEvent(
                pointerId = 0,
                action = TouchAction.Up,
                audioTimeMs = 700L,
                normalizedX = 0.4f,
                normalizedY = 0.8f,
            )
        )

        assertNull(pendingNote)
        assertNotNull(note)
        assertEquals(TapNote::class, note!!::class)
        assertEquals(1, chart.notes.size)
        assertEquals(NoteType.Tap, chart.notes.first().type)
        assertEquals(630L, chart.notes.first().rawTimeMs)
        assertEquals(0.4f, chart.notes.first().normalizedX)
        assertEquals(1.25, chart.notes.first().snappedBeat ?: -1.0, 0.0001)
    }

    @Test
    fun downAndLongUpEventCreatesHoldNote() {
        val chart = Chart(SongMetadata(title = "test"), TimingMap(bpm = 120.0))
        val recorder = TouchRecorder(chart, Snapper(chart.timing))

        recorder.record(
            RecordedTouchEvent(
                pointerId = 0,
                action = TouchAction.Down,
                audioTimeMs = 500L,
                normalizedX = 0.25f,
                normalizedY = 0.8f,
            )
        )
        val note = recorder.record(
            RecordedTouchEvent(
                pointerId = 0,
                action = TouchAction.Up,
                audioTimeMs = 900L,
                normalizedX = 0.25f,
                normalizedY = 0.8f,
            )
        )

        assertNotNull(note)
        assertEquals(HoldNote::class, note!!::class)
        assertEquals(1, chart.notes.size)
        assertEquals(NoteType.Hold, chart.notes.first().type)
        assertEquals(500L, chart.notes.first().rawTimeMs)
        assertEquals(900L, chart.notes.first().rawEndTimeMs)
        assertEquals(400L, chart.notes.first().durationMs)
        assertEquals(1.0, chart.notes.first().snappedBeat ?: -1.0, 0.0001)
        assertEquals(1.75, chart.notes.first().snappedEndBeat ?: -1.0, 0.0001)
    }

    @Test
    fun moveEventRecordsRawEventWithoutCreatingNote() {
        val chart = Chart(SongMetadata(title = "test"), TimingMap(bpm = 120.0))
        val recorder = TouchRecorder(chart, Snapper(chart.timing))

        val note = recorder.record(
            RecordedTouchEvent(
                pointerId = 0,
                action = TouchAction.Move,
                audioTimeMs = 640L,
                normalizedX = 0.5f,
                normalizedY = 0.8f,
            )
        )

        assertNull(note)
        assertEquals(1, recorder.events.size)
        assertEquals(0, chart.notes.size)
    }

    @Test
    fun duplicatePolicyPreventsSameLaneSameSnappedBeat() {
        val chart = Chart(SongMetadata(title = "test"), TimingMap(bpm = 120.0))
        val recorder = TouchRecorder(
            chart = chart,
            snapper = Snapper(chart.timing),
            duplicatePolicy = LaneBeatDuplicatePolicy(EqualDivisionLaneMapping(laneCount = 6)),
        )

        recorder.record(
            RecordedTouchEvent(
                pointerId = 0,
                action = TouchAction.Down,
                audioTimeMs = 500L,
                normalizedX = 0.1f,
                normalizedY = 0.8f,
            )
        )
        val firstNote = recorder.record(
            RecordedTouchEvent(
                pointerId = 0,
                action = TouchAction.Up,
                audioTimeMs = 560L,
                normalizedX = 0.1f,
                normalizedY = 0.8f,
            )
        )

        recorder.record(
            RecordedTouchEvent(
                pointerId = 0,
                action = TouchAction.Down,
                audioTimeMs = 500L,
                normalizedX = 0.1f,
                normalizedY = 0.8f,
            )
        )
        val duplicateNote = recorder.record(
            RecordedTouchEvent(
                pointerId = 0,
                action = TouchAction.Up,
                audioTimeMs = 560L,
                normalizedX = 0.1f,
                normalizedY = 0.8f,
            )
        )

        assertNotNull(firstNote)
        assertNull(duplicateNote)
        assertEquals(1, chart.notes.size)
    }
}
