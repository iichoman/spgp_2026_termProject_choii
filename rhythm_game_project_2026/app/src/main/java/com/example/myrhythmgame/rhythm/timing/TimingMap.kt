// 음원 시간과 박자 변환

package com.example.myrhythmgame.rhythm.timing

data class TimingMap(
    val bpm: Double,
    val offsetMs: Long = 0L, // 시작 박자 오프셋: 사용되지 않음...
    val snapDivisionsPerBeat: Int = DEFAULT_SNAP_DIVISIONS_PER_BEAT,
) {
    init {
        require(bpm > 0.0) { "값이 0보다 커야합니다" }
        require(snapDivisionsPerBeat > 0) { "값이 0보다 커야합니다." }
    }

    val beatDurationMs: Double
        get() = MS_PER_MINUTE / bpm

    fun beatAt(timeMs: Long): Double {
        return (timeMs - offsetMs) / beatDurationMs
    }

    fun timeAtBeat(beat: Double): Long {
        return (offsetMs + beat * beatDurationMs).toLong()
    }

    companion object {
        private const val MS_PER_MINUTE = 60_000.0
        const val DEFAULT_SNAP_DIVISIONS_PER_BEAT = 4
    }
}
