package com.enon.writingai.feature.capture

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.enon.writingai.OCRApplication

data class CaptureUiState(
    val guidance: String = "Frame the page edge-to-edge and keep the text inside the guide area.",
    val cameraStatus: String = "Waiting for camera permission.",
    val outputName: String = "No capture yet",
    val capturedUri: String? = null,
)

class CaptureViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val appContainer = (application as OCRApplication).appContainer

    var uiState by mutableStateOf(CaptureUiState())
        private set

    val canContinue: Boolean
        get() = uiState.capturedUri != null

    fun onCameraReady() {
        uiState = uiState.copy(
            cameraStatus = "Camera is ready. Keep your device steady and tap capture.",
        )
    }

    fun onCameraError(message: String) {
        uiState = uiState.copy(cameraStatus = message)
    }

    fun onPhotoCaptured(
        capturedUri: String,
        outputName: String,
    ) {
        appContainer.captureImageUseCase(
            sourceUri = capturedUri,
            displayName = outputName,
        )
        uiState = uiState.copy(
            cameraStatus = "Capture complete. Review and continue.",
            outputName = outputName,
            capturedUri = capturedUri,
        )
    }

    fun retakeCapture() {
        uiState = uiState.copy(
            guidance = "Frame the page edge-to-edge and keep the text inside the guide area.",
            cameraStatus = "Review dismissed. Capture another photo when ready.",
            outputName = "No capture yet",
            capturedUri = null,
        )
    }
}
