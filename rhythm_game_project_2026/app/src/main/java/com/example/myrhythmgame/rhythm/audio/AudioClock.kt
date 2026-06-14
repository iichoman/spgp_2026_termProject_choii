package com.example.myrhythmgame.rhythm.audio

/*
* AudioClock:
* 음원 시계 - 음원과 에디터의 입력 기록, 노트 출력, 박자 스냅, 노트 판정에 사용
*
*
*/
interface AudioClock {
    val state: PlaybackState
    val positionMs: Long
    val durationMs: Long
    val isPlaying: Boolean
        get() = state == PlaybackState.Playing
    val isPrepared: Boolean
        get() = state.isPrepared

    fun load(source: AudioSource)
    fun play()
    fun pause()
    fun stop()
    fun seekTo(positionMs: Long)
    fun setVolume(volume: Float)
    fun release()
}
