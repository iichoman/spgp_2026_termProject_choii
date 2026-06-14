package com.example.myrhythmgame.rhythm.lane

enum class LaneMode(
    val laneCount: Int,
    val displayName: String,
) {
    FourKey(laneCount = 4, displayName = "4 KEY"),
    SixKey(laneCount = 6, displayName = "6 KEY");

    companion object {
        val Default = FourKey

        fun fromLaneCount(laneCount: Int): LaneMode? {
            return entries.firstOrNull { it.laneCount == laneCount }
        }

        fun laneCountOrDefault(laneCount: Int): Int {
            return fromLaneCount(laneCount)?.laneCount ?: Default.laneCount
        }
    }
}
