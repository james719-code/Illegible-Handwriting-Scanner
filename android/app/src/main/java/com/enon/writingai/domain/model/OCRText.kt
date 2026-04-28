package com.enon.writingai.domain.model

data class OCRText(
    val rawText: String,
    val normalizedText: String,
    val confidence: Float,
)
