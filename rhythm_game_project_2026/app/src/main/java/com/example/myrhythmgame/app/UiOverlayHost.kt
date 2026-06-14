package com.example.myrhythmgame.app

import android.view.View

interface UiOverlayHost {
    fun showOverlay(view: View)
    fun clearOverlay()
}
