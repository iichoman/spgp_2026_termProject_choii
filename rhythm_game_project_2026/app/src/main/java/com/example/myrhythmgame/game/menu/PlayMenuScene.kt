package com.example.myrhythmgame.game.menu

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.MotionEvent
import android.view.LayoutInflater
import android.view.View
import com.example.myrhythmgame.R
import com.example.myrhythmgame.databinding.SceneChartMenuBinding
import com.example.myrhythmgame.game.BindingOverlayScene
import com.example.myrhythmgame.game.play.PlayScene
import com.example.myrhythmgame.rhythm.storage.ChartFileInfo
import com.example.myrhythmgame.rhythm.storage.ChartRepository
import com.example.myrhythmgame.rhythm.storage.ChartStorageException
import kr.ac.tukorea.ge.spgp2026.a2dg.view.GameContext

class PlayMenuScene(gctx: GameContext) : BindingOverlayScene(gctx) {
    private val chartRepository = ChartRepository(gctx.view.context)
    private var songs = emptyList<String>()
    private var selectedSong: String? = null
    private var charts = emptyList<ChartFileInfo>()
    private var selectedChartFileName: String? = null
    private var songListScrollY = 0
    private var chartListScrollY = 0

    private val backgroundPaint = Paint().apply { color = Color.BLACK }

    override fun onEnter() {
        loadSongs()
        loadCharts()
        super.onEnter()
    }

    override fun onResume() {
        loadCharts()
        super.onResume()
    }

    override fun createOverlay(): View {
        return SceneChartMenuBinding.inflate(LayoutInflater.from(gctx.view.context)).apply {
            titleText.setText(R.string.title_play_menu)
            playButton.visibility = View.VISIBLE
            statusText.visibility = View.GONE

            songs.forEach { song ->
                songList.addView(createMenuRow(gctx.view.context, song.substringAfterLast('/'), song == selectedSong) {
                    rememberScrollPositions(this)
                    selectedSong = song
                    selectedChartFileName = null
                    loadCharts()
                    refreshOverlay()
                })
            }
            charts.forEach { chart ->
                chartList.addView(createMenuRow(
                    gctx.view.context,
                    text(R.string.chart_lane_format, chart.metadata.displayName, chart.metadata.laneCount),
                    chart.fileName == selectedChartFileName,
                ) {
                    rememberScrollPositions(this)
                    selectedChartFileName = chart.fileName
                    refreshOverlay()
                })
            }
            playButton.isEnabled = selectedChartFileName != null
            playButton.setOnClickListener {
                selectedChartFileName?.let { PlayScene(gctx, it).push() }
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

    private fun loadSongs() {
        songs = gctx.view.context.assets.list("mp3").orEmpty()
            .filter { it.endsWith(".mp3", ignoreCase = true) }
            .sorted()
            .map { "mp3/$it" }
        if (selectedSong == null) selectedSong = songs.firstOrNull()
    }

    private fun loadCharts() {
        charts = try {
            selectedSong?.let(chartRepository::listChartsForAudio).orEmpty()
        } catch (_: ChartStorageException) {
            emptyList()
        }
        if (selectedChartFileName !in charts.map { it.fileName }) {
            selectedChartFileName = charts.firstOrNull()?.fileName
        }
    }

    private fun text(resId: Int, vararg args: Any): String {
        return gctx.view.context.getString(resId, *args)
    }
}
