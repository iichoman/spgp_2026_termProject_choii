package com.example.myrhythmgame.game.play

enum class JudgmentResult(
    val windowMs: Long,
    val score: Int,
) {
    Perfect(windowMs = 40L, score = 1000),
    Great(windowMs = 80L, score = 700),
    Good(windowMs = 120L, score = 400),
    Miss(windowMs = Long.MAX_VALUE, score = 0);

    companion object {
        fun fromDelta(deltaMs: Long): JudgmentResult? {
            val absDelta = kotlin.math.abs(deltaMs)
            return when {
                absDelta <= Perfect.windowMs -> Perfect
                absDelta <= Great.windowMs -> Great
                absDelta <= Good.windowMs -> Good
                else -> null
            }
        }
    }
}
