package com.example.myrhythmgame.game.editor

data class RecordingNotePreview(
    val startTimeMs: Long,
    val snappedStartBeat: Double?,
    val normalizedX: Float,
)
