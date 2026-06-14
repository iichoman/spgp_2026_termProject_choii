# MyRhythmGame Architecture Analysis

## Project Findings

### proj_TapTu

`proj_TapTu` is a compact Java rhythm game. Its rhythm model is useful as a reference, but it is not accurate enough to become the direct base for a real-time chart editor.

Important structure:

- `MainScene` owns scene state, background UI, note spawning, speed toggle, and song lifecycle.
- `Song` owns `MediaPlayer`, asset loading, note list loading, and sequential note popping.
- `Note` parses a simple fixed-lane note format: `N <lane> <millis>`.
- `NoteSprite` renders falling notes by comparing note time against scene music time.

Critical limitation:

`MainScene` advances `musicTime` by adding `GameView.frameTime`. This means gameplay time is driven by render/update frames, not by the actual audio playback clock. For a casual demo this is acceptable, but for rhythm gameplay and especially real-time chart recording it causes drift risk.

### a2dg

`a2dg` is the Kotlin a2dg extracted from CookieRun. It uses the `kr.ac.tukorea.ge.spgp2026.a2dg` package and exposes `BaseGameActivity`, `GameView`, `Scene`, `World`, and the related Kotlin object/resource helpers.

In `proj_MyRhythmGame`, this module is managed inside the project as Gradle module `:a2dg`.

### proj_CookieRun/a2dg

The Kotlin a2dg implementation lives under `proj_CookieRun/a2dg`.

It is a better base than the TapTu Java a2dg because it already has:

- `BaseGameActivity`
- `GameView`
- `GameContext`
- `GameMetrics`
- `Scene`
- `SceneStack`
- `World`
- Kotlin `IGameObject` / `ITouchable`
- better scene lifecycle separation
- virtual coordinate support
- touch coordinate conversion support
- object recycling without reflection-based construction

This is the source that the internal `a2dg` module is based on.

## Java To Kotlin Translation Feasibility

Direct translation is feasible for:

- song metadata model
- simple note model
- note spawn queue concept
- falling note visual formula
- scene/layer organization
- button-based UI controls
- asset-based sample loading

Direct translation is not recommended for:

- audio timing
- `musicTime += frameTime`
- fixed integer lane data
- `MediaPlayer` as the authoritative rhythm clock
- note animation driven by `System.currentTimeMillis`
- text note format as the final chart format

The TapTu game logic should be treated as a prototype, not as an engine.

## Rhythm-Specific Requirements

MyRhythmGame needs a separate rhythm domain layer above a2dg:

- audio clock
- latency calibration
- chart model
- editor input recorder
- beat grid / snap math
- playback renderer
- judgment model
- chart serializer

a2dg should remain responsible for:

- frame loop
- drawing
- scene stack
- coordinate transform
- touch event delivery
- lightweight game object containers

It should not own chart semantics or BPM math.

## Recommended Core Data Model

The internal chart format should not be lane-count based.

Recommended base types:

```kotlin
data class Chart(
    val metadata: SongMetadata,
    val timing: TimingMap,
    val notes: MutableList<ChartNote>,
)

data class SongMetadata(
    val title: String,
    val artist: String,
    val audioUri: String,
)

data class TimingMap(
    val bpmEvents: List<BpmEvent>,
    val offsetMs: Long,
)

data class BpmEvent(
    val beat: Double,
    val bpm: Double,
)

data class ChartNote(
    val id: Long,
    var type: NoteType,
    var rawTimeMs: Long,
    var snappedBeat: Double?,
    var normalizedX: Float,
    var durationMs: Long = 0L,
    var points: MutableList<SlidePoint> = mutableListOf(),
)

enum class NoteType {
    Tap,
    Hold,
    Flick,
    Slide,
}

data class SlidePoint(
    val timeMs: Long,
    val normalizedX: Float,
)
```

Important rule:

`rawTimeMs` and `normalizedX` should preserve the user's actual input. Snapping should produce derived values, not destroy original recording data.

