package com.example.myrhythmgame.game.play

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import android.view.LayoutInflater
import android.view.View
import android.util.SparseIntArray
import com.example.myrhythmgame.R
import com.example.myrhythmgame.app.UiOverlayHost
import com.example.myrhythmgame.databinding.OverlayPlayDialogBinding
import com.example.myrhythmgame.rhythm.audio.AndroidMediaAudioClock
import com.example.myrhythmgame.rhythm.audio.AudioClockException
import com.example.myrhythmgame.rhythm.audio.AudioLatencyConfig
import com.example.myrhythmgame.rhythm.audio.AudioSource
import com.example.myrhythmgame.rhythm.audio.NoteSoundPlayer
import com.example.myrhythmgame.rhythm.audio.PlaybackState
import com.example.myrhythmgame.rhythm.audio.SoundEffectKey
import com.example.myrhythmgame.rhythm.audio.SoundPoolNoteSoundPlayer
import com.example.myrhythmgame.rhythm.chart.Chart
import com.example.myrhythmgame.rhythm.chart.ChartNote
import com.example.myrhythmgame.rhythm.chart.HoldNote
import com.example.myrhythmgame.rhythm.lane.EqualDivisionLaneMapping
import com.example.myrhythmgame.rhythm.lane.LaneMapping
import com.example.myrhythmgame.rhythm.lane.LaneMode
import com.example.myrhythmgame.rhythm.storage.ChartRepository
import com.example.myrhythmgame.rhythm.storage.ChartStorageException
import com.example.myrhythmgame.rhythm.settings.AudioSettingsRepository
import kr.ac.tukorea.ge.spgp2026.a2dg.scene.Scene
import kr.ac.tukorea.ge.spgp2026.a2dg.scene.World
import kr.ac.tukorea.ge.spgp2026.a2dg.view.GameContext

