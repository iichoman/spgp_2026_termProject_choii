package com.example.myrhythmgame.game.play

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayStateTest {
    @Test
    fun holdTicksIncreaseScoreAndComboUntilEndBeat() {
        val state = PlayState()

        state.startHold(
            noteId = 1L,
            judgment = JudgmentResult.Great,
            audioTimeMs = 1000L,
            startBeat = 4.0,
            endBeat = 5.0,
            tickStepBeat = 0.25,
        )

        assertTrue(state.isActiveHold(1L))
        assertEquals(JudgmentResult.Great.score, state.score)
        assertEquals(1, state.combo)

        val scoredTicks = state.scoreHoldTicks(1L, currentBeat = 4.75, audioTimeMs = 1400L)

        assertEquals(3, scoredTicks)
        assertEquals(JudgmentResult.Great.score + PlayState.HOLD_TICK_SCORE * 3, state.score)
        assertEquals(4, state.combo)
        assertEquals(4, state.maxCombo)
    }

    @Test
    fun holdTicksDoNotScorePastEndBeat() {
        val state = PlayState()

        state.startHold(
            noteId = 1L,
            judgment = JudgmentResult.Perfect,
            audioTimeMs = 1000L,
            startBeat = 4.0,
            endBeat = 4.5,
            tickStepBeat = 0.25,
        )

        val scoredTicks = state.scoreHoldTicks(1L, currentBeat = 6.0, audioTimeMs = 2000L)

        assertEquals(2, scoredTicks)
        assertEquals(JudgmentResult.Perfect.score + PlayState.HOLD_TICK_SCORE * 2, state.score)
        assertEquals(3, state.combo)
    }

    @Test
    fun missClearsActiveHoldAndResetsCombo() {
        val state = PlayState()

        state.startHold(
            noteId = 1L,
            judgment = JudgmentResult.Good,
            audioTimeMs = 1000L,
            startBeat = 4.0,
            endBeat = 5.0,
            tickStepBeat = 0.25,
        )

        state.applyMiss(1L, audioTimeMs = 1200L)

        assertFalse(state.isActiveHold(1L))
        assertTrue(state.isJudged(1L))
        assertEquals(0, state.combo)
        assertEquals(JudgmentResult.Miss, state.lastJudgment)
        assertEquals(PlayState.MAX_HEALTH - PlayState.MISS_DAMAGE, state.health)
    }

    @Test
    fun repeatedMissForSameNoteOnlyDamagesHealthOnce() {
        val state = PlayState()

        state.applyMiss(noteId = 1L, audioTimeMs = 1000L)
        state.applyMiss(noteId = 1L, audioTimeMs = 1100L)

        assertEquals(PlayState.MAX_HEALTH - PlayState.MISS_DAMAGE, state.health)
    }

    @Test
    fun healthNeverDropsBelowZero() {
        val state = PlayState()

        repeat(PlayState.MAX_HEALTH / PlayState.MISS_DAMAGE + 2) { index ->
            state.applyMiss(noteId = index.toLong(), audioTimeMs = index.toLong())
        }

        assertEquals(0, state.health)
        assertTrue(state.isHealthDepleted)
    }

    @Test
    fun successfulHitRecoversHealthWithoutExceedingMaximum() {
        val state = PlayState(health = PlayState.MAX_HEALTH - 1)

        state.applyHit(noteId = 1L, judgment = JudgmentResult.Perfect, audioTimeMs = 1000L)

        assertEquals(PlayState.MAX_HEALTH, state.health)
    }

    @Test
    fun holdStartRecoversHealthButHoldTicksDoNot() {
        val state = PlayState(health = 50)

        state.startHold(
            noteId = 1L,
            judgment = JudgmentResult.Great,
            audioTimeMs = 1000L,
            startBeat = 4.0,
            endBeat = 5.0,
            tickStepBeat = 0.25,
        )
        val healthAfterStart = state.health
        state.scoreHoldTicks(noteId = 1L, currentBeat = 4.75, audioTimeMs = 1400L)

        assertEquals(50 + PlayState.HIT_HEAL, healthAfterStart)
        assertEquals(healthAfterStart, state.health)
    }
}
