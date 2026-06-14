// 노트 중복 생성 방지
package com.example.myrhythmgame.rhythm.input

import com.example.myrhythmgame.rhythm.chart.Chart
import com.example.myrhythmgame.rhythm.lane.LaneMapping
import kotlin.math.abs

class LaneBeatDuplicatePolicy(
    private val laneMapping: LaneMapping,
    private val beatTolerance: Double = DEFAULT_BEAT_TOLERANCE,
) : NoteDuplicatePolicy {
    override fun isDuplicate(chart: Chart, normalizedX: Float, snappedBeat: Double): Boolean {
        val lane = laneMapping.laneOf(normalizedX)
        return chart.notes.any { note ->
            val noteBeat = note.snappedBeat ?: return@any false
            laneMapping.laneOf(note.normalizedX) == lane &&
                    abs(noteBeat - snappedBeat) <= beatTolerance
        }
    }

    companion object {
        private const val DEFAULT_BEAT_TOLERANCE = 0.0001
    }
}
