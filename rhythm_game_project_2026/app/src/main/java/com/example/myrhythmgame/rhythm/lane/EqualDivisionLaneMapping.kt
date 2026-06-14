package com.example.myrhythmgame.rhythm.lane

class EqualDivisionLaneMapping(
    override val laneCount: Int,
) : LaneMapping {
    init {
        require(laneCount > 0) { "값이 0보다 커야합니다." }  // 레인(key)수
    }

    override fun laneOf(normalizedX: Float): Int {
        return (normalizedX.coerceIn(0f, 1f) * laneCount)
            .toInt()
            .coerceIn(0, laneCount - 1)
    }

    override fun centerOfLane(lane: Int): Float {
        val safeLane = lane.coerceIn(0, laneCount - 1)
        return (safeLane + 0.5f) / laneCount
    }

    override fun snapX(normalizedX: Float): Float {
        return centerOfLane(laneOf(normalizedX))
    }
}
