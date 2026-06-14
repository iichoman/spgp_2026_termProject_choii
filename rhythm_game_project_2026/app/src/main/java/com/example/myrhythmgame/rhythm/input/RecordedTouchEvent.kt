package com.example.myrhythmgame.rhythm.input

enum class TouchAction {
    Down,
    Move,
    Up,
    Cancel,
}

data class RecordedTouchEvent(
    val pointerId: Int,
    val action: TouchAction,
    val audioTimeMs: Long,
    val normalizedX: Float,
    val normalizedY: Float,
) {
    init {
        require(audioTimeMs >= 0L) { "Audio time 이 양수여야 합니다" }
    }
}
