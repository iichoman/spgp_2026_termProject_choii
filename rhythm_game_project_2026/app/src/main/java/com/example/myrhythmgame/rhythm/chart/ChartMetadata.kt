package com.example.myrhythmgame.rhythm.chart

import com.example.myrhythmgame.rhythm.lane.LaneMode

data class ChartMetadata(
    val id: String,
    val displayName: String,
    val difficulty: Int = DEFAULT_DIFFICULTY,
    val laneCount: Int = LaneMode.Default.laneCount,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
) {
    init {
        require(LaneMode.fromLaneCount(laneCount) != null) {
            "Unsupported lane count: $laneCount"
        }
    }

    companion object {
        const val DEFAULT_DIFFICULTY = 1

        fun defaultFor(songTitle: String): ChartMetadata {
            return ChartMetadata(
                id = "default",
                displayName = if (songTitle.isBlank()) "New Chart" else songTitle,
            )
        }
    }
}
