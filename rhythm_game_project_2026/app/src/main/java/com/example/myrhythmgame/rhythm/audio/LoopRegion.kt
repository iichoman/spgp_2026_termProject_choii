package com.example.myrhythmgame.rhythm.audio

data class LoopRegion(
    val startMs: Long,
    val endMs: Long,
    val enabled: Boolean = true,
) {
    init {
        require(startMs >= 0L) { "Loop start must be non-negative." }
        require(endMs > startMs) { "Loop end must be greater than loop start." }
    }

    fun shouldWrap(positionMs: Long): Boolean {
        return enabled && positionMs >= endMs
    }
}
