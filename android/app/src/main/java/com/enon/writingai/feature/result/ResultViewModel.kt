package com.enon.writingai.feature.result

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.enon.writingai.OCRApplication
import com.enon.writingai.core.export.PdfExporter
import com.enon.writingai.core.export.TxtExporter
import com.enon.writingai.domain.model.ScanResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class ExportUiState(
    val inProgress: Boolean = false,
    val message: String? = null,
)

class ResultViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val appContainer = (application as OCRApplication).appContainer
    private val txtExporter = TxtExporter(application)
    private val pdfExporter = PdfExporter(application)

    var result by mutableStateOf<ScanResult?>(null)
        private set

    var exportState by mutableStateOf(ExportUiState())
        private set

    init {
        refreshLatest()
    }

    fun refreshLatest() {
        viewModelScope.launch {
            result = withContext(Dispatchers.IO) {
                appContainer.getHistoryUseCase().firstOrNull()
            }
        }
    }

    fun exportTxt() {
        exportCurrent("TXT") { scanResult -> txtExporter.export(scanResult) }
    }

    fun exportPdf() {
        exportCurrent("PDF") { scanResult -> pdfExporter.export(scanResult) }
    }

    private fun exportCurrent(
        label: String,
        exporter: (ScanResult) -> File,
    ) {
        val currentResult = result
        if (currentResult == null) {
            exportState = ExportUiState(message = "No scan result is ready to export.")
            return
        }

        viewModelScope.launch {
            exportState = ExportUiState(inProgress = true, message = "Exporting $label...")
            val exportResult = withContext(Dispatchers.IO) {
                runCatching { exporter(currentResult) }
            }
            exportState = exportResult.fold(
                onSuccess = { file ->
                    ExportUiState(message = "$label exported: ${file.name}")
                },
                onFailure = { exception ->
                    ExportUiState(message = "$label export failed: ${exception.message ?: "Unknown error"}")
                },
            )
        }
    }
}