class PlayScene(
    gctx: GameContext,
    private val chartFileName: String,
) : Scene(gctx) {
    private enum class Layer {
        Note,
    }

    private enum class PlayMode {
        Playing,
        Paused,
        Countdown,
        Result,
    }

    override val clipsRect = true
    private val noteWorld = World(arrayOf(Layer.Note))
    override val world: World<*> = noteWorld

    private val audioClock = AndroidMediaAudioClock(gctx.view.context)
    private val noteSoundPlayer: NoteSoundPlayer = SoundPoolNoteSoundPlayer(
        context = gctx.view.context,
        soundResources = mapOf(SoundEffectKey.LaneTouch to R.raw.note_tap),
    )
    private val chartRepository = ChartRepository(gctx.view.context)
    private val audioSettingsRepository = AudioSettingsRepository(gctx.view.context)
    private val latencyConfig = AudioLatencyConfig()
    private var laneMapping: LaneMapping = EqualDivisionLaneMapping(LaneMode.Default.laneCount)
    private val playState = PlayState()
    private val activeLaneByPointerId = SparseIntArray()
    private val activeNoteObjects = mutableMapOf<Long, PlayNoteObject>()

    private var chart: Chart? = null
    private var errorMessage: String? = null
    private var resumesAfterPause = false
    private var noteSpeed = DEFAULT_NOTE_SPEED
    private var playMode = PlayMode.Playing
    private var countdownRemainingSec = 0f
    private var cleared = false
    private val overlayHost: UiOverlayHost
        get() = gctx.view.context as UiOverlayHost

    private val backgroundPaint = Paint().apply {
        style = Paint.Style.FILL
        color = Color.BLACK
    }

    private val lanePaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = Color.rgb(52, 60, 76)
    }

    private val beatPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = Color.rgb(38, 76, 96)
    }

    private val downBeatPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        color = Color.rgb(70, 124, 152)
    }

    private val laneFeedbackPaint = Paint().apply {
        style = Paint.Style.FILL
        color = Color.argb(96, 80, 170, 255)
        isAntiAlias = true
    }

    private val judgmentLinePaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 8f
        color = Color.rgb(255, 214, 92)
    }

    private val notePaint = Paint().apply {
        style = Paint.Style.FILL
        color = Color.rgb(87, 190, 255)
        isAntiAlias = true
    }

    private val noteBorderPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = Color.rgb(235, 246, 255)
        isAntiAlias = true
    }

    private val missPaint = Paint().apply {
        style = Paint.Style.FILL
        color = Color.rgb(90, 96, 110)
        isAntiAlias = true
    }

    private val missBorderPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = Color.rgb(155, 160, 170)
        isAntiAlias = true
    }

    private val scorePaint = Paint().apply {
        color = Color.rgb(226, 232, 244)
        textSize = 38f
        textAlign = Paint.Align.RIGHT
        isAntiAlias = true
    }

    private val healthBackgroundPaint = Paint().apply {
        style = Paint.Style.FILL
        color = Color.rgb(48, 48, 48)
    }

    private val healthPaint = Paint().apply {
        style = Paint.Style.FILL
        color = Color.rgb(80, 210, 125)
    }

    private val healthBorderPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = Color.WHITE
    }

    private val judgmentPaint = Paint().apply {
        color = Color.rgb(255, 214, 92)
        textSize = 58f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }

    private val comboPaint = Paint().apply {
        color = Color.rgb(235, 239, 247)
        textSize = 40f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }

    private val errorPaint = Paint().apply {
        color = Color.rgb(255, 112, 112)
        textSize = 30f
        isAntiAlias = true
    }

    private val pauseDimPaint = Paint().apply {
        color = Color.argb(190, 0, 0, 0)
    }

    private val countdownPaint = Paint().apply {
        color = Color.WHITE
        textSize = 150f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }

    override fun onEnter() {
        loadChartAndAudio()
    }

    override fun onPause() {
        resumesAfterPause = audioClock.isPlaying
        audioClock.pause()
    }

    override fun onResume() {
        if (
            resumesAfterPause &&
            (playMode == PlayMode.Playing || playMode == PlayMode.Result) &&
            audioClock.isPrepared
        ) {
            audioClock.play()
        }
    }

    override fun onExit() {
        overlayHost.clearOverlay()
        noteSoundPlayer.release()
        audioClock.release()
        activeNoteObjects.clear()
    }

    override fun update(gctx: GameContext) {
        if (playMode == PlayMode.Paused || playMode == PlayMode.Result) return
        if (playMode == PlayMode.Countdown) {
            updateCountdown(gctx.frameTime)
            return
        }

        val currentTimeMs = audioClock.positionMs
        spawnNoteObjects(currentTimeMs)
        updateMissedNotes(currentTimeMs)
        updateActiveHoldNotes(currentTimeMs)
        removeInactiveNoteObjects(currentTimeMs)
        noteWorld.update(gctx)
        if (
            playState.isHealthDepleted ||
            audioClock.state == PlaybackState.Completed ||
            hasNoRemainingNotes()
        ) {
            finishPlay(cleared = !playState.isHealthDepleted)
        }
    }

    override fun draw(canvas: Canvas) {
        val width = gctx.metrics.width
        val height = gctx.metrics.height
        val judgmentY = height * JUDGMENT_LINE_RATIO
        val currentTimeMs = audioClock.positionMs

        canvas.drawRect(0f, 0f, width, height, backgroundPaint)
        chart?.let { drawBeatGrid(canvas, it, currentTimeMs, width, height, judgmentY) }
        drawActiveLaneFeedback(canvas, width, height)
        drawLaneGuides(canvas, width, height)
        canvas.drawLine(0f, judgmentY, width, judgmentY, judgmentLinePaint)
        drawHealthBar(canvas, width)
        drawScore(canvas, width)

        noteWorld.draw(canvas)

        drawJudgment(canvas, width)
        drawCountdown(canvas, width, height)

        errorMessage?.let {
            canvas.drawText(text(R.string.error_format, it), 38f, 248f, errorPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (playMode != PlayMode.Playing) {
            return true
        }

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val index = event.actionIndex
                val lane = laneOfScreenX(event.getX(index))
                activeLaneByPointerId.put(event.getPointerId(index), lane)
                noteSoundPlayer.play(SoundEffectKey.LaneTouch)
                judgeTouch(event.getX(index), event.getY(index))
            }

            MotionEvent.ACTION_MOVE -> {
                updateActivePointerLanes(event)
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                activeLaneByPointerId.delete(event.getPointerId(event.actionIndex))
            }

        MotionEvent.ACTION_CANCEL -> {
                activeLaneByPointerId.clear()
                breakActiveHolds(audioClock.positionMs)
            }
        }
        updateActiveHoldNotes(audioClock.positionMs)
        return true
    }

    override fun onBackPressed(): Boolean {
        if (playMode == PlayMode.Playing) {
            pausePlay()
        }
        return true
    }

    private fun loadChartAndAudio() {
        try {
            val audioSettings = audioSettingsRepository.load()
            audioClock.setVolume(audioSettings.musicVolume)
            noteSoundPlayer.setVolume(audioSettings.effectVolume)
            val loadedChart = chartRepository.load(chartFileName)
            if (loadedChart == null) {
                errorMessage = text(R.string.error_chart_not_found, chartFileName)
                return
            }
            chart = loadedChart
            laneMapping = EqualDivisionLaneMapping(loadedChart.metadata.laneCount)
            audioClock.load(AudioSource.AssetSource(loadedChart.song.audioPath))
            audioClock.play()
            errorMessage = null
        } catch (e: ChartStorageException) {
            errorMessage = e.message
        } catch (e: AudioClockException) {
            errorMessage = e.message
        }
    }

    private fun drawScore(canvas: Canvas, width: Float) {
        canvas.drawText(playState.score.toString(), width - SCORE_RIGHT_MARGIN, SCORE_TEXT_Y, scorePaint)
    }

    private fun drawHealthBar(canvas: Canvas, width: Float) {
        val bounds = RectF(HEALTH_MARGIN, HEALTH_TOP, width - HEALTH_MARGIN, HEALTH_BOTTOM)
        val ratio = playState.health.toFloat() / PlayState.MAX_HEALTH
        val fill = RectF(bounds.left, bounds.top, bounds.left + bounds.width() * ratio, bounds.bottom)
        healthPaint.color = when {
            ratio > 0.5f -> Color.rgb(80, 210, 125)
            ratio > 0.25f -> Color.rgb(255, 200, 80)
            else -> Color.rgb(255, 90, 90)
        }
        canvas.drawRect(bounds, healthBackgroundPaint)
        canvas.drawRect(fill, healthPaint)
        canvas.drawRect(bounds, healthBorderPaint)
    }

    private fun drawCountdown(canvas: Canvas, width: Float, height: Float) {
        if (playMode != PlayMode.Countdown) return
        canvas.drawRect(0f, 0f, width, height, pauseDimPaint)
        val count = kotlin.math.ceil(countdownRemainingSec).toInt().coerceAtLeast(1)
        canvas.drawText(count.toString(), width / 2f, height / 2f + 50f, countdownPaint)
    }

    private fun pausePlay() {
        playMode = PlayMode.Paused
        audioClock.pause()
        activeLaneByPointerId.clear()
        showPauseOverlay()
    }

    private fun finishPlay(cleared: Boolean) {
        if (playMode == PlayMode.Result) return
        this.cleared = cleared
        playMode = PlayMode.Result
        if (!cleared) {
            audioClock.pause()
        }
        activeLaneByPointerId.clear()
        showResultOverlay()
    }

    private fun hasNoRemainingNotes(): Boolean {
        val loadedChart = chart ?: return false
        return loadedChart.notes.all { playState.isJudged(it.id) }
    }

    private fun startResumeCountdown() {
        overlayHost.clearOverlay()
        playMode = PlayMode.Countdown
        countdownRemainingSec = RESUME_COUNTDOWN_SECONDS
    }

    private fun showPauseOverlay() {
        val binding = OverlayPlayDialogBinding.inflate(LayoutInflater.from(gctx.view.context))
        binding.titleText.setText(R.string.pause_title)
        binding.primaryButton.setText(R.string.action_resume)
        binding.secondaryButton.setText(R.string.action_exit)
        binding.primaryButton.setOnClickListener { startResumeCountdown() }
        binding.secondaryButton.setOnClickListener { pop() }
        overlayHost.showOverlay(binding.root)
    }

    private fun showResultOverlay() {
        val binding = OverlayPlayDialogBinding.inflate(LayoutInflater.from(gctx.view.context))
        binding.titleText.setText(if (cleared) R.string.result_cleared else R.string.result_failed)
        binding.scoreText.visibility = View.VISIBLE
        binding.maxComboText.visibility = View.VISIBLE
        binding.scoreText.text = text(R.string.result_score_format, playState.score)
        binding.maxComboText.text = text(R.string.result_max_combo_format, playState.maxCombo)
        binding.primaryButton.setText(R.string.action_retry)
        binding.secondaryButton.setText(R.string.action_menu)
        binding.primaryButton.setOnClickListener { PlayScene(gctx, chartFileName).change() }
        binding.secondaryButton.setOnClickListener { pop() }
        overlayHost.showOverlay(binding.root)
    }

    private fun updateCountdown(frameTime: Float) {
        countdownRemainingSec -= frameTime
        if (countdownRemainingSec > 0f) return

        countdownRemainingSec = 0f
        playMode = PlayMode.Playing
        if (audioClock.isPrepared) {
            audioClock.play()
        }
    }

    private fun drawLaneGuides(canvas: Canvas, width: Float, height: Float) {
        for (i in 1 until laneMapping.laneCount) {
            val x = width * i / laneMapping.laneCount
            canvas.drawLine(x, 0f, x, height, lanePaint)
        }
    }

    private fun drawActiveLaneFeedback(canvas: Canvas, width: Float, height: Float) {
        val laneWidth = width / laneMapping.laneCount
        val activeLanes = activeLanes()
        for (lane in activeLanes) {
            val left = lane * laneWidth
            canvas.drawRect(left, 0f, left + laneWidth, height, laneFeedbackPaint)
        }
    }

    private fun drawBeatGrid(
        canvas: Canvas,
        chart: Chart,
        currentTimeMs: Long,
        width: Float,
        height: Float,
        judgmentY: Float,
    ) {
        val currentBeat = chart.timing.beatAt(currentTimeMs)
        val firstBeat = kotlin.math.floor(currentBeat - BEATS_BEHIND).toInt()
        val lastBeat = kotlin.math.ceil(currentBeat + BEATS_AHEAD).toInt()

        for (beat in firstBeat..lastBeat) {
            val beatTimeMs = chart.timing.timeAtBeat(beat.toDouble())
            val y = yForTime(beatTimeMs, currentTimeMs, judgmentY)
            if (y < 0f || y > height) continue

            val paint = if (beat % 4 == 0) downBeatPaint else beatPaint
            canvas.drawLine(0f, y, width, y, paint)
        }
    }

    private fun drawJudgment(canvas: Canvas, width: Float) {
        val judgment = playState.lastJudgment ?: return
        val ageMs = audioClock.positionMs - playState.lastJudgmentTimeMs
        if (ageMs !in 0L..JUDGMENT_TEXT_DURATION_MS) return

        judgmentPaint.color = when (judgment) {
            JudgmentResult.Perfect -> Color.rgb(255, 236, 171)
            JudgmentResult.Great -> Color.rgb(120, 210, 160)
            JudgmentResult.Good -> Color.rgb(87, 190, 255)
            JudgmentResult.Miss -> Color.rgb(255, 112, 112)
        }
        canvas.drawText(judgmentText(judgment), width / 2f, JUDGMENT_TEXT_Y, judgmentPaint)
        if (playState.combo > 0) {
            canvas.drawText(text(R.string.combo_format, playState.combo), width / 2f, COMBO_TEXT_Y, comboPaint)
        }
    }

    private fun judgeTouch(screenX: Float, screenY: Float) {
        val loadedChart = chart ?: return
        val point = gctx.metrics.fromScreen(screenX, screenY)
        val inputLane = laneMapping.laneOf(point.x / gctx.metrics.width)
        val inputTimeMs = latencyConfig.judgmentTimeFor(audioClock.positionMs)
        val target = findClosestTouchableNote(loadedChart, inputLane, inputTimeMs) ?: return
        val deltaMs = inputTimeMs - noteTimeMs(loadedChart, target)
        val judgment = JudgmentResult.fromDelta(deltaMs) ?: return

        if (target is HoldNote) {
            playState.startHold(
                noteId = target.id,
                judgment = judgment,
                audioTimeMs = audioClock.positionMs,
                startBeat = noteStartBeat(loadedChart, target),
                endBeat = noteEndBeat(loadedChart, target),
                tickStepBeat = HOLD_TICK_STEP_BEAT,
            )
        } else {
            playState.applyHit(target.id, judgment, audioClock.positionMs)
            removeNoteObject(target.id)
        }
        playNoteEffect(inputLane, judgment)
    }

    private fun updateActivePointerLanes(event: MotionEvent) {
        for (index in 0 until event.pointerCount) {
            activeLaneByPointerId.put(
                event.getPointerId(index),
                laneOfScreenX(event.getX(index)),
            )
        }
    }

    private fun activeLanes(): Set<Int> {
        val lanes = mutableSetOf<Int>()
        for (index in 0 until activeLaneByPointerId.size()) {
            lanes.add(activeLaneByPointerId.valueAt(index))
        }
        return lanes
    }

    private fun laneOfScreenX(screenX: Float): Int {
        val point = gctx.metrics.fromScreen(screenX, 0f)
        return laneMapping.laneOf(point.x / gctx.metrics.width)
    }

    private fun findClosestTouchableNote(
        chart: Chart,
        lane: Int,
        inputTimeMs: Long,
    ): ChartNote? {
        return chart.notes
            .asSequence()
            .filter { !playState.isJudged(it.id) }
            .filter { !playState.isActiveHold(it.id) }
            .filter { laneMapping.laneOf(it.normalizedX) == lane }
            .map { note -> note to inputTimeMs - noteTimeMs(chart, note) }
            .filter { (_, deltaMs) -> kotlin.math.abs(deltaMs) <= JudgmentResult.Good.windowMs }
            .minByOrNull { (_, deltaMs) -> kotlin.math.abs(deltaMs) }
            ?.first
    }

    private fun updateMissedNotes(currentTimeMs: Long) {
        val loadedChart = chart ?: return
        for (note in loadedChart.notes) {
            if (playState.isJudged(note.id) || playState.isActiveHold(note.id)) continue
            val noteTimeMs = noteTimeMs(loadedChart, note)
            if (currentTimeMs - noteTimeMs > MISS_GRACE_MS) {
                playState.applyMiss(note.id, currentTimeMs)
                if (note !is HoldNote) {
                    removeNoteObject(note.id)
                }
                playNoteEffect(laneMapping.laneOf(note.normalizedX), JudgmentResult.Miss)
            }
        }
    }

    private fun updateActiveHoldNotes(currentTimeMs: Long) {
        val loadedChart = chart ?: return
        val activeLanes = activeLanes()
        val activeHoldNotes = loadedChart.notes
            .asSequence()
            .filterIsInstance<HoldNote>()
            .filter { playState.isActiveHold(it.id) }
            .toList()

        for (note in activeHoldNotes) {
            val lane = laneMapping.laneOf(note.normalizedX)
            val endTimeMs = noteEndTimeMs(loadedChart, note)
            if (currentTimeMs >= endTimeMs) {
                playState.scoreHoldTicks(note.id, noteEndBeat(loadedChart, note), currentTimeMs)
                playState.completeHold(note.id)
                removeNoteObject(note.id)
            } else if (lane !in activeLanes) {
                playState.applyMiss(note.id, currentTimeMs)
                playNoteEffect(lane, JudgmentResult.Miss)
            } else {
                playState.scoreHoldTicks(note.id, loadedChart.timing.beatAt(currentTimeMs), currentTimeMs)
            }
        }
    }

    private fun breakActiveHolds(currentTimeMs: Long) {
        val loadedChart = chart ?: return
        val activeHoldNotes = loadedChart.notes
            .asSequence()
            .filterIsInstance<HoldNote>()
            .filter { playState.isActiveHold(it.id) }
            .toList()

        for (note in activeHoldNotes) {
            playState.applyMiss(note.id, currentTimeMs)
            playNoteEffect(laneMapping.laneOf(note.normalizedX), JudgmentResult.Miss)
        }
    }

    private fun spawnNoteObjects(currentTimeMs: Long) {
        val loadedChart = chart ?: return
        for (note in loadedChart.notes) {
            if (shouldSkipSpawning(note) || note.id in activeNoteObjects) continue

            val noteTimeMs = noteTimeMs(loadedChart, note)
            if (noteTimeMs !in (currentTimeMs - SPAWN_BEHIND_MS)..(currentTimeMs + SPAWN_AHEAD_MS)) continue
            if (!isNoteVisible(loadedChart, note, currentTimeMs)) continue

            val noteObject = PlayNoteObject(
                note = note,
                laneMapping = laneMapping,
                noteTimeMsProvider = { noteTimeMs(loadedChart, it) },
                noteEndTimeMsProvider = { noteEndTimeMs(loadedChart, it) },
                currentTimeMsProvider = { audioClock.positionMs },
                widthProvider = { gctx.metrics.width },
                heightProvider = { gctx.metrics.height },
                judgmentYProvider = { gctx.metrics.height * JUDGMENT_LINE_RATIO },
                pixelsPerMsProvider = ::pixelsPerMs,
                shouldTrimHoldProvider = { playState.isActiveHold(it) || playState.isMissed(it) },
                isMissedProvider = { playState.isMissed(it) },
                notePaint = notePaint,
                borderPaint = noteBorderPaint,
                missedNotePaint = missPaint,
                missedBorderPaint = missBorderPaint,
            )
            activeNoteObjects[note.id] = noteObject
            noteWorld.add(noteObject, Layer.Note)
        }
    }

    private fun removeInactiveNoteObjects(currentTimeMs: Long) {
        val loadedChart = chart ?: return
        val expiredNoteIds = activeNoteObjects.values
            .asSequence()
            .filter {
                shouldRemoveJudgedNote(it.note) ||
                        currentTimeMs - noteEndTimeMs(loadedChart, it.note) > DESPAWN_AFTER_MS ||
                        !isNoteVisible(loadedChart, it.note, currentTimeMs)
            }
            .map { it.note.id }
            .toList()

        for (noteId in expiredNoteIds) {
            removeNoteObject(noteId)
        }
    }

    private fun shouldSkipSpawning(note: ChartNote): Boolean {
        if (!playState.isJudged(note.id)) return false
        return note !is HoldNote || !playState.isMissed(note.id)
    }

    private fun shouldRemoveJudgedNote(note: ChartNote): Boolean {
        if (!playState.isJudged(note.id)) return false
        return note !is HoldNote || !playState.isMissed(note.id)
    }

    private fun removeNoteObject(noteId: Long) {
        val noteObject = activeNoteObjects.remove(noteId) ?: return
        noteWorld.remove(noteObject, Layer.Note)
    }

    private fun noteTimeMs(chart: Chart, note: ChartNote): Long {
        val beat = note.snappedBeat ?: return note.rawTimeMs
        return chart.timing.timeAtBeat(beat)
    }

    private fun noteEndTimeMs(chart: Chart, note: ChartNote): Long {
        val endBeat = note.snappedEndBeat
        if (endBeat != null) return chart.timing.timeAtBeat(endBeat)
        return note.rawEndTimeMs ?: noteTimeMs(chart, note)
    }

    private fun noteStartBeat(chart: Chart, note: ChartNote): Double {
        return note.snappedBeat ?: chart.timing.beatAt(note.rawTimeMs)
    }

    private fun noteEndBeat(chart: Chart, note: ChartNote): Double {
        val rawEndTimeMs = note.rawEndTimeMs ?: note.rawTimeMs
        return note.snappedEndBeat ?: chart.timing.beatAt(rawEndTimeMs)
    }

    private fun isNoteVisible(chart: Chart, note: ChartNote, currentTimeMs: Long): Boolean {
        val judgmentY = gctx.metrics.height * JUDGMENT_LINE_RATIO
        val startY = yForTime(noteTimeMs(chart, note), currentTimeMs, judgmentY)
        val endY = yForTime(noteEndTimeMs(chart, note), currentTimeMs, judgmentY)
        val top = if (note is HoldNote) minOf(startY, endY) else startY
        val bottom = if (note is HoldNote) maxOf(startY, endY) else startY
        return bottom >= -NOTE_VISIBILITY_PADDING && top <= gctx.metrics.height + NOTE_VISIBILITY_PADDING
    }

    private fun yForTime(timeMs: Long, currentTimeMs: Long, judgmentY: Float): Float {
        val timeDiffMs = timeMs - currentTimeMs
        return judgmentY - timeDiffMs * pixelsPerMs()
    }

    private fun pixelsPerMs(): Float {
        return BASE_PIXELS_PER_MS * noteSpeed
    }

    private fun playNoteEffect(lane: Int, judgment: JudgmentResult) {

    }

    private fun judgmentText(judgment: JudgmentResult): String {
        return text(
            when (judgment) {
                JudgmentResult.Perfect -> R.string.judgment_perfect
                JudgmentResult.Great -> R.string.judgment_great
                JudgmentResult.Good -> R.string.judgment_good
                JudgmentResult.Miss -> R.string.judgment_miss
            }
        )
    }

    private fun text(resId: Int, vararg args: Any): String {
        return gctx.view.context.getString(resId, *args)
    }

    companion object {
        private const val JUDGMENT_LINE_RATIO = 0.78f
        private const val BASE_PIXELS_PER_MS = 0.32f
        private const val DEFAULT_NOTE_SPEED = 4.3f
        private const val BEATS_BEHIND = 2.0
        private const val BEATS_AHEAD = 8.0
        private const val SPAWN_AHEAD_MS = 3000L
        private const val SPAWN_BEHIND_MS = 250L
        private const val DESPAWN_AFTER_MS = 300L
        private const val MISS_GRACE_MS = 140L
        private const val NOTE_VISIBILITY_PADDING = 38f
        private const val HOLD_TICK_STEP_BEAT = 0.25
        private const val JUDGMENT_TEXT_DURATION_MS = 550L
        private const val JUDGMENT_TEXT_Y = 370f
        private const val COMBO_TEXT_Y = 420f
        private const val SCORE_TEXT_Y = 100f
        private const val HEALTH_MARGIN = 24f
        private const val HEALTH_TOP = 18f
        private const val HEALTH_BOTTOM = 48f
        private const val SCORE_RIGHT_MARGIN = 32f
        private const val RESUME_COUNTDOWN_SECONDS = 3f
    }
}
