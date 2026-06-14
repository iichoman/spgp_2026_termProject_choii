package com.example.myrhythmgame.game.menu

import android.app.Activity
import android.app.AlertDialog
import android.text.InputType
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.MotionEvent
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import com.example.myrhythmgame.R
import com.example.myrhythmgame.databinding.SceneChartMenuBinding
import com.example.myrhythmgame.game.BindingOverlayScene
import com.example.myrhythmgame.game.editor.EditorScene
import com.example.myrhythmgame.rhythm.chart.Chart
import com.example.myrhythmgame.rhythm.chart.ChartMetadata
import com.example.myrhythmgame.rhythm.chart.SongMetadata
import com.example.myrhythmgame.rhythm.lane.LaneMode
import com.example.myrhythmgame.rhythm.storage.ChartFileInfo
import com.example.myrhythmgame.rhythm.storage.ChartRepository
import com.example.myrhythmgame.rhythm.storage.ChartStorageException
import com.example.myrhythmgame.rhythm.timing.TimingMap
import kr.ac.tukorea.ge.spgp2026.a2dg.view.GameContext

class MenuScene(gctx: GameContext) : BindingOverlayScene(gctx) {
    private val chartRepository = ChartRepository(gctx.view.context)

    private var songs = emptyList<String>()
    private var selectedSong: String? = null
    private var chartsForSelectedSong = emptyList<ChartFileInfo>()
    private var selectedChartFileName: String? = null
    private var statusText = text(R.string.message_select_song)
    private var songListScrollY = 0
    private var chartListScrollY = 0

    private val backgroundPaint = Paint().apply {
        style = Paint.Style.FILL
        color = Color.BLACK
    }

    override fun onEnter() {
        reloadSongs()
        reloadCharts()
        super.onEnter()
    }

    override fun onResume() {
        reloadCharts()
        super.onResume()
    }

    override fun createOverlay(): View {
        return SceneChartMenuBinding.inflate(LayoutInflater.from(gctx.view.context)).apply {
            titleText.setText(R.string.title_edit_menu)
            editActions.visibility = View.VISIBLE
            statusText.text = this@MenuScene.statusText

            songs.forEach { song ->
                songList.addView(createMenuRow(gctx.view.context, song.substringAfterLast('/'), song == selectedSong) {
                    rememberScrollPositions(this)
                    selectedSong = song
                    selectedChartFileName = null
                    reloadCharts()
                    this@MenuScene.statusText = text(R.string.status_song_selected, song)
                    refreshOverlay()
                })
            }
            chartsForSelectedSong.forEach { chart ->
                chartList.addView(createMenuRow(
                    gctx.view.context,
                    chartText(chart),
                    chart.fileName == selectedChartFileName,
                ) {
                    rememberScrollPositions(this)
                    selectedChartFileName = chart.fileName
                    this@MenuScene.statusText = text(R.string.status_chart_selected, chart.metadata.displayName)
                    refreshOverlay()
                })
            }

            newMapButton.isEnabled = selectedSong != null
            openButton.isEnabled = selectedChartFileName != null
            renameButton.isEnabled = selectedChartFileName != null
            deleteButton.isEnabled = selectedChartFileName != null
            newMapButton.setOnClickListener { createNewMap() }
            openButton.setOnClickListener { openSelectedMap() }
            renameButton.setOnClickListener { renameSelectedMap() }
            deleteButton.setOnClickListener {
                rememberScrollPositions(this)
                deleteSelectedMap()
                refreshOverlay()
            }
            restoreScrollPositions(this)
        }.root
    }

    private fun rememberScrollPositions(binding: SceneChartMenuBinding) {
        songListScrollY = binding.songScrollView.scrollY
        chartListScrollY = binding.chartScrollView.scrollY
    }

    private fun restoreScrollPositions(binding: SceneChartMenuBinding) {
        binding.songScrollView.post { binding.songScrollView.scrollTo(0, songListScrollY) }
        binding.chartScrollView.post { binding.chartScrollView.scrollTo(0, chartListScrollY) }
    }

