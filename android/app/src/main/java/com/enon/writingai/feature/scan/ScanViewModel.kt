package com.enon.writingai.feature.scan

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.enon.writingai.OCRApplication
import com.enon.writingai.core.utils.IdGenerator
import com.enon.writingai.domain.model.ProcessingStatus
import com.enon.writingai.domain.model.ScanResult
import com.enon.writingai.domain.model.hasUsableOcrText
import com.enon.writingai.feature.scan.state.ScanUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ScanViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val appContainer = (application as OCRApplication).appContainer

    var uiState by mutableStateOf(ScanUiState())
        private set

    private var processingJob: Job? = null

    fun advanceScan() {
        when (uiState.status) {
            ProcessingStatus.Scanning -> return
            ProcessingStatus.Completed -> {
                reset()
                startScan()
            }
            else -> startScan()
        }
    }

    private fun startScan() {
        processingJob?.cancel()

        val sample = appContainer.processingCacheStore.currentSample
        if (sample == null) {
            uiState = uiState.copy(
                status = ProcessingStatus.Failed,
                stageLabel = "No image",
                note = "Capture or import an image before starting processing.",
                progress = 0,
            )
            return
        }

        processingJob = viewModelScope.launch {
            try {
                publishProgress(
                    progress = 8,
                    label = "Preparing image",
                    note = "Loading selected handwriting image.",
                )

                val preprocessed = appContainer.preprocessImageUseCase(sample) { label, progress ->
                    publishProgress(
                        progress = progress,
                        label = label,
                        note = "Applying $label...",
                    )
                }

                publishProgress(
                    progress = 76,
                    label = "Illegibility analysis",
                    note = "Running handwritten legibility analysis.",
                )
                val flaggedRegions = appContainer.detectIllegibleTextUseCase(preprocessed)

                publishProgress(
                    progress = 90,
                    label = "Extracting text",
                    note = "Default AI in use for text extraction.",
                )
                val extractedText = withContext(Dispatchers.IO) {
                    appContainer.runOCRUseCase(preprocessed)
                }

                publishProgress(
                    progress = 96,
                    label = "Saving history",
                    note = "Storing result in local history database.",
                )
                appContainer.saveResultUseCase(
                    ScanResult(
                        id = IdGenerator.next("result"),
                        sourceName = preprocessed.displayName,
                        sourceUri = preprocessed.sourceUri,
                        text = extractedText,
                        flaggedRegions = flaggedRegions,
                        status = ProcessingStatus.Completed,
                        createdAt = LocalDateTime.now().format(dateFormatter),
                    ),
                )

                publishProgress(
                    progress = 100,
                    label = "Completed",
                    note = if (
                        ScanResult(
                            id = "",
                            sourceName = preprocessed.displayName,
                            sourceUri = preprocessed.sourceUri,
                            text = extractedText,
                            flaggedRegions = flaggedRegions,
                            status = ProcessingStatus.Completed,
                            createdAt = "",
                        ).hasUsableOcrText()
                    ) {
                        "Processing complete. Review the extracted text."
                    } else {
                        "Processing complete. The image was saved, but no OCR text is available for this result."
                    },
                    status = ProcessingStatus.Completed,
                )
            } catch (exception: Exception) {
                publishProgress(
                    progress = 0,
                    label = "Failed",
                    note = "Processing failed: ${exception.message ?: "Unknown error"}",
                    status = ProcessingStatus.Failed,
                )
            }
        }
    }

    private fun publishProgress(
        progress: Int,
        label: String,
        note: String,
        status: ProcessingStatus = ProcessingStatus.Scanning,
    ) {
        uiState = uiState.copy(
            status = status,
            progress = progress,
            stageLabel = label,
            note = note,
        )
    }

    fun reset() {
        processingJob?.cancel()
        uiState = ScanUiState()
    }

    override fun onCleared() {
        processingJob?.cancel()
        super.onCleared()
    }

    private companion object {
        val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    }
}
