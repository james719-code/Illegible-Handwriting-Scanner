package com.enon.writingai.domain.model

data class ScanResult(
    val id: String,
    val sourceName: String,
    val sourceUri: String? = null,
    val text: OCRText,
    val flaggedRegions: Int,
    val status: ProcessingStatus,
    val createdAt: String,
)
