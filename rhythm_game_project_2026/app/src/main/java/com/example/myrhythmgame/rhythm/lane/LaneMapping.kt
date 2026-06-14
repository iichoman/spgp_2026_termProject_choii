package com.example.myrhythmgame.rhythm.lane

interface LaneMapping {
    val laneCount: Int

    fun laneOf(normalizedX: Float): Int
    fun centerOfLane(lane: Int): Float
    fun snapX(normalizedX: Float): Float
}
