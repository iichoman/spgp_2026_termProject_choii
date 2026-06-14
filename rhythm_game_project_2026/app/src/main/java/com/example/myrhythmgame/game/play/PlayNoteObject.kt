package com.example.myrhythmgame.game.play

import android.graphics.Canvas
import android.graphics.Paint
import com.example.myrhythmgame.rhythm.chart.ChartNote
import com.example.myrhythmgame.rhythm.chart.HoldNote
import com.example.myrhythmgame.rhythm.lane.LaneMapping
import kr.ac.tukorea.ge.spgp2026.a2dg.objects.IGameObject
import kr.ac.tukorea.ge.spgp2026.a2dg.view.GameContext

class PlayNoteObject(
    val note: ChartNote,
    private val laneMapping: LaneMapping,
    private val noteTimeMsProvider: (ChartNote) -> Long,
    private val noteEndTimeMsProvider: (ChartNote) -> Long,
    private val currentTimeMsProvider: () -> Long,
    private val widthProvider: () -> Float,
    private val heightProvider: () -> Float,
    private val judgmentYProvider: () -> Float,
    private val pixelsPerMsProvider: () -> Float,
    private val shouldTrimHoldProvider: (Long) -> Boolean,
    private val isMissedProvider: (Long) -> Boolean,
    private val notePaint: Paint,
    private val borderPaint: Paint,
    private val missedNotePaint: Paint,
    private val missedBorderPaint: Paint,
) : IGameObject {
    override fun update(gctx: GameContext) = Unit

    override fun draw(canvas: Canvas) {
        val width = widthProvider()
        val height = heightProvider()
        val startY = yForTime(noteTimeMsProvider(note), currentTimeMsProvider(), judgmentYProvider())
        val endY = yForTime(noteEndTimeMsProvider(note), currentTimeMsProvider(), judgmentYProvider())
        if (maxOf(startY, endY) < -NOTE_HEIGHT || minOf(startY, endY) > height + NOTE_HEIGHT) return

        val laneWidth = width / laneMapping.laneCount
        val noteWidth = laneWidth * NOTE_WIDTH_RATIO
        val centerX = laneMapping.snapX(note.normalizedX) * width
        val left = centerX - noteWidth / 2f
        val right = centerX + noteWidth / 2f
        val activeHoldBottomY = judgmentYProvider()
        val top = if (note is HoldNote) {
            minOf(startY, endY) - NOTE_HEIGHT / 2f
        } else {
            startY - NOTE_HEIGHT / 2f
        }
        val bottom = if (note is HoldNote) {
            val fullBottom = maxOf(startY, endY) + NOTE_HEIGHT / 2f
            if (shouldTrimHoldProvider(note.id)) {
                minOf(fullBottom, activeHoldBottomY)
            } else {
                fullBottom
            }
        } else {
            startY + NOTE_HEIGHT / 2f
        }
        if (bottom <= top) return

        val fillPaint = if (isMissedProvider(note.id)) missedNotePaint else notePaint
        val outlinePaint = if (isMissedProvider(note.id)) missedBorderPaint else borderPaint
        canvas.drawRect(left, top, right, bottom, fillPaint)
        canvas.drawRect(left, top, right, bottom, outlinePaint)
    }

    private fun yForTime(timeMs: Long, currentTimeMs: Long, judgmentY: Float): Float {
        val timeDiffMs = timeMs - currentTimeMs
        return judgmentY - timeDiffMs * pixelsPerMsProvider()
    }

    companion object {
        private const val NOTE_WIDTH_RATIO = 0.82f
        private const val NOTE_HEIGHT = 38f
    }
}
