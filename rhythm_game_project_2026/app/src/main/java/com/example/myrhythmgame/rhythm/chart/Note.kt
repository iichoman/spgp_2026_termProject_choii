package com.example.myrhythmgame.rhythm.chart
// 노트 클래스 정의
// 탭노트, 롱노트
// 플릭, 슬라이드는 구현되지 않음
enum class NoteType {
    Tap,
    Hold,
    Flick,
    Slide,
}

// 저장될 노트 정보 관리
sealed class ChartNote(
    val id: Long,
    val type: NoteType,
    rawStartTimeMs: Long,
    snappedStartBeat: Double?,
    normalizedX: Float,
    val soundKey: String? = null,
    val effectKey: String? = null,
) {
    // 실제 입력된 시간
    var rawStartTimeMs: Long = rawStartTimeMs
        set(value) {
            field = value
            ensureEndTimeIsNotBeforeStart()
        }

    // 보정된 비트
    var snappedStartBeat: Double? = snappedStartBeat

    var normalizedX: Float = normalizedX.coerceIn(0f, 1f)
        set(value) {
            field = value.coerceIn(0f, 1f)
        }

    open var rawEndTimeMs: Long?
        get() = null
        set(_) = Unit

    open var snappedEndBeat: Double?
        get() = null
        set(_) = Unit

    open var durationMs: Long
        get() = rawEndTimeMs?.let { (it - rawStartTimeMs).coerceAtLeast(0L) } ?: 0L
        set(value) {
            rawEndTimeMs = rawStartTimeMs + value.coerceAtLeast(0L)
        }

    var rawTimeMs: Long
        get() = rawStartTimeMs
        set(value) {
            rawStartTimeMs = value
        }

    var snappedBeat: Double?
        get() = snappedStartBeat
        set(value) {
            snappedStartBeat = value
        }

    protected open fun ensureEndTimeIsNotBeforeStart() = Unit
}

class TapNote(
    id: Long,
    rawStartTimeMs: Long,
    snappedStartBeat: Double?,
    normalizedX: Float,
    soundKey: String? = DEFAULT_SOUND_KEY,
    effectKey: String? = DEFAULT_EFFECT_KEY,
) : ChartNote(
    id = id,
    type = NoteType.Tap,
    rawStartTimeMs = rawStartTimeMs,
    snappedStartBeat = snappedStartBeat,
    normalizedX = normalizedX,
    soundKey = soundKey,
    effectKey = effectKey,
) {
    companion object {
        const val DEFAULT_SOUND_KEY = "tap"
        const val DEFAULT_EFFECT_KEY = "tap"
    }
}

class HoldNote(
    id: Long,
    rawStartTimeMs: Long,
    snappedStartBeat: Double?,
    normalizedX: Float,
    rawEndTimeMs: Long,
    snappedEndBeat: Double?,
    soundKey: String? = DEFAULT_SOUND_KEY,
    effectKey: String? = DEFAULT_EFFECT_KEY,
) : ChartNote(
    id = id,
    type = NoteType.Hold,
    rawStartTimeMs = rawStartTimeMs,
    snappedStartBeat = snappedStartBeat,
    normalizedX = normalizedX,
    soundKey = soundKey,
    effectKey = effectKey,
) {
    override var rawEndTimeMs: Long? = rawEndTimeMs.coerceAtLeast(rawStartTimeMs)
        set(value) {
            field = value?.coerceAtLeast(rawStartTimeMs)
        }

    override var snappedEndBeat: Double? = snappedEndBeat

    override fun ensureEndTimeIsNotBeforeStart() {
        rawEndTimeMs = rawEndTimeMs
    }

    companion object {
        const val DEFAULT_SOUND_KEY = "hold"
        const val DEFAULT_EFFECT_KEY = "hold"
    }
}
