package com.example.myrhythmgame.game.menu

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import com.example.myrhythmgame.databinding.SceneHomeBinding
import com.example.myrhythmgame.game.BindingOverlayScene
import kr.ac.tukorea.ge.spgp2026.a2dg.view.GameContext

class HomeScene(gctx: GameContext) : BindingOverlayScene(gctx) {
    private val backgroundPaint = Paint().apply { color = Color.BLACK }

    override fun createOverlay(): View {
        return SceneHomeBinding.inflate(LayoutInflater.from(gctx.view.context)).apply {
            playButton.setOnClickListener { PlayMenuScene(gctx).push() }
            editButton.setOnClickListener { MenuScene(gctx).push() }
            settingsButton.setOnClickListener { SettingsScene(gctx).push() }
        }.root
    }

    override fun draw(canvas: Canvas) {
        canvas.drawRect(0f, 0f, gctx.metrics.width, gctx.metrics.height, backgroundPaint)
    }
}
