package com.example.myrhythmgame.game.menu

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.widget.SeekBar
import com.example.myrhythmgame.R
import com.example.myrhythmgame.databinding.SceneSettingsBinding
import com.example.myrhythmgame.game.BindingOverlayScene
import com.example.myrhythmgame.rhythm.audio.SoundEffectKey
import com.example.myrhythmgame.rhythm.audio.SoundPoolNoteSoundPlayer
import com.example.myrhythmgame.rhythm.settings.AudioSettings
import com.example.myrhythmgame.rhythm.settings.AudioSettingsRepository
import kr.ac.tukorea.ge.spgp2026.a2dg.view.GameContext

class SettingsScene(gctx: GameContext) : BindingOverlayScene(gctx) {
    private val repository = AudioSettingsRepository(gctx.view.context)
    private var settings = repository.load()
    private val effectPlayer = SoundPoolNoteSoundPlayer(
        context = gctx.view.context,
        soundResources = mapOf(SoundEffectKey.LaneTouch to R.raw.note_tap),
    ).also { it.setVolume(settings.effectVolume) }
    private val backgroundPaint = Paint().apply { color = Color.BLACK }

    override fun createOverlay(): View {
        return SceneSettingsBinding.inflate(LayoutInflater.from(gctx.view.context)).apply {
            musicVolumeSeekBar.progress = volumeProgress(settings.musicVolume)
            effectVolumeSeekBar.progress = volumeProgress(settings.effectVolume)
            updateLabels(this)

            musicVolumeSeekBar.setOnSeekBarChangeListener(volumeListener { volume ->
                settings = settings.copy(musicVolume = volume)
                saveAndRefreshLabels(this)
            })
            effectVolumeSeekBar.setOnSeekBarChangeListener(volumeListener { volume ->
                settings = settings.copy(effectVolume = volume)
                effectPlayer.setVolume(volume)
                saveAndRefreshLabels(this)
            })
            testEffectButton.setOnClickListener { effectPlayer.play(SoundEffectKey.LaneTouch) }
            backButton.setOnClickListener { pop() }
        }.root
    }

    override fun draw(canvas: Canvas) {
        canvas.drawRect(0f, 0f, gctx.metrics.width, gctx.metrics.height, backgroundPaint)
    }

    override fun onExit() {
        repository.save(settings)
        effectPlayer.release()
        super.onExit()
    }

    private fun saveAndRefreshLabels(binding: SceneSettingsBinding) {
        repository.save(settings)
        updateLabels(binding)
    }

    private fun updateLabels(binding: SceneSettingsBinding) {
        binding.musicVolumeLabel.text = text(
            R.string.setting_volume_format,
            text(R.string.setting_music_volume),
            volumeProgress(settings.musicVolume),
        )
        binding.effectVolumeLabel.text = text(
            R.string.setting_volume_format,
            text(R.string.setting_effect_volume),
            volumeProgress(settings.effectVolume),
        )
    }

    private fun volumeListener(onChanged: (Float) -> Unit): SeekBar.OnSeekBarChangeListener {
        return object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) onChanged(progress / 100f)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        }
    }

    private fun volumeProgress(volume: Float): Int {
        return (volume.coerceIn(AudioSettings.MIN_VOLUME, AudioSettings.MAX_VOLUME) * 100).toInt()
    }

    private fun text(resId: Int, vararg args: Any): String {
        return gctx.view.context.getString(resId, *args)
    }
}
