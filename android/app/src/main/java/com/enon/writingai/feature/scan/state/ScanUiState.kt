package com.enon.writingai.feature.scan.state

import com.enon.writingai.domain.model.ProcessingStatus

data class ScanUiState(
    val status: ProcessingStatus = ProcessingStatus.Idle,
    val engineLabel: String = "Default AI in use",
    val progress: Int = 0,
    val stageLabel: String = "Ready",
    val note: String = "Ready to analyze the current handwriting sample.",
)
