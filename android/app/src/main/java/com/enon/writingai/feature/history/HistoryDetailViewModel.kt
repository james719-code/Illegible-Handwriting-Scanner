package com.enon.writingai.feature.history

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.enon.writingai.OCRApplication
import com.enon.writingai.domain.model.ScanResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HistoryDetailViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val appContainer = (application as OCRApplication).appContainer

    var entry by mutableStateOf<ScanResult?>(null)
        private set

    fun loadEntry(entryId: String) {
        if (entryId.isBlank()) {
            entry = null
            return
        }

        viewModelScope.launch {
            entry = withContext(Dispatchers.IO) {
                appContainer.getHistoryUseCase().firstOrNull { it.id == entryId }
            }
        }
    }
}
