package com.example.myrhythmgame.rhythm.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool

class SoundPoolNoteSoundPlayer(
    context: Context,
    soundResources: Map<String, Int>,
    maxStreams: Int = DEFAULT_MAX_STREAMS,
) : NoteSoundPlayer {
    private val soundPool = SoundPool.Builder()
        .setMaxStreams(maxStreams)
        .setAudioAttributes(SOUND_ATTRIBUTES)
        .build()

    private val soundIdsByKey = mutableMapOf<String, Int>()
    private val loadedSoundIds = mutableSetOf<Int>()
    private var isReleased = false
    private var volume = DEFAULT_VOLUME

    init {
        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == LOAD_SUCCESS) {
                loadedSoundIds.add(sampleId)
            }
        }
        soundResources.forEach { (soundKey, resourceId) ->
            soundIdsByKey[soundKey] = soundPool.load(context, resourceId, LOAD_PRIORITY)
        }
    }

    override fun play(soundKey: String) {
        if (isReleased) return
        val soundId = soundIdsByKey[soundKey] ?: return
        if (soundId !in loadedSoundIds) return

        soundPool.play(
            soundId,
            volume,
            volume,
            PLAY_PRIORITY,
            NO_LOOP,
            DEFAULT_RATE,
        )
    }

    override fun setVolume(volume: Float) {
        this.volume = volume.coerceIn(MIN_VOLUME, MAX_VOLUME)
    }

    override fun release() {
        if (isReleased) return
        isReleased = true
        loadedSoundIds.clear()
        soundPool.release()
    }

    companion object {
        private const val DEFAULT_MAX_STREAMS = 8
        private const val LOAD_PRIORITY = 1
        private const val LOAD_SUCCESS = 0
        private const val PLAY_PRIORITY = 1
        private const val NO_LOOP = 0
        private const val DEFAULT_VOLUME = 1f
        private const val MIN_VOLUME = 0f
        private const val MAX_VOLUME = 1f
        private const val DEFAULT_RATE = 1f

        private val SOUND_ATTRIBUTES = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
    }
}
