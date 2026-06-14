package com.example.myrhythmgame.game.play

data class PlayState(
    var score: Int = 0,
    var combo: Int = 0,
    var maxCombo: Int = 0,
    var health: Int = MAX_HEALTH,
    var lastJudgment: JudgmentResult? = null,
    var lastJudgmentTimeMs: Long = 0L,
    val judgedNoteIds: MutableSet<Long> = mutableSetOf(),
    val missedNoteIds: MutableSet<Long> = mutableSetOf(),
    val activeHolds: MutableMap<Long, ActiveHoldState> = mutableMapOf(),
) {
    fun applyHit(noteId: Long, judgment: JudgmentResult, audioTimeMs: Long) {
        judgedNoteIds.add(noteId)
        lastJudgment = judgment
        lastJudgmentTimeMs = audioTimeMs
        score += judgment.score
        combo += 1
        maxCombo = maxOf(maxCombo, combo)
        recoverHealth()
    }

    fun applyMiss(noteId: Long, audioTimeMs: Long) {
        if (noteId in judgedNoteIds) return
        activeHolds.remove(noteId)
        judgedNoteIds.add(noteId)
        missedNoteIds.add(noteId)
        lastJudgment = JudgmentResult.Miss
        lastJudgmentTimeMs = audioTimeMs
        combo = 0
        health = (health - MISS_DAMAGE).coerceAtLeast(0)
    }

    fun isJudged(noteId: Long): Boolean {
        return noteId in judgedNoteIds
    }

    fun isActiveHold(noteId: Long): Boolean {
        return noteId in activeHolds
    }

    fun isMissed(noteId: Long): Boolean {
        return noteId in missedNoteIds
    }

    val isHealthDepleted: Boolean
        get() = health <= 0

    fun startHold(
        noteId: Long,
        judgment: JudgmentResult,
        audioTimeMs: Long,
        startBeat: Double,
        endBeat: Double,
        tickStepBeat: Double,
    ) {
        activeHolds[noteId] = ActiveHoldState(
            noteId = noteId,
            endBeat = endBeat,
            tickStepBeat = tickStepBeat,
            nextTickBeat = startBeat + tickStepBeat,
        )
        lastJudgment = judgment
        lastJudgmentTimeMs = audioTimeMs
        score += judgment.score
        combo += 1
        maxCombo = maxOf(maxCombo, combo)
        recoverHealth()
    }

    fun scoreHoldTicks(noteId: Long, currentBeat: Double, audioTimeMs: Long): Int {
        val holdState = activeHolds[noteId] ?: return 0
        var scoredTickCount = 0
        while (holdState.nextTickBeat <= currentBeat && holdState.nextTickBeat <= holdState.endBeat) {
            score += HOLD_TICK_SCORE
            combo += 1
            maxCombo = maxOf(maxCombo, combo)
            scoredTickCount += 1
            holdState.nextTickBeat += holdState.tickStepBeat
        }
        if (scoredTickCount > 0) {
            lastJudgment = JudgmentResult.Perfect
            lastJudgmentTimeMs = audioTimeMs
        }
        return scoredTickCount
    }

    fun completeHold(noteId: Long) {
        activeHolds.remove(noteId)
        judgedNoteIds.add(noteId)
    }

    private fun recoverHealth() {
        health = (health + HIT_HEAL).coerceAtMost(MAX_HEALTH)
    }

    companion object {
        const val MAX_HEALTH = 100
        const val MISS_DAMAGE = 10
        const val HIT_HEAL = 2
        const val HOLD_TICK_SCORE = 100
    }
}
