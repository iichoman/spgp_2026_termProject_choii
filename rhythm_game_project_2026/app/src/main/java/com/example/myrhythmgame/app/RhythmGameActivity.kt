package com.example.myrhythmgame.app

import android.view.View
import android.view.ViewGroup
import com.example.myrhythmgame.databinding.ActivityMainBinding
import com.example.myrhythmgame.game.menu.TitleScene
import kr.ac.tukorea.ge.spgp2026.a2dg.activity.BaseGameActivity
import kr.ac.tukorea.ge.spgp2026.a2dg.scene.Scene
import kr.ac.tukorea.ge.spgp2026.a2dg.view.GameContext
import kr.ac.tukorea.ge.spgp2026.a2dg.view.GameView

class RhythmGameActivity : BaseGameActivity(), UiOverlayHost {
    private lateinit var binding: ActivityMainBinding

    override val drawsDebugGrid: Boolean = false
    override val drawsDebugInfo: Boolean = false
    override val drawsFpsGraph: Boolean = false

    override fun createContentView(gameView: GameView): View {
        binding = ActivityMainBinding.inflate(layoutInflater)
        binding.root.addView(
            gameView,
            0,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )
        return binding.root
    }

    override fun showOverlay(view: View) {
        binding.overlayContainer.removeAllViews()
        binding.overlayContainer.addView(view)
    }

    override fun clearOverlay() {
        binding.overlayContainer.removeAllViews()
    }

    override fun createRootScene(gctx: GameContext): Scene {
        gctx.metrics.setSize(900f, 1600f)
        return TitleScene(gctx)
    }
}
