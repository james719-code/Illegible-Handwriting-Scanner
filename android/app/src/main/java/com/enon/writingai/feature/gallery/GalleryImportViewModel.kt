package com.enon.writingai.feature.gallery

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.enon.writingai.OCRApplication

data class GalleryImportUiState(
    val sourceHint: String = "Choose a handwriting image from your device to continue OCR.",
    val previewName: String = "No image selected yet",
    val selectedUri: String? = null,
)

class GalleryImportViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val appContainer = (application as OCRApplication).appContainer

    var uiState by mutableStateOf(GalleryImportUiState())
        private set

    fun onImagePicked(
        pickedUri: String,
        displayName: String,
    ) {
        appContainer.importImageUseCase(pickedUri)
        uiState = uiState.copy(
            previewName = displayName,
            selectedUri = pickedUri,
        )
    }
}
