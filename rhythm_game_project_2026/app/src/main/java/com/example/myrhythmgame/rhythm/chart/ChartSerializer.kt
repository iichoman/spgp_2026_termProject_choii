/**
 * Chart 객체와 JSON 문자열 사이의 변환을 담당하는 직렬화 객체이다.
 *
 * 주요 기능:
 * - Chart 객체를 JSON 문자열로 변환하여 파일에 저장할 수 있게 한다.
 * - 저장된 JSON 문자열을 다시 Chart 객체로 복원한다.
 * - 곡 정보, 채보 정보, 타이밍 정보, 노트 목록을 하나의 JSON 구조로 관리한다.
 * - TapNote와 HoldNote를 노트 타입에 맞는 객체로 복원한다.
 * - 이전 버전 채보 파일의 필드도 읽을 수 있도록 하위 호환성을 지원한다.
 *
 * 저장되는 주요 데이터:
 * - song: 곡 제목, 가수, 음원 파일 경로
 * - chart: 채보 ID, 표시 이름, 난이도, 레인 수, 생성 및 수정 시간
 * - timing: BPM, 박자 오프셋, 한 박자당 스냅 분할 수
 * - notes: 노트 ID, 타입, 입력 시간, 스냅된 박자, 위치, 효과음 정보
 *
 * ChartSerializer는 실제 파일을 직접 읽거나 저장하지 않는다.
 * 파일 입출력은 ChartRepository가 담당하며,
 * 이 객체는 메모리의 Chart 데이터와 JSON 문자열 사이의 변환만 담당한다.
 *
 * JSON 최상위에 version 값을 기록하여 채보 저장 형식의 버전을 구분한다.
 * 이전 채보에서 사용하던 rawTimeMs 및 snappedBeat 필드도 읽을 수 있도록
 * 대체 값을 적용하여 기존 채보 파일과의 호환성을 유지한다.
 *
 * 현재 NoteType에는 Flick와 Slide도 정의되어 있지만,
 * 복원 시 Hold 타입을 제외한 노트는 TapNote로 처리한다.
 * 따라서 Flick 및 Slide 노트의 실제 구현을 추가할 때
 * noteFromJson 함수의 타입별 복원 로직도 함께 확장해야 한다.
 *
 * AI를 통해 구현하였다.
 */

package com.example.myrhythmgame.rhythm.chart

import com.example.myrhythmgame.rhythm.timing.TimingMap
import com.example.myrhythmgame.rhythm.lane.LaneMode
import org.json.JSONArray
import org.json.JSONObject

object ChartSerializer {
    private const val VERSION = 2

    fun toJson(chart: Chart): String {
        return JSONObject()
            .put("version", VERSION)
            .put(
                "song",
                JSONObject()
                    .put("title", chart.song.title)
                    .put("artist", chart.song.artist)
                    .put("audioPath", chart.song.audioPath)
            )
            .put(
                "chart",
                JSONObject()
                    .put("id", chart.metadata.id)
                    .put("displayName", chart.metadata.displayName)
                    .put("difficulty", chart.metadata.difficulty)
                    .put("laneCount", chart.metadata.laneCount)
                    .put("createdAt", chart.metadata.createdAt)
                    .put("updatedAt", chart.metadata.updatedAt)
            )
            .put(
                "timing",
                JSONObject()
                    .put("bpm", chart.timing.bpm)
                    .put("offsetMs", chart.timing.offsetMs)
                    .put("snapDivisionsPerBeat", chart.timing.snapDivisionsPerBeat)
            )
            .put("notes", notesToJson(chart.notes))
            .toString(2)
    }

    fun fromJson(json: String): Chart {
        val root = JSONObject(json)
        val song = root.getJSONObject("song")
        val songMetadata = SongMetadata(
            title = song.getString("title"),
            artist = song.optString("artist", ""),
            audioPath = song.optString("audioPath", ""),
        )
        val timing = root.getJSONObject("timing")
        val chartMetadata = chartMetadataFromJson(root.optJSONObject("chart"), songMetadata.title)

        return Chart(
            song = songMetadata,
            metadata = chartMetadata,
            timing = TimingMap(
                bpm = timing.getDouble("bpm"),
                offsetMs = timing.optLong("offsetMs", 0L),
                snapDivisionsPerBeat = timing.optInt(
                    "snapDivisionsPerBeat",
                    TimingMap.DEFAULT_SNAP_DIVISIONS_PER_BEAT,
                ),
            ),
            notes = notesFromJson(root.optJSONArray("notes") ?: JSONArray()),
        )
    }

