package com.example.myrhythmgame.rhythm.chart

import com.example.myrhythmgame.rhythm.timing.TimingMap

data class Chart(
    val song: SongMetadata,
    val metadata: ChartMetadata = ChartMetadata.defaultFor(song.title),
    val timing: TimingMap,
    val notes: MutableList<ChartNote> = mutableListOf(),
) {
    constructor(
        song: SongMetadata,
        timing: TimingMap,
        notes: MutableList<ChartNote> = mutableListOf(),
    ) : this(
        song = song,
        metadata = ChartMetadata.defaultFor(song.title),
        timing = timing,
        notes = notes,
    )
}
