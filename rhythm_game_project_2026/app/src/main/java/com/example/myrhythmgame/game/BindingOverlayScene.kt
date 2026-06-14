package com.example.myrhythmgame.game

import android.view.View
import com.example.myrhythmgame.app.UiOverlayHost
import kr.ac.tukorea.ge.spgp2026.a2dg.scene.Scene
import kr.ac.tukorea.ge.spgp2026.a2dg.view.GameContext

abstract class BindingOverlayScene(gctx: GameContext) : Scene(gctx) {
    private val overlayHost: UiOverlayHost
        get() = gctx.view.context as UiOverlayHost

    protected abstract fun createOverlay(): View

    override fun onEnter() {
        showOverlay()
    }

    override fun onPause() {
        overlayHost.clearOverlay()
    }

    override fun onResume() {
        showOverlay()
    }

    override fun onExit() {
        overlayHost.clearOverlay()
    }

    protected fun refreshOverlay() {
        showOverlay()
    }

    private fun showOverlay() {
        overlayHost.showOverlay(createOverlay())
    }
}