    private fun chartMetadataFromJson(chart: JSONObject?, songTitle: String): ChartMetadata {
        if (chart == null) return ChartMetadata.defaultFor(songTitle)

        return ChartMetadata(
            id = chart.optString("id", "default"),
            displayName = chart.optString("displayName", songTitle.ifBlank { "New Chart" }),
            difficulty = chart.optInt("difficulty", ChartMetadata.DEFAULT_DIFFICULTY),
            laneCount = LaneMode.laneCountOrDefault(
                chart.optInt("laneCount", LaneMode.Default.laneCount)
            ),
            createdAt = chart.optLong("createdAt", 0L),
            updatedAt = chart.optLong("updatedAt", 0L),
        )
    }

    private fun notesToJson(notes: List<ChartNote>): JSONArray {
        val array = JSONArray()
        for (note in notes) {
            array.put(
                JSONObject()
                    .put("id", note.id)
                    .put("type", note.type.name)
                    .put("rawStartTimeMs", note.rawStartTimeMs)
                    .put("snappedStartBeat", note.snappedStartBeat)
                    .put("rawEndTimeMs", note.rawEndTimeMs)
                    .put("snappedEndBeat", note.snappedEndBeat)
                    .put("normalizedX", note.normalizedX.toDouble())
                    .put("durationMs", note.durationMs)
                    .put("soundKey", note.soundKey)
                    .put("effectKey", note.effectKey)
            )
        }
        return array
    }

    private fun notesFromJson(array: JSONArray): MutableList<ChartNote> {
        val notes = mutableListOf<ChartNote>()
        for (i in 0 until array.length()) {
            val item = array.getJSONObject(i)
            notes.add(noteFromJson(item))
        }
        return notes
    }

    private fun noteFromJson(item: JSONObject): ChartNote {
        val type = NoteType.valueOf(item.getString("type"))
        val rawStartTimeMs = item.optLong("rawStartTimeMs", item.optLong("rawTimeMs"))
        val snappedStartBeat = when {
            item.has("snappedStartBeat") && !item.isNull("snappedStartBeat") -> item.getDouble("snappedStartBeat")
            item.has("snappedBeat") && !item.isNull("snappedBeat") -> item.getDouble("snappedBeat")
            else -> null
        }
        val normalizedX = item.getDouble("normalizedX").toFloat()
        val soundKey = if (item.isNull("soundKey")) null else item.optString("soundKey")
        val effectKey = if (item.isNull("effectKey")) null else item.optString("effectKey")

        return when (type) {
            NoteType.Hold -> HoldNote(
                id = item.getLong("id"),
                rawStartTimeMs = rawStartTimeMs,
                snappedStartBeat = snappedStartBeat,
                normalizedX = normalizedX,
                rawEndTimeMs = item.optLong(
                    "rawEndTimeMs",
                    rawStartTimeMs + item.optLong("durationMs", 0L),
                ),
                snappedEndBeat = if (item.isNull("snappedEndBeat")) null else item.getDouble("snappedEndBeat"),
                soundKey = soundKey ?: HoldNote.DEFAULT_SOUND_KEY,
                effectKey = effectKey ?: HoldNote.DEFAULT_EFFECT_KEY,
            )

            else -> TapNote(
                id = item.getLong("id"),
                rawStartTimeMs = rawStartTimeMs,
                snappedStartBeat = snappedStartBeat,
                normalizedX = normalizedX,
                soundKey = soundKey ?: TapNote.DEFAULT_SOUND_KEY,
                effectKey = effectKey ?: TapNote.DEFAULT_EFFECT_KEY,
            )
        }
    }
}
