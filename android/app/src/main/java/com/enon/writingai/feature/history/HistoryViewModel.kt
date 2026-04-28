package com.enon.writingai.feature.history

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.enon.writingai.OCRApplication
import com.enon.writingai.core.utils.IdGenerator
import com.enon.writingai.domain.model.OCRText
import com.enon.writingai.domain.model.ProcessingStatus
import com.enon.writingai.domain.model.ScanResult
import com.enon.writingai.domain.model.displayTitle
import com.enon.writingai.domain.model.hasUsableAnalysis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class HistoryViewMode {
    List,
    Card,
}

data class HistoryEntry(
    val id: String,
    val title: String,
    val subtitle: String,
    val imageUri: String?,
)

data class HistoryUiState(
    val mode: HistoryViewMode = HistoryViewMode.List,
    val entries: List<HistoryEntry> = emptyList(),
)

class HistoryViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val appContainer = (application as OCRApplication).appContainer

    var uiState by mutableStateOf(HistoryUiState())
        private set

    init {
        refreshEntries()
    }

    fun setMode(mode: HistoryViewMode) {
        uiState = uiState.copy(mode = mode)
    }

    fun createEntry(
        title: String,
        imageUri: String?,
    ) {
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty()) {
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            appContainer.saveResultUseCase(
                ScanResult(
                    id = IdGenerator.next("history"),
                    sourceName = cleanTitle,
                    sourceUri = imageUri?.trim()?.ifEmpty { null },
                    text = OCRText(
                        rawText = cleanTitle,
                        normalizedText = cleanTitle,
                        confidence = 1f,
                    ),
                    flaggedRegions = 0,
                    status = ProcessingStatus.Completed,
                    createdAt = LocalDateTime.now().format(dateFormatter),
                ),
            )
            refreshEntries()
        }
    }

    fun updateEntryTitle(
        id: String,
        newTitle: String,
    ) {
        val cleanTitle = newTitle.trim()
        if (cleanTitle.isEmpty()) {
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val current = appContainer.getHistoryUseCase().firstOrNull { it.id == id } ?: return@launch
            appContainer.saveResultUseCase(
                current.copy(
                    sourceName = cleanTitle,
                    text = current.text.copy(normalizedText = cleanTitle),
                ),
            )
            refreshEntries()
        }
    }

    fun deleteEntry(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            appContainer.deleteHistoryEntryUseCase(id)
            refreshEntries()
        }
    }

    fun refreshEntries() {
        viewModelScope.launch {
            val mapped = withContext(Dispatchers.IO) {
                appContainer.getHistoryUseCase().map { result ->
                    HistoryEntry(
                        id = result.id,
                        title = result.displayTitle(),
                        subtitle = if (result.hasUsableAnalysis()) {
                            "${result.createdAt} | Confidence ${String.format(Locale.US, "%.2f", result.text.confidence)}"
                        } else {
                            result.createdAt
                        },
                        imageUri = result.sourceUri,
                    )
                }
            }
            uiState = uiState.copy(entries = mapped)
        }
    }

    private companion object {
        val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    }
}
