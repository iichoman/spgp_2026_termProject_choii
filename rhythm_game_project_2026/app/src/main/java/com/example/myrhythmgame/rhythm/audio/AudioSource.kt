package com.example.myrhythmgame.rhythm.audio

import android.net.Uri
import androidx.annotation.RawRes

sealed interface AudioSource {
    data class UriSource(val uri: Uri) : AudioSource
    data class AssetSource(val path: String) : AudioSource
    data class RawResourceSource(@param:RawRes val resId: Int) : AudioSource
}
