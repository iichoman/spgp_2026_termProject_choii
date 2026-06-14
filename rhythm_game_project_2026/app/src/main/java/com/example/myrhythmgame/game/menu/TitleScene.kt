package com.example.myrhythmgame.game.menu

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.MotionEvent
import com.example.myrhythmgame.R
import kr.ac.tukorea.ge.spgp2026.a2dg.scene.Scene
import kr.ac.tukorea.ge.spgp2026.a2dg.view.GameContext

class TitleScene(gctx: GameContext) : Scene(gctx) {
    private val titleImage = gctx.res.getDrawable(R.mipmap.ic_launcher).apply {
        setBounds(250, 390, 650, 790)
    }

    private val backgroundPaint = Paint().apply {
        color = Color.BLACK
    }
    private val titlePaint = Paint().apply {
        color = Color.WHITE
        textSize = 72f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }
    private val promptPaint = Paint().apply {
        color = Color.rgb(180, 220, 255)
        textSize = 34f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }

    override fun draw(canvas: Canvas) {
        val width = gctx.metrics.width
        canvas.drawRect(0f, 0f, width, gctx.metrics.height, backgroundPaint)
        titleImage.draw(canvas)
        canvas.drawText(text(R.string.title_game), width / 2f, 900f, titlePaint)
        canvas.drawText(text(R.string.title_touch_any_screen), width / 2f, 1230f, promptPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_UP) {
            HomeScene(gctx).push()
        }
        return true
    }

    private fun text(resId: Int): String = gctx.view.context.getString(resId)
}
