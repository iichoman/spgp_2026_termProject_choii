package com.example.myrhythmgame.rhythm.audio

data class AudioLatencyConfig(
    val inputOffsetMs: Long = 0L,
    val outputOffsetMs: Long = 0L,
) {
    fun recordingTimeFor(audioPositionMs: Long): Long {
        return (audioPositionMs - inputOffsetMs).coerceAtLeast(0L)
    }

    fun judgmentTimeFor(audioPositionMs: Long): Long {
        return (audioPositionMs + outputOffsetMs).coerceAtLeast(0L)
    }
}
