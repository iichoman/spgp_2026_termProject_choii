package com.example.myrhythmgame.rhythm.audio

interface NoteSoundPlayer {
    fun play(soundKey: String)
    fun setVolume(volume: Float)
    fun release()
}
