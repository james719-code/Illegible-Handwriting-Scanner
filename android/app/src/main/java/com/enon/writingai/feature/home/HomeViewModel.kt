package com.enon.writingai.feature.home

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.enon.writingai.OCRApplication
import com.enon.writingai.domain.model.ProcessingStatus
import com.enon.writingai.domain.model.ScanResult
import com.enon.writingai.domain.model.hasUsableAnalysis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

data class HomeAction(
    val title: String,
    val subtitle: String,
)

data class HomeDashboardData(
    val heroTag: String,
    val regionLabel: String,
    val processedCount: String,
    val processedCaption: String,
    val averageConfidenceValue: String,
    val confidenceLabel: String,
    val modelLabel: String,
    val spotlightTitle: String,
    val spotlightSubtitle: String,
)

class HomeViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val appContainer = (application as OCRApplication).appContainer

    var dashboard by mutableStateOf(defaultDashboard())
        private set

    val capture = HomeAction(
        title = "Scan",
        subtitle = "Camera capture",
    )
    val import = HomeAction(
        title = "Import",
        subtitle = "Gallery image",
    )
    val history = HomeAction(
        title = "History",
        subtitle = "Saved outputs",
    )
    val settings = HomeAction(
        title = "Settings",
        subtitle = "App controls",
    )

    init {
        refreshDashboard()
    }

    fun refreshDashboard() {
        viewModelScope.launch {
            val historyResults = withContext(Dispatchers.IO) {
                appContainer.getHistoryUseCase()
            }
            dashboard = historyResults.toDashboardData()
        }
    }

    private fun List<ScanResult>.toDashboardData(): HomeDashboardData {
        val completedResults = filter { it.status == ProcessingStatus.Completed }
        if (completedResults.isEmpty()) {
            return defaultDashboard()
        }

        val datedResults = completedResults.mapNotNull { result ->
            parseCreatedAt(result.createdAt)?.let { createdAt -> result to createdAt }
        }
        val today = LocalDate.now()
        val todayResults = datedResults.filter { (_, createdAt) -> createdAt.toLocalDate() == today }
        val resultSource = if (todayResults.isNotEmpty()) {
            todayResults.map { it.first }
        } else {
            completedResults
        }

        val analysableResults = resultSource.filter { it.hasUsableAnalysis() }

        val averageConfidence = analysableResults
            .map { it.text.confidence.toDouble() }
            .average()
            .takeIf { !it.isNaN() }

        val averageConfidenceValue = averageConfidence
            ?.let { "${(it * 100).roundToInt()}%" }
            ?: "--"

        val processedCount = if (todayResults.isNotEmpty()) {
            todayResults.size
        } else {
            completedResults.size
        }

        val processedCaption = when {
            todayResults.isNotEmpty() && processedCount == 1 -> "Page processed today"
            todayResults.isNotEmpty() -> "Pages processed today"
            completedResults.size == 1 -> "Saved page in history"
            else -> "Saved pages in history"
        }

        val latestCreatedAt = datedResults.maxByOrNull { it.second }?.second

        return HomeDashboardData(
            heroTag = if (completedResults.size == 1) {
                "1 saved scan"
            } else {
                "${completedResults.size} saved scans"
            },
            regionLabel = "Handwriting OCR Workspace",
            processedCount = processedCount.toString(),
            processedCaption = processedCaption,
            averageConfidenceValue = averageConfidenceValue,
            confidenceLabel = if (averageConfidence == null) "" else if (todayResults.isNotEmpty()) {
                "Today avg $averageConfidenceValue"
            } else {
                "Overall avg $averageConfidenceValue"
            },
            modelLabel = if (latestCreatedAt != null) {
                "Latest saved ${latestCreatedAt.format(timeFormatter)}"
            } else {
                ""
            },
            spotlightTitle = "Start a fresh scan",
            spotlightSubtitle = "Capture a clear page and process it with Default AI.",
        )
    }

    private fun parseCreatedAt(value: String): LocalDateTime? {
        return runCatching {
            LocalDateTime.parse(value, storageFormatter)
        }.getOrNull()
    }

    private companion object {
        val storageFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

        fun defaultDashboard() = HomeDashboardData(
            heroTag = "No saved scans yet",
            regionLabel = "Handwriting OCR Workspace",
            processedCount = "0",
            processedCaption = "Start with a capture or import",
            averageConfidenceValue = "--",
            confidenceLabel = "",
            modelLabel = "",
            spotlightTitle = "Start a fresh scan",
            spotlightSubtitle = "Capture a clear page and process it with Default AI.",
        )
    }
}
