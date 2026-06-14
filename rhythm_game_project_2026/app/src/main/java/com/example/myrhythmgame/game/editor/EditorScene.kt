package com.example.myrhythmgame.game.editor

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.text.InputType
import android.view.MotionEvent
import android.view.LayoutInflater
import android.view.View
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.SeekBar
import android.widget.EditText
import com.example.myrhythmgame.R
import com.example.myrhythmgame.databinding.OverlayEditorControlsBinding
import com.example.myrhythmgame.game.BindingOverlayScene
import com.example.myrhythmgame.game.play.PlayScene
import com.example.myrhythmgame.rhythm.audio.AndroidMediaAudioClock
import com.example.myrhythmgame.rhythm.audio.AudioClockException
import com.example.myrhythmgame.rhythm.audio.AudioLatencyConfig
import com.example.myrhythmgame.rhythm.audio.AudioSource
import com.example.myrhythmgame.rhythm.chart.Chart
import com.example.myrhythmgame.rhythm.chart.ChartNote
import com.example.myrhythmgame.rhythm.chart.HoldNote
import com.example.myrhythmgame.rhythm.chart.SongMetadata
import com.example.myrhythmgame.rhythm.input.RecordedTouchEvent
import com.example.myrhythmgame.rhythm.input.LaneBeatDuplicatePolicy
import com.example.myrhythmgame.rhythm.input.TouchAction
import com.example.myrhythmgame.rhythm.input.TouchRecorder
import com.example.myrhythmgame.rhythm.lane.EqualDivisionLaneMapping
import com.example.myrhythmgame.rhythm.lane.LaneMapping
import com.example.myrhythmgame.rhythm.lane.LaneMode
import com.example.myrhythmgame.rhythm.storage.ChartRepository
import com.example.myrhythmgame.rhythm.storage.ChartStorageException
import com.example.myrhythmgame.rhythm.settings.AudioSettingsRepository
import com.example.myrhythmgame.rhythm.timing.Snapper
import com.example.myrhythmgame.rhythm.timing.TimingMap
import kr.ac.tukorea.ge.spgp2026.a2dg.view.GameContext
import java.util.Locale

