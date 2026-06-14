package com.example.myrhythmgame.rhythm.timing

class Snapper(
    private val timingMap: TimingMap,
    val divisionsPerBeat: Int = timingMap.snapDivisionsPerBeat,
) {
    init {
        require(divisionsPerBeat > 0) { "값이 0보다 커야합니다" }
    }

    fun snapBeatFor(timeMs: Long): Double {
        val beat = timingMap.beatAt(timeMs)
        return (kotlin.math.round(beat * divisionsPerBeat) / divisionsPerBeat)
    }

    fun snapTimeFor(timeMs: Long): Long {
        return timingMap.timeAtBeat(snapBeatFor(timeMs))
    }

    val beatStep: Double
        get() = 1.0 / divisionsPerBeat

    companion object {
        const val DEFAULT_DIVISIONS_PER_BEAT = TimingMap.DEFAULT_SNAP_DIVISIONS_PER_BEAT
    }
}
