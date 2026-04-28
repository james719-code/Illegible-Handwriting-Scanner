package com.enon.writingai.feature.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

data class SettingsUiState(
    val enableBinarization: Boolean = true,
    val saveIntermediateImages: Boolean = false,
    val engineName: String = "Default AI in use",
)

class SettingsViewModel : ViewModel() {
    var uiState by mutableStateOf(SettingsUiState())
        private set

    fun toggleBinarization(checked: Boolean) {
        uiState = uiState.copy(enableBinarization = checked)
    }

    fun toggleIntermediateImages(checked: Boolean) {
        uiState = uiState.copy(saveIntermediateImages = checked)
    }
}
