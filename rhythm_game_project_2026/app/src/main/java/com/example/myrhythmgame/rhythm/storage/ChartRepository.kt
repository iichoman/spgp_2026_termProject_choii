package com.example.myrhythmgame.rhythm.storage

import android.content.Context
import com.example.myrhythmgame.rhythm.chart.Chart
import com.example.myrhythmgame.rhythm.chart.ChartSerializer
import java.io.File
import java.io.IOException
import java.util.Locale

class ChartRepository(
    context: Context,
) {
    private val chartsDir = File(context.filesDir, CHARTS_DIR_NAME)

    fun listCharts(): List<ChartFileInfo> {
        val files = chartsDir
            .listFiles { file -> file.isFile && file.name.endsWith(CHART_FILE_EXTENSION) }
            .orEmpty()

        return files
            .map { file -> chartFileInfo(file) }
            .sortedWith(compareBy<ChartFileInfo> { it.song.audioPath }.thenBy { it.metadata.displayName })
    }

    fun listChartsForAudio(audioPath: String): List<ChartFileInfo> {
        return listCharts().filter { it.song.audioPath == audioPath }
    }

    fun load(fileName: String): Chart? {
        val file = chartFile(fileName)
        if (!file.exists()) return null

        return try {
            ChartSerializer.fromJson(file.readText())
        } catch (e: Exception) {
            throw ChartStorageException("Failed to load chart file: ${file.absolutePath}", e)
        }
    }

    fun save(fileName: String, chart: Chart): File {
        val now = System.currentTimeMillis()
        val chartToSave = chart.copy(
            metadata = chart.metadata.copy(
                createdAt = chart.metadata.createdAt.takeIf { it > 0L } ?: now,
                updatedAt = now,
            )
        )
        return try {
            if (!chartsDir.exists()) {
                chartsDir.mkdirs()
            }
            chartFile(fileName).also { file ->
                file.writeText(ChartSerializer.toJson(chartToSave))
            }
        } catch (e: IOException) {
            throw ChartStorageException("Failed to save chart file: ${chartFile(fileName).absolutePath}", e)
        } catch (e: RuntimeException) {
            throw ChartStorageException("Failed to serialize chart file: ${chartFile(fileName).absolutePath}", e)
        }
    }

    fun delete(fileName: String): Boolean {
        val file = chartFile(fileName)
        return !file.exists() || file.delete()
    }

    fun rename(fileName: String, displayName: String): Chart {
        val chart = load(fileName)
            ?: throw ChartStorageException("Chart file does not exist: ${chartFile(fileName).absolutePath}")
        val renamedChart = chart.copy(
            metadata = chart.metadata.copy(displayName = displayName.ifBlank { chart.metadata.displayName })
        )
        save(fileName, renamedChart)
        return renamedChart
    }

    fun createFileName(audioPath: String, chartId: String): String {
        val audioName = File(audioPath).nameWithoutExtension.ifBlank { "song" }
        return "${sanitizeFilePart(audioName)}_${sanitizeFilePart(chartId)}$CHART_FILE_EXTENSION"
    }

    fun createUniqueFileName(audioPath: String, displayName: String): String {
        val baseChartId = sanitizeFilePart(displayName.ifBlank { "new_chart" })
        var index = 1
        var candidate = createFileName(audioPath, baseChartId)
        while (chartFile(candidate).exists()) {
            candidate = createFileName(audioPath, "${baseChartId}_$index")
            index += 1
        }
        return candidate
    }

    fun pathFor(fileName: String): String {
        return chartFile(fileName).absolutePath
    }

    private fun chartFile(fileName: String): File {
        return File(chartsDir, fileName)
    }

    private fun chartFileInfo(file: File): ChartFileInfo {
        return try {
            val chart = ChartSerializer.fromJson(file.readText())
            ChartFileInfo(
                fileName = file.name,
                song = chart.song,
                metadata = chart.metadata,
                noteCount = chart.notes.size,
                updatedAt = file.lastModified(),
            )
        } catch (e: Exception) {
            throw ChartStorageException("Failed to read chart summary: ${file.absolutePath}", e)
        }
    }

    private fun sanitizeFilePart(value: String): String {
        val sanitized = value
            .lowercase(Locale.US)
            .replace(Regex("[^a-z0-9_-]+"), "_")
            .trim('_')
        return sanitized.ifBlank { "chart" }
    }

    companion object {
        private const val CHARTS_DIR_NAME = "charts"
        private const val CHART_FILE_EXTENSION = ".chart.json"
    }
}