    override fun draw(canvas: Canvas) {
        canvas.drawRect(0f, 0f, gctx.metrics.width, gctx.metrics.height, backgroundPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return true
    }

    private fun createNewMap() {
        val song = selectedSong ?: return
        val defaultName = text(R.string.default_new_chart, chartsForSelectedSong.size + 1)
        showNewMapDialog(
            initialName = defaultName,
        ) { displayName, laneMode ->
            createNewMap(song, displayName.ifBlank { defaultName }, laneMode)
        }
    }

    private fun createNewMap(song: String, displayName: String, laneMode: LaneMode) {
        val chartIndex = chartsForSelectedSong.size + 1
        val chartId = "new_chart_$chartIndex"
        val fileName = chartRepository.createUniqueFileName(song, displayName)
        val chart = Chart(
            song = SongMetadata(
                title = song.substringAfterLast('/').substringBeforeLast('.'),
                audioPath = song,
            ),
            metadata = ChartMetadata(
                id = chartId,
                displayName = displayName,
                laneCount = laneMode.laneCount,
            ),
            timing = TimingMap(bpm = DEFAULT_BPM),
        )

        try {
            chartRepository.save(fileName, chart)
            selectedChartFileName = fileName
            reloadCharts()
            statusText = text(R.string.status_created, displayName, laneModeText(laneMode))
            EditorScene(gctx, song, fileName).push()
        } catch (e: ChartStorageException) {
            statusText = text(R.string.status_create_failed, e.message.orEmpty())
        }
    }

    private fun openSelectedMap() {
        val song = selectedSong ?: return
        val fileName = selectedChartFileName ?: return
        EditorScene(gctx, song, fileName).push()
    }

    private fun deleteSelectedMap() {
        val fileName = selectedChartFileName ?: return
        try {
            if (chartRepository.delete(fileName)) {
                selectedChartFileName = null
                reloadCharts()
                statusText = text(R.string.status_deleted, fileName)
            } else {
                statusText = text(R.string.status_delete_failed, fileName)
            }
        } catch (e: ChartStorageException) {
            statusText = text(R.string.status_delete_failed, e.message.orEmpty())
        }
    }

    private fun renameSelectedMap() {
        val fileName = selectedChartFileName ?: return
        val currentChart = chartsForSelectedSong.firstOrNull { it.fileName == fileName } ?: return
        showMapNameDialog(
            title = text(R.string.dialog_rename_map),
            initialName = currentChart.metadata.displayName,
        ) { displayName ->
            if (displayName.isBlank()) {
                statusText = text(R.string.status_rename_empty)
                return@showMapNameDialog
            }
            try {
                chartRepository.rename(fileName, displayName)
                reloadCharts()
                selectedChartFileName = fileName
                statusText = text(R.string.status_renamed, displayName)
                refreshOverlay()
            } catch (e: ChartStorageException) {
                statusText = text(R.string.status_rename_failed, e.message.orEmpty())
                refreshOverlay()
            }
        }
    }

    private fun showMapNameDialog(
        title: String,
        initialName: String,
        onConfirm: (String) -> Unit,
    ) {
        val context = gctx.view.context
        val showDialog = {
            val input = EditText(context).apply {
                setText(initialName)
                selectAll()
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
                setSingleLine(true)
            }
            AlertDialog.Builder(context)
                .setTitle(title)
                .setView(input)
                .setPositiveButton(text(R.string.action_ok)) { _, _ -> onConfirm(input.text.toString().trim()) }
                .setNegativeButton(text(R.string.action_cancel), null)
                .show()
            Unit
        }

        val activity = context as? Activity
        if (activity == null) {
            showDialog()
        } else {
            activity.runOnUiThread(showDialog)
        }
    }

    private fun showNewMapDialog(
        initialName: String,
        onConfirm: (String, LaneMode) -> Unit,
    ) {
        val context = gctx.view.context
        val showDialog = {
            val input = EditText(context).apply {
                setText(initialName)
                selectAll()
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
                setSingleLine(true)
            }
            val laneGroup = RadioGroup(context).apply {
                orientation = RadioGroup.HORIZONTAL
            }
            LaneMode.entries.forEach { mode ->
                laneGroup.addView(
                    RadioButton(context).apply {
                        id = mode.ordinal + LANE_RADIO_ID_BASE
                        text = laneModeText(mode)
                        isChecked = mode == LaneMode.Default
                    }
                )
            }
            val content = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                val padding = (NEW_MAP_DIALOG_PADDING_DP * resources.displayMetrics.density).toInt()
                setPadding(padding, padding / 2, padding, 0)
                addView(input)
                addView(laneGroup)
            }

            AlertDialog.Builder(context)
                .setTitle(text(R.string.dialog_new_map))
                .setView(content)
                .setPositiveButton(text(R.string.action_create)) { _, _ ->
                    val selectedMode = LaneMode.entries.firstOrNull {
                        laneGroup.checkedRadioButtonId == it.ordinal + LANE_RADIO_ID_BASE
                    } ?: LaneMode.Default
                    onConfirm(input.text.toString().trim(), selectedMode)
                }
                .setNegativeButton(text(R.string.action_cancel), null)
                .show()
            Unit
        }

        val activity = context as? Activity
        if (activity == null) {
            showDialog()
        } else {
            activity.runOnUiThread(showDialog)
        }
    }

    private fun reloadSongs() {
        songs = try {
            gctx.view.context.assets
                .list(ASSET_MP3_DIR)
                .orEmpty()
                .filter { it.endsWith(".mp3", ignoreCase = true) }
                .sorted()
                .map { "$ASSET_MP3_DIR/$it" }
        } catch (e: Exception) {
            statusText = text(R.string.status_read_songs_failed, e.message.orEmpty())
            emptyList()
        }

        if (selectedSong == null) {
            selectedSong = songs.firstOrNull()
        }
    }

    private fun reloadCharts() {
        val song = selectedSong
        chartsForSelectedSong = if (song == null) {
            emptyList()
        } else {
            try {
                chartRepository.listChartsForAudio(song)
            } catch (e: ChartStorageException) {
                statusText = text(R.string.status_read_charts_failed, e.message.orEmpty())
                emptyList()
            }
        }

        if (selectedChartFileName != null && chartsForSelectedSong.none { it.fileName == selectedChartFileName }) {
            selectedChartFileName = null
        }
    }

    private fun chartText(chart: ChartFileInfo): String {
        return text(
            R.string.chart_summary_format,
            chart.metadata.displayName,
            chart.metadata.laneCount,
            chart.noteCount,
            chart.metadata.difficulty,
        )
    }

    private fun text(resId: Int, vararg args: Any): String {
        return gctx.view.context.getString(resId, *args)
    }

    private fun laneModeText(mode: LaneMode): String {
        return text(
            when (mode) {
                LaneMode.FourKey -> R.string.lane_four_key
                LaneMode.SixKey -> R.string.lane_six_key
            }
        )
    }

    companion object {
        private const val ASSET_MP3_DIR = "mp3"
        private const val DEFAULT_BPM = 118.0
        private const val LANE_RADIO_ID_BASE = 1000
        private const val NEW_MAP_DIALOG_PADDING_DP = 20
    }
}
