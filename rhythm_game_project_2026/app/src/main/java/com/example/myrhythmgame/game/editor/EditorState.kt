package com.example.myrhythmgame.game.editor

data class EditorState(
    var selectedNoteId: Long? = null,
    var isDraggingTimeline: Boolean = false,
    var dragStartY: Float = 0f,
    var dragStartAudioTimeMs: Long = 0L,
)
