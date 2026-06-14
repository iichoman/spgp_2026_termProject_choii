package com.example.myrhythmgame.rhythm.audio

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import java.io.IOException

class AndroidMediaAudioClock(
    context: Context,
) : AudioClock {
    private val appContext = context.applicationContext
    private var mediaPlayer: MediaPlayer? = null
    private var volume = DEFAULT_VOLUME

    override var state: PlaybackState = PlaybackState.Idle
        private set

    override val positionMs: Long
        get() = readPositionMs()

    override val durationMs: Long
        get() = readDurationMs()

    override fun load(source: AudioSource) {
        releasePlayer()
        state = PlaybackState.Loading

        try {
            mediaPlayer = createPreparedPlayer(source)
            state = PlaybackState.Ready
        } catch (e: IOException) {
            state = PlaybackState.Error
            throw AudioClockException("Failed to load audio source: $source", e)
        } catch (e: RuntimeException) {
            state = PlaybackState.Error
            throw AudioClockException("Failed to prepare audio source: $source", e)
        }
    }

    override fun play() {
        val player = requirePlayer()
        if (!state.isPrepared) {
            throw AudioClockException("Cannot play audio while state is $state.")
        }
        player.start()
        state = PlaybackState.Playing
    }

    override fun pause() {
        val player = mediaPlayer ?: return
        if (state != PlaybackState.Playing) return
        player.pause()
        state = PlaybackState.Paused
    }

    override fun stop() {
        val player = mediaPlayer ?: return
        if (!state.isPrepared) return
        player.pause()
        seekPlayerTo(player, 0L)
        state = PlaybackState.Stopped
    }

    override fun seekTo(positionMs: Long) {
        val player = requirePlayer()
        if (!state.isPrepared) {
            throw AudioClockException("Cannot seek audio while state is $state.")
        }
        seekPlayerTo(player, positionMs.coerceIn(0L, durationMs.coerceAtLeast(0L)))
    }

    override fun setVolume(volume: Float) {
        this.volume = volume.coerceIn(MIN_VOLUME, MAX_VOLUME)
        mediaPlayer?.setVolume(this.volume, this.volume)
    }

    override fun release() {
        releasePlayer()
        state = PlaybackState.Released
    }

    fun updateLoop(loopRegion: LoopRegion?) {
        if (loopRegion == null || !loopRegion.shouldWrap(positionMs)) return
        seekTo(loopRegion.startMs)
        if (state != PlaybackState.Playing) return
        requirePlayer().start()
    }

    private fun createPreparedPlayer(source: AudioSource): MediaPlayer {
        return MediaPlayer().apply {
            setAudioAttributes(musicAttributes)
            setVolume(volume, volume)
            setOnCompletionListener {
                state = PlaybackState.Completed
            }
            setOnErrorListener { _, _, _ ->
                state = PlaybackState.Error
                true
            }
            setDataSourceFrom(source)
            prepare()
        }
    }

    private fun MediaPlayer.setDataSourceFrom(source: AudioSource) {
        when (source) {
            is AudioSource.UriSource -> setDataSource(appContext, source.uri)
            is AudioSource.AssetSource -> {
                val descriptor = appContext.assets.openFd(source.path)
                setDataSource(descriptor)
            }
            is AudioSource.RawResourceSource -> {
                val descriptor = appContext.resources.openRawResourceFd(source.resId)
                    ?: throw AudioClockException("Raw resource ${source.resId} could not be opened.")
                setDataSource(descriptor)
            }
        }
    }

    private fun MediaPlayer.setDataSource(descriptor: AssetFileDescriptor) {
        descriptor.use {
            setDataSource(it.fileDescriptor, it.startOffset, it.length)
        }
    }

    private fun readPositionMs(): Long {
        val player = mediaPlayer ?: return 0L
        if (!state.isPrepared) return 0L
        return runCatching { player.currentPosition.toLong() }.getOrDefault(0L)
    }

    private fun readDurationMs(): Long {
        val player = mediaPlayer ?: return 0L
        if (!state.isPrepared) return 0L
        return runCatching { player.duration.toLong() }.getOrDefault(0L)
    }

    private fun requirePlayer(): MediaPlayer {
        return mediaPlayer ?: throw AudioClockException("Audio source is not loaded.")
    }

    private fun releasePlayer() {
        mediaPlayer?.release()
        mediaPlayer = null
    }

    private fun seekPlayerTo(player: MediaPlayer, positionMs: Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            player.seekTo(positionMs, MediaPlayer.SEEK_CLOSEST)
        } else {
            @Suppress("DEPRECATION")
            player.seekTo(positionMs.toInt())
        }
    }

    companion object {
        private const val MIN_VOLUME = 0f
        private const val MAX_VOLUME = 1f
        private const val DEFAULT_VOLUME = 1f
        private val musicAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
    }
}