class EditorScene(
    gctx: GameContext,
    private val audioPath: String = TEST_AUDIO_ASSET,
    private val chartFileName: String = CHART_FILE_NAME,
) : BindingOverlayScene(gctx) {
    override val clipsRect = true

    private val audioClock = AndroidMediaAudioClock(gctx.view.context)
    private val chartRepository = ChartRepository(gctx.view.context)
    private val audioSettingsRepository = AudioSettingsRepository(gctx.view.context)
    private val latencyConfig = AudioLatencyConfig()
    private var laneMapping: LaneMapping = EqualDivisionLaneMapping(LaneMode.Default.laneCount)
    private val editorState = EditorState()

    private var chart = createDefaultChart()
    private var snapper = Snapper(chart.timing)
    private var touchRecorder = createTouchRecorder()

    private var errorMessage: String? = null
    private var chartStatus = text(R.string.status_chart_new)
    private var resumesAfterPause = false
    private var noteSpeed = DEFAULT_NOTE_SPEED
    private var recordingPreview: RecordingNotePreview? = null
    private var controlsBinding: OverlayEditorControlsBinding? = null
    private var isSeekingWithBinding = false

    private val backgroundPaint = Paint().apply {
        style = Paint.Style.FILL
        color = Color.BLACK
    }

    private val lanePaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = Color.rgb(58, 65, 80)
    }

    private val beatPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = Color.rgb(45, 86, 108)
    }

    private val snapGridPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 1f
        color = Color.rgb(34, 54, 68)
    }

    private val downBeatPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        color = Color.rgb(78, 134, 162)
    }

    private val judgmentLinePaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 8f
        color = Color.rgb(255, 214, 92)
    }

    private val textPaint = Paint().apply {
        color = Color.rgb(230, 234, 242)
        textSize = 34f
        isAntiAlias = true
    }

    private val notePaint = Paint().apply {
        style = Paint.Style.FILL
        color = Color.rgb(87, 190, 255)
        isAntiAlias = true
    }

    private val snappedNotePaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = Color.rgb(255, 255, 255)
        isAntiAlias = true
    }

    private val selectedNotePaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 6f
        color = Color.rgb(255, 214, 92)
        isAntiAlias = true
    }

    private val rawNotePaint = Paint().apply {
        style = Paint.Style.FILL
        color = Color.rgb(255, 112, 112)
        isAntiAlias = true
    }

    private val previewNotePaint = Paint().apply {
        style = Paint.Style.FILL
        color = Color.argb(150, 120, 210, 160)
        isAntiAlias = true
    }

    private val previewBorderPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = Color.rgb(205, 250, 220)
        isAntiAlias = true
    }

    private val listPaint = Paint().apply {
        color = Color.rgb(179, 188, 204)
        textSize = 24f
        isAntiAlias = true
    }

    private val errorPaint = Paint().apply {
        color = Color.rgb(255, 112, 112)
        textSize = 30f
        isAntiAlias = true
    }

    override fun onEnter() {
        loadChart()
        try {
            audioClock.setVolume(audioSettingsRepository.load().musicVolume)
            audioClock.load(AudioSource.AssetSource(chart.song.audioPath))
            audioClock.play()
        } catch (e: AudioClockException) {
            errorMessage = e.message
        }
        super.onEnter()
    }

    override fun onPause() {
        resumesAfterPause = audioClock.isPlaying
        audioClock.pause()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        if (resumesAfterPause && audioClock.isPrepared) {
            audioClock.play()
        }
    }

    override fun onExit() {
        saveChart()
        audioClock.release()
        controlsBinding = null
        super.onExit()
    }

    override fun createOverlay(): View {
        return OverlayEditorControlsBinding.inflate(LayoutInflater.from(gctx.view.context)).apply {
            controlsBinding = this
            playPauseButton.setOnClickListener {
                togglePlayback()
                updateBindingControls()
            }
            testButton.setOnClickListener { openTestPlay() }
            bpmButton.setOnClickListener { showBpmDialog() }
            snapButton.setOnClickListener { showSnapDialog() }
            upButton.setOnClickListener { moveSelectedNote(snapper.beatStep) }
            downButton.setOnClickListener { moveSelectedNote(-snapper.beatStep) }
            playbackSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (!fromUser || !audioClock.isPrepared) return
                    audioClock.seekTo((audioClock.durationMs * progress / BINDING_SEEK_MAX).toLong())
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                    isSeekingWithBinding = true
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    isSeekingWithBinding = false
                }
            })
            updateBindingControls()
        }.root.apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM,
            )
        }
    }

    override fun draw(canvas: Canvas) {
        val width = gctx.metrics.width
        val height = gctx.metrics.height
        val judgmentY = height * JUDGMENT_LINE_RATIO
        val audioTimeMs = audioClock.positionMs
        val visibleNoteCount = visibleNoteCount(audioTimeMs, width, height, judgmentY)

        canvas.drawRect(0f, 0f, width, height, backgroundPaint)
        drawBeatGrid(canvas, audioTimeMs, width, height, judgmentY)
        drawLaneGuides(canvas, width, height)
        canvas.drawLine(0f, judgmentY, width, judgmentY, judgmentLinePaint)
        canvas.drawText(text(R.string.title_chart_editor), 36f, 70f, textPaint)
        canvas.drawText(text(R.string.editor_time_format, audioTimeMs, audioClock.durationMs), 36f, 116f, textPaint)
        canvas.drawText(
            text(
                R.string.editor_info_format,
                formatBpm(chart.timing.bpm),
                chart.timing.snapDivisionsPerBeat,
                laneMapping.laneCount,
                chart.notes.size,
                visibleNoteCount,
            ),
            36f,
            162f,
            textPaint,
        )
        canvas.drawText(chartStatus, 36f, 208f, textPaint)
        canvas.drawText(text(R.string.editor_help), 36f, 254f, textPaint)

        drawNotes(canvas, audioTimeMs, width, height, judgmentY)
        drawRecordingPreview(canvas, audioTimeMs, width, height, judgmentY)
        drawRecentNotes(canvas, width)
        updateBindingControls()

        errorMessage?.let {
            canvas.drawText(text(R.string.error_format, it), 36f, 310f, errorPaint)
        }
    }

    private fun loadChart() {
        try {
            val loadedChart = chartRepository.load(chartFileName)
            chart = loadedChart ?: createDefaultChart(audioPath)
            laneMapping = EqualDivisionLaneMapping(chart.metadata.laneCount)
            rebuildTimingTools()
            chartStatus = if (loadedChart == null) {
                text(R.string.status_chart_new_file, chartFileName)
            } else {
                text(R.string.status_chart_loaded, loadedChart.notes.size)
            }
            errorMessage = null
        } catch (e: ChartStorageException) {
            chart = createDefaultChart(audioPath)
            laneMapping = EqualDivisionLaneMapping(chart.metadata.laneCount)
            rebuildTimingTools()
            chartStatus = text(R.string.status_chart_new_after_load_failure)
            errorMessage = e.message
        }
    }

    private fun saveChart() {
        try {
            val file = chartRepository.save(chartFileName, chart)
            chartStatus = text(R.string.status_chart_saved, chart.notes.size)
            errorMessage = null
            android.util.Log.i("EditorScene", "Chart saved: ${file.absolutePath}")
        } catch (e: ChartStorageException) {
            chartStatus = text(R.string.status_chart_save_failed)
            errorMessage = e.message
        }
    }

    private fun drawLaneGuides(canvas: Canvas, width: Float, height: Float) {
        for (i in 1 until laneMapping.laneCount) {
            val x = width * i / laneMapping.laneCount
            canvas.drawLine(x, 0f, x, height, lanePaint)
        }
    }

    private fun drawBeatGrid(
        canvas: Canvas,
        audioTimeMs: Long,
        width: Float,
        height: Float,
        judgmentY: Float,
    ) {
        val currentBeat = chart.timing.beatAt(audioTimeMs)
        val divisions = chart.timing.snapDivisionsPerBeat
        val firstGridIndex = kotlin.math.floor((currentBeat - BEATS_BEHIND) * divisions).toInt()
        val lastGridIndex = kotlin.math.ceil((currentBeat + BEATS_AHEAD) * divisions).toInt()

        for (gridIndex in firstGridIndex..lastGridIndex) {
            val beat = gridIndex.toDouble() / divisions
            val beatTimeMs = chart.timing.timeAtBeat(beat)
            val y = yForTime(beatTimeMs, audioTimeMs, judgmentY)
            if (y < 0f || y > height) continue

            val isWholeBeat = gridIndex % divisions == 0
            val wholeBeat = gridIndex / divisions
            val paint = when {
                isWholeBeat && wholeBeat % 4 == 0 -> downBeatPaint
                isWholeBeat -> beatPaint
                else -> snapGridPaint
            }
            canvas.drawLine(0f, y, width, y, paint)
        }
    }

    private fun drawNotes(
        canvas: Canvas,
        audioTimeMs: Long,
        width: Float,
        height: Float,
        judgmentY: Float,
    ) {
        for (note in chart.notes) {
            val bounds = boundsForNote(note, audioTimeMs, width, judgmentY)
            if (bounds.bottom < 0f || bounds.top > height) continue

            if (note is HoldNote) {
                drawHoldNote(canvas, bounds)
            } else {
                drawTapNote(canvas, bounds)
            }
            if (editorState.selectedNoteId == note.id) {
                drawSelectedNote(canvas, bounds)
            }

            val rawY = yForTime(note.rawTimeMs, audioTimeMs, judgmentY)
            drawRawNoteMarker(canvas, note.normalizedX * width, rawY)
        }
    }

    private fun visibleNoteCount(
        audioTimeMs: Long,
        width: Float,
        height: Float,
        judgmentY: Float,
    ): Int {
        return chart.notes.count { note ->
            val bounds = boundsForNote(note, audioTimeMs, width, judgmentY)
            bounds.bottom >= 0f && bounds.top <= height
        }
    }

    private fun boundsForNote(
        note: ChartNote,
        audioTimeMs: Long,
        width: Float,
        judgmentY: Float,
    ): NoteBounds {
        val laneWidth = width / laneMapping.laneCount
        val noteWidth = laneWidth * NOTE_WIDTH_RATIO
        val centerX = note.normalizedX * width
        val startY = yForTime(snappedTimeMsFor(note), audioTimeMs, judgmentY)
        val endY = yForTime(snappedEndTimeMsFor(note), audioTimeMs, judgmentY)
        val top = minOf(startY, endY) - NOTE_HEIGHT / 2f
        val bottom = maxOf(startY, endY) + NOTE_HEIGHT / 2f
        return NoteBounds(
            note = note,
            left = centerX - noteWidth / 2f,
            top = top,
            right = centerX + noteWidth / 2f,
            bottom = bottom,
        )
    }

    private fun drawTapNote(canvas: Canvas, bounds: NoteBounds) {
        canvas.drawRect(bounds.left, bounds.top, bounds.right, bounds.bottom, notePaint)
        canvas.drawRect(bounds.left, bounds.top, bounds.right, bounds.bottom, snappedNotePaint)
    }

    private fun drawHoldNote(canvas: Canvas, bounds: NoteBounds) {
        canvas.drawRect(bounds.left, bounds.top, bounds.right, bounds.bottom, notePaint)
        canvas.drawRect(bounds.left, bounds.top, bounds.right, bounds.bottom, snappedNotePaint)
    }

    private fun drawRecordingPreview(
        canvas: Canvas,
        audioTimeMs: Long,
        width: Float,
        height: Float,
        judgmentY: Float,
    ) {
        val preview = recordingPreview ?: return
        val endTimeMs = latencyConfig.recordingTimeFor(audioClock.positionMs).coerceAtLeast(preview.startTimeMs)
        val bounds = boundsForPreview(preview, endTimeMs, audioTimeMs, width, judgmentY)
        if (bounds.bottom < 0f || bounds.top > height) return

        canvas.drawRect(bounds, previewNotePaint)
        canvas.drawRect(bounds, previewBorderPaint)
    }

    private fun boundsForPreview(
        preview: RecordingNotePreview,
        endTimeMs: Long,
        audioTimeMs: Long,
        width: Float,
        judgmentY: Float,
    ): RectF {
        val laneWidth = width / laneMapping.laneCount
        val noteWidth = laneWidth * NOTE_WIDTH_RATIO
        val centerX = preview.normalizedX * width
        val startTimeMs = preview.snappedStartBeat?.let { chart.timing.timeAtBeat(it) } ?: preview.startTimeMs
        val startY = yForTime(startTimeMs, audioTimeMs, judgmentY)
        val endY = if (endTimeMs - preview.startTimeMs >= HOLD_PREVIEW_THRESHOLD_MS) {
            val snappedEndBeat = snapper.snapBeatFor(endTimeMs)
            yForTime(chart.timing.timeAtBeat(snappedEndBeat), audioTimeMs, judgmentY)
        } else {
            startY
        }
        return RectF(
            centerX - noteWidth / 2f,
            minOf(startY, endY) - NOTE_HEIGHT / 2f,
            centerX + noteWidth / 2f,
            maxOf(startY, endY) + NOTE_HEIGHT / 2f,
        )
    }

    private fun drawSelectedNote(canvas: Canvas, bounds: NoteBounds) {
        canvas.drawRect(bounds.left, bounds.top, bounds.right, bounds.bottom, selectedNotePaint)
    }

    private fun drawRawNoteMarker(canvas: Canvas, centerX: Float, centerY: Float) {
        canvas.drawCircle(centerX, centerY, RAW_NOTE_RADIUS, rawNotePaint)
    }

    private fun findTouchedNote(
        touchX: Float,
        touchY: Float,
        audioTimeMs: Long,
        width: Float,
        height: Float,
        judgmentY: Float,
    ): ChartNote? {
        return chart.notes
            .asSequence()
            .map { boundsForNote(it, audioTimeMs, width, judgmentY) }
            .filter { it.bottom >= 0f && it.top <= height }
            .filter { it.contains(touchX, touchY, NOTE_HIT_PADDING) }
            .minByOrNull { kotlin.math.abs(it.centerY - touchY) }
            ?.note
    }

    private fun drawRecentNotes(canvas: Canvas, width: Float) {
        val notes = chart.notes.takeLast(RECENT_NOTE_COUNT).asReversed()
        var y = RECENT_NOTE_LIST_TOP
        for (note in notes) {
            canvas.drawText(recentNoteText(note), width - RECENT_NOTE_LIST_WIDTH, y, listPaint)
            y += RECENT_NOTE_LINE_HEIGHT
        }
    }

    private fun recentNoteText(note: ChartNote): String {
        val lane = laneMapping.laneOf(note.normalizedX) + 1
        val beat = note.snappedBeat ?: chart.timing.beatAt(note.rawTimeMs)
        val beatText = String.format(Locale.US, "%.2f", beat)
        if (note is HoldNote) {
            val endBeat = note.snappedEndBeat ?: chart.timing.beatAt(note.rawEndTimeMs ?: note.rawTimeMs)
            val endBeatText = String.format(Locale.US, "%.2f", endBeat)
            return text(R.string.recent_hold_note, note.id, lane, beatText, endBeatText)
        }
        return text(R.string.recent_tap_note, note.id, lane, note.rawTimeMs, beatText)
    }

    private fun snappedTimeMsFor(note: ChartNote): Long {
        val beat = note.snappedBeat ?: return note.rawTimeMs
        return chart.timing.timeAtBeat(beat)
    }

    private fun snappedEndTimeMsFor(note: ChartNote): Long {
        val endBeat = note.snappedEndBeat
        if (endBeat != null) return chart.timing.timeAtBeat(endBeat)
        return note.rawEndTimeMs ?: snappedTimeMsFor(note)
    }

    private fun yForTime(timeMs: Long, audioTimeMs: Long, judgmentY: Float): Float {
        val timeDiffMs = timeMs - audioTimeMs
        return judgmentY - timeDiffMs * pixelsPerMs()
    }

    private fun pixelsPerMs(): Float {
        return BASE_PIXELS_PER_MS * noteSpeed
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val action = event.toTouchAction() ?: return true
        val point = gctx.metrics.fromScreen(event.x, event.y)
        if (touchRecorder.hasActiveSession && action != TouchAction.Down) {
            recordNoteAt(point.x, point.y, action)
            return true
        }
        if (handleTimelineDrag(action, point.y)) return true

        if (action != TouchAction.Down) return true

        val judgmentY = gctx.metrics.height * JUDGMENT_LINE_RATIO
        if (point.y < judgmentY) {
            selectNoteAt(point.x, point.y, judgmentY)
        } else {
            recordNoteAt(point.x, point.y, action)
        }
        return true
    }

    private fun handleTimelineDrag(action: TouchAction, y: Float): Boolean {
        if (!editorState.isDraggingTimeline) return false

        when (action) {
            TouchAction.Move -> seekTimelineByDrag(y)
            TouchAction.Up, TouchAction.Cancel -> editorState.isDraggingTimeline = false
            TouchAction.Down -> return false
        }
        return true
    }

    private fun showBpmDialog() {
        val context = gctx.view.context
        val showDialog = {
            val input = EditText(context).apply {
                setText(formatBpm(chart.timing.bpm))
                selectAll()
                inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
                setSingleLine(true)
            }
            AlertDialog.Builder(context)
                .setTitle(text(R.string.dialog_bpm_title))
                .setView(input)
                .setPositiveButton(text(R.string.action_apply)) { _, _ ->
                    val bpm = input.text.toString().toDoubleOrNull()
                    if (bpm == null || bpm !in MIN_BPM..MAX_BPM) {
                        chartStatus = text(R.string.status_bpm_range, formatBpm(MIN_BPM), formatBpm(MAX_BPM))
                    } else {
                        updateBpm(bpm)
                    }
                }
                .setNegativeButton(text(R.string.action_cancel), null)
                .show()
            Unit
        }
        runOnActivityThread(showDialog)
    }

    private fun showSnapDialog() {
        val context = gctx.view.context
        val labels = SNAP_DIVISION_OPTIONS.map { text(R.string.snap_option_format, it) }.toTypedArray()
        val selectedIndex = SNAP_DIVISION_OPTIONS.indexOf(chart.timing.snapDivisionsPerBeat)
            .coerceAtLeast(0)
        val showDialog = {
            AlertDialog.Builder(context)
                .setTitle(text(R.string.dialog_beat_snap_title))
                .setSingleChoiceItems(labels, selectedIndex) { dialog, index ->
                    updateSnapDivisions(SNAP_DIVISION_OPTIONS[index])
                    dialog.dismiss()
                }
                .setNegativeButton(text(R.string.action_cancel), null)
                .show()
            Unit
        }
        runOnActivityThread(showDialog)
    }

    private fun runOnActivityThread(action: () -> Unit) {
        val activity = gctx.view.context as? Activity
        if (activity == null) {
            action()
        } else {
            activity.runOnUiThread(action)
        }
    }

    private fun updateBpm(bpm: Double) {
        chart = chart.copy(timing = chart.timing.copy(bpm = bpm))
        rebuildTimingTools()
        chartStatus = text(R.string.status_bpm_changed, formatBpm(bpm))
    }

    private fun updateSnapDivisions(divisionsPerBeat: Int) {
        chart = chart.copy(
            timing = chart.timing.copy(snapDivisionsPerBeat = divisionsPerBeat)
        )
        rebuildTimingTools()
        chartStatus = text(R.string.status_snap_changed, divisionsPerBeat)
    }

    private fun rebuildTimingTools() {
        snapper = Snapper(chart.timing)
        touchRecorder = createTouchRecorder()
        recordingPreview = null
    }

    private fun moveSelectedNote(beatDelta: Double) {
        val selectedNoteId = editorState.selectedNoteId ?: return
        val note = chart.notes.firstOrNull { it.id == selectedNoteId } ?: return
        val currentBeat = note.snappedStartBeat ?: chart.timing.beatAt(note.rawStartTimeMs)
        val targetBeat = (currentBeat + beatDelta).coerceAtLeast(0.0)
        val hasDuplicate = chart.notes.any {
            it.id != note.id &&
                    laneMapping.laneOf(it.normalizedX) == laneMapping.laneOf(note.normalizedX) &&
                    kotlin.math.abs((it.snappedStartBeat ?: chart.timing.beatAt(it.rawStartTimeMs)) - targetBeat) < BEAT_EPSILON
        }
        if (hasDuplicate) {
            chartStatus = text(R.string.status_move_duplicate)
            return
        }

        val moved = NoteBeatMover(chart.timing).move(note, beatDelta)
        chartStatus = if (moved) {
            text(R.string.status_note_moved, note.id, formatBeat(note.snappedStartBeat ?: targetBeat))
        } else {
            text(R.string.status_note_first_beat, note.id)
        }
    }

    private fun formatBpm(bpm: Double): String {
        return String.format(Locale.US, "%.2f", bpm).trimEnd('0').trimEnd('.')
    }

    private fun formatBeat(beat: Double): String {
        return String.format(Locale.US, "%.3f", beat).trimEnd('0').trimEnd('.')
    }

    private fun togglePlayback() {
        if (!audioClock.isPrepared) return
        if (audioClock.isPlaying) {
            audioClock.pause()
            chartStatus = text(R.string.status_playback_paused)
        } else {
            audioClock.play()
            chartStatus = text(R.string.status_playback_playing)
        }
    }

    private fun updateBindingControls() {
        controlsBinding?.apply {
            playPauseButton.setText(if (audioClock.isPlaying) R.string.action_pause else R.string.action_play)
            val hasSelection = editorState.selectedNoteId != null
            upButton.isEnabled = hasSelection
            downButton.isEnabled = hasSelection
            if (!isSeekingWithBinding && audioClock.durationMs > 0L) {
                playbackSeekBar.progress =
                    (audioClock.positionMs * BINDING_SEEK_MAX / audioClock.durationMs).toInt()
            }
        }
    }

    private fun openTestPlay() {
        saveChart()
        PlayScene(gctx, chartFileName).push()
    }

    private fun selectNoteAt(x: Float, y: Float, judgmentY: Float) {
        val note = findTouchedNote(
            touchX = x,
            touchY = y,
            audioTimeMs = audioClock.positionMs,
            width = gctx.metrics.width,
            height = gctx.metrics.height,
            judgmentY = judgmentY,
        )
        editorState.selectedNoteId = note?.id
        chartStatus = if (note == null) {
            beginTimelineDrag(y)
            text(R.string.status_timeline_drag)
        } else {
            text(R.string.status_note_selected, note.id)
        }
    }

    private fun beginTimelineDrag(y: Float) {
        editorState.selectedNoteId = null
        editorState.isDraggingTimeline = true
        editorState.dragStartY = y
        editorState.dragStartAudioTimeMs = audioClock.positionMs
    }

    private fun seekTimelineByDrag(currentY: Float) {
        if (!audioClock.isPrepared) return
        val deltaY = currentY - editorState.dragStartY
        val deltaMs = (deltaY / SEEK_PIXELS_PER_MS).toLong()
        val targetMs = (editorState.dragStartAudioTimeMs + deltaMs)
            .coerceIn(0L, audioClock.durationMs.coerceAtLeast(0L))
        audioClock.seekTo(targetMs)
        chartStatus = text(R.string.status_timeline_seek, targetMs)
    }

    private fun recordNoteAt(x: Float, y: Float, action: TouchAction) {
        val rawNormalizedX = (x / gctx.metrics.width).coerceIn(0f, 1f)
        val laneNormalizedX = laneMapping.snapX(rawNormalizedX)
        val normalizedY = (y / gctx.metrics.height).coerceIn(0f, 1f)
        val recordTimeMs = latencyConfig.recordingTimeFor(audioClock.positionMs)
        updateRecordingPreview(action, recordTimeMs, laneNormalizedX)

        val note = touchRecorder.record(
            RecordedTouchEvent(
                pointerId = 0,
                action = action,
                audioTimeMs = recordTimeMs,
                normalizedX = laneNormalizedX,
                normalizedY = normalizedY,
            )
        )
        editorState.selectedNoteId = note?.id
        chartStatus = if (note == null) {
            when (action) {
                TouchAction.Down -> text(R.string.status_note_recording)
                TouchAction.Up -> text(R.string.status_note_duplicate)
                TouchAction.Cancel -> text(R.string.status_note_recording_canceled)
                TouchAction.Move -> chartStatus
            }
        } else {
            recordingPreview = null
            text(R.string.status_note_recorded, note.type.name, note.id)
        }
    }

    private fun updateRecordingPreview(action: TouchAction, recordTimeMs: Long, normalizedX: Float) {
        when (action) {
            TouchAction.Down -> {
                recordingPreview = RecordingNotePreview(
                    startTimeMs = recordTimeMs,
                    snappedStartBeat = snapper.snapBeatFor(recordTimeMs),
                    normalizedX = normalizedX,
                )
            }

            TouchAction.Up, TouchAction.Cancel -> {
                recordingPreview = null
            }

            TouchAction.Move -> Unit
        }
    }

    private fun createTouchRecorder(): TouchRecorder {
        return TouchRecorder(
            chart = chart,
            snapper = snapper,
            duplicatePolicy = LaneBeatDuplicatePolicy(laneMapping),
        )
    }

    private fun createDefaultChart(audioPath: String = TEST_AUDIO_ASSET): Chart {
        return Chart(
            song = SongMetadata(
                title = audioPath.substringAfterLast('/').substringBeforeLast('.').ifBlank { "Test Song" },
                audioPath = audioPath,
            ),
            timing = TimingMap(bpm = TEST_BPM),
        )
    }

    private fun MotionEvent.toTouchAction(): TouchAction? {
        return when (actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> TouchAction.Down
            MotionEvent.ACTION_MOVE -> TouchAction.Move
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> TouchAction.Up
            MotionEvent.ACTION_CANCEL -> TouchAction.Cancel
            else -> null
        }
    }

    private fun text(resId: Int, vararg args: Any): String {
        return gctx.view.context.getString(resId, *args)
    }

    companion object {
        private const val CHART_FILE_NAME = "test_song.chart.json"
        private const val TEST_AUDIO_ASSET = "mp3/s001.mp3"
        private const val TEST_BPM = 111.0
        private const val JUDGMENT_LINE_RATIO = 0.78f
        private const val BINDING_SEEK_MAX = 1000L
        private const val BASE_PIXELS_PER_MS = 0.32f
        private const val SEEK_PIXELS_PER_MS = 0.32f
        private const val DEFAULT_NOTE_SPEED = 1.0f
        private const val BEATS_BEHIND = 2.0
        private const val BEATS_AHEAD = 8.0
        private const val NOTE_WIDTH_RATIO = 0.82f
        private const val NOTE_HEIGHT = 38f
        private const val NOTE_HIT_PADDING = 18f
        private const val RAW_NOTE_RADIUS = 6f
        private const val HOLD_PREVIEW_THRESHOLD_MS = 180L
        private const val RECENT_NOTE_COUNT = 6
        private const val RECENT_NOTE_LIST_TOP = 430f
        private const val RECENT_NOTE_LIST_WIDTH = 430f
        private const val RECENT_NOTE_LINE_HEIGHT = 30f
        private const val MIN_BPM = 20.0
        private const val MAX_BPM = 400.0
        private const val BEAT_EPSILON = 0.000001
        private val SNAP_DIVISION_OPTIONS = listOf(1, 2, 3, 4, 6, 8, 12, 16)
    }
}
