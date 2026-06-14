package com.example.myrhythmgame.rhythm.input

import com.example.myrhythmgame.rhythm.chart.Chart

fun interface NoteDuplicatePolicy {
    fun isDuplicate(chart: Chart, normalizedX: Float, snappedBeat: Double): Boolean

    companion object {
        val AllowAll = NoteDuplicatePolicy { _, _, _ -> false }
    }
}
