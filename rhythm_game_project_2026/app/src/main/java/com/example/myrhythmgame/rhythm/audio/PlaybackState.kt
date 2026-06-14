package com.example.myrhythmgame.rhythm.audio

enum class PlaybackState {
    Idle,
    Loading,
    Ready,
    Playing,
    Paused,
    Stopped,
    Completed,
    Released,
    Error;

    val isPrepared: Boolean
        get() = this == Ready ||
                this == Playing ||
                this == Paused ||
                this == Stopped ||
                this == Completed
}
