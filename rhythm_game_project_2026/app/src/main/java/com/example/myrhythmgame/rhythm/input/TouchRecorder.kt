// 입력 시간 기록하여 ChartNote로 변환
// 입력 유지 시간을 계산하여 탭노트/롱노트 판정

package com.example.myrhythmgame.rhythm.input

import com.example.myrhythmgame.rhythm.chart.Chart
import com.example.myrhythmgame.rhythm.chart.ChartNote
import com.example.myrhythmgame.rhythm.chart.HoldNote
import com.example.myrhythmgame.rhythm.chart.TapNote
import com.example.myrhythmgame.rhythm.timing.Snapper

class TouchRecorder(
    private val chart: Chart,
    private val snapper: Snapper,
    private val duplicatePolicy: NoteDuplicatePolicy = NoteDuplicatePolicy.AllowAll,
) {
    private var nextNoteId = (chart.notes.maxOfOrNull { it.id } ?: 0L) + 1L
    private var activeEvent: RecordedTouchEvent? = null
    val events: MutableList<RecordedTouchEvent> = mutableListOf()

    val hasActiveSession: Boolean
        get() = activeEvent != null

    fun record(event: RecordedTouchEvent): ChartNote? {
        events.add(event)
        return when (event.action) {
            TouchAction.Down -> {
                activeEvent = event
                null
            }

            TouchAction.Up -> finishActiveEvent(event)
            TouchAction.Cancel -> {
                activeEvent = null
                null
            }

            TouchAction.Move -> null
        }
    }

    private fun finishActiveEvent(endEvent: RecordedTouchEvent): ChartNote? {
        val startEvent = activeEvent ?: return null
        activeEvent = null

        val startTimeMs = startEvent.audioTimeMs
        val endTimeMs = endEvent.audioTimeMs.coerceAtLeast(startTimeMs)
        val snappedStartBeat = snapper.snapBeatFor(startTimeMs)
        if (duplicatePolicy.isDuplicate(chart, startEvent.normalizedX, snappedStartBeat)) {
            return null
        }

        val durationMs = endTimeMs - startTimeMs
        val note = if (durationMs >= HOLD_THRESHOLD_MS) {
            HoldNote(
                id = nextNoteId++,
                rawStartTimeMs = startTimeMs,
                snappedStartBeat = snappedStartBeat,
                normalizedX = startEvent.normalizedX,
                rawEndTimeMs = endTimeMs,
                snappedEndBeat = snapper.snapBeatFor(endTimeMs),
            )
        } else {
            TapNote(
                id = nextNoteId++,
                rawStartTimeMs = startTimeMs,
                snappedStartBeat = snappedStartBeat,
                normalizedX = startEvent.normalizedX,
            )
        }

        chart.notes.add(note)
        return note
    }

    companion object {
        private const val HOLD_THRESHOLD_MS = 180L
    }
}
