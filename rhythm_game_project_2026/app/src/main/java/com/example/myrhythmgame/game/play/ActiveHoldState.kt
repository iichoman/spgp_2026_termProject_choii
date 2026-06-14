package com.example.myrhythmgame.game.play

data class ActiveHoldState(
    val noteId: Long,
    val endBeat: Double,
    val tickStepBeat: Double,
    var nextTickBeat: Double,
)
