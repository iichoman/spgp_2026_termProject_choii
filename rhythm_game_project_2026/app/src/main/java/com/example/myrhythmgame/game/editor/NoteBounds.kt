package com.example.myrhythmgame.game.editor

import com.example.myrhythmgame.rhythm.chart.ChartNote

data class NoteBounds(
    val note: ChartNote,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    fun contains(x: Float, y: Float, padding: Float = 0f): Boolean {
        return x >= left - padding &&
                x <= right + padding &&
                y >= top - padding &&
                y <= bottom + padding
    }

    val centerY: Float
        get() = (top + bottom) / 2f
}