## Audio Strategy

For the first prototype:

- Android `MediaPlayer` or `ExoPlayer` can be used to load and play mp3 files.
- The authoritative time must come from the audio player position, not accumulated frame time.

For serious latency-sensitive gameplay/editor recording:

- consider Oboe/AAudio for low-latency native audio
- consider FMOD if interactive audio tooling, robust seeking, and cross-device behavior matter more than dependency size
- keep an `AudioClock` interface so the implementation can be swapped later

Recommended interface:

```kotlin
interface AudioClock {
    val positionMs: Long
    val isPlaying: Boolean
    fun play()
    fun pause()
    fun seekTo(positionMs: Long)
}
```

Gameplay, editor preview, note spawning, judgment, and recording should all read from `AudioClock.positionMs`.

## Input Recording Strategy

Touch input should be recorded as raw events first:

```kotlin
data class RecordedTouchEvent(
    val pointerId: Int,
    val action: TouchAction,
    val audioTimeMs: Long,
    val normalizedX: Float,
    val normalizedY: Float,
)
```

Then convert event sequences into notes:

- down/up with short duration -> tap
- down/hold/up -> hold
- fast directional movement near release -> flick
- continuous drag with multiple points -> slide

This two-step approach is important because note type inference will evolve.

## BPM Grid And Snapping

Snapping should be a service, not a property hidden inside the note object.

Recommended responsibilities:

- convert time to beat
- convert beat to time
- find nearest grid division
- apply snap strength or threshold
- support variable BPM later

The first version can assume one BPM and one offset. The model should still allow multiple BPM events.

## Proposed proj_MyRhythmGame Structure

```text
app/src/main/java/com/example/myrhythmgame/
  app/
    MainActivity.kt
    RhythmGameActivity.kt

  game/
    RhythmScene.kt
    editor/
      ChartEditorScene.kt
      RecordingController.kt
      EditSelection.kt
      LoopRegion.kt
    play/
      PlaytestScene.kt
      JudgmentController.kt
    render/
      NoteRenderer.kt
      LaneRenderer.kt
      BeatGridRenderer.kt
      JudgmentLine.kt

  rhythm/
    audio/
      AudioClock.kt
      AndroidMediaAudioClock.kt
    chart/
      Chart.kt
      ChartNote.kt
      NoteType.kt
      ChartSerializer.kt
    timing/
      TimingMap.kt
      BeatMath.kt
      Snapper.kt
      LatencyConfig.kt
    input/
      RecordedTouchEvent.kt
      TouchRecorder.kt
      NoteInference.kt
    lane/
      LaneMapping.kt
      EqualDivisionLaneMapping.kt
```

## Migration Plan

1. Keep `a2dg` synced from `proj_CookieRun/a2dg`, not from `proj_TapTu/a2dg`.
2. Add `a2dg` as the `:a2dg` module in `proj_MyRhythmGame`. Done.
3. Add a game activity based on `BaseGameActivity`. Done as `RhythmGameActivity`.
4. Add a minimal `ChartEditorScene` that displays. Started:
   - judgment line
   - beat grid
   - touch position markers
   - current audio time
5. Add the rhythm domain model independent of rendering.
6. Add an `AudioClock` implementation.
7. Implement raw touch recording against `AudioClock.positionMs`.
8. Add snap math and lane mapping.
9. Add playtest mode after chart recording works.

## Decision

Use Kotlin a2dg from CookieRun as the visual/game-loop foundation, but do not copy TapTu's rhythm timing model directly.

TapTu provides useful reference behavior for:

- basic falling note presentation
- note pre-spawning
- simple song selection
- asset conventions

MyRhythmGame needs new rhythm architecture for:

- audio-clock-based timing
- raw input capture
- normalized position chart data
- BPM snap grid
- looped editor playback
- latency calibration
- non-fixed-lane chart representation
