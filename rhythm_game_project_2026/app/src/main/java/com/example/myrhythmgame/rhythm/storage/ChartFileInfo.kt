package com.example.myrhythmgame.rhythm.storage

import com.example.myrhythmgame.rhythm.chart.ChartMetadata
import com.example.myrhythmgame.rhythm.chart.SongMetadata

data class ChartFileInfo(
    val fileName: String,
    val song: SongMetadata,
    val metadata: ChartMetadata,
    val noteCount: Int,
    val updatedAt: Long,
)
