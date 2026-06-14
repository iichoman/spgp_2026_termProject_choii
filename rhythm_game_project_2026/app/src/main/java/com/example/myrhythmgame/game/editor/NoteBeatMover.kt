package com.example.myrhythmgame.game.editor

import com.example.myrhythmgame.rhythm.chart.ChartNote
import com.example.myrhythmgame.rhythm.chart.HoldNote
import com.example.myrhythmgame.rhythm.timing.TimingMap

class NoteBeatMover(
    private val timingMap: TimingMap,
) {
    fun move(note: ChartNote, beatDelta: Double): Boolean {
        val startBeat = note.snappedStartBeat ?: timingMap.beatAt(note.rawStartTimeMs)
        val endBeat = if (note is HoldNote) {
            note.snappedEndBeat ?: timingMap.beatAt(note.rawEndTimeMs ?: note.rawStartTimeMs)
        } else {
            null
        }
        val clampedDelta = beatDelta.coerceAtLeast(-startBeat)
        if (clampedDelta == 0.0) return false

        note.snappedStartBeat = startBeat + clampedDelta
        note.rawStartTimeMs = timingMap.timeAtBeat(note.snappedStartBeat ?: startBeat)
        if (note is HoldNote && endBeat != null) {
            note.snappedEndBeat = endBeat + clampedDelta
            note.rawEndTimeMs = timingMap.timeAtBeat(note.snappedEndBeat ?: endBeat)
        }
        return true
    }
}
