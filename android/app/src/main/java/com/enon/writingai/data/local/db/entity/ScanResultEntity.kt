package com.enon.writingai.data.local.db.entity

data class ScanResultEntity(
    val id: String,
    val sourceName: String,
    val rawText: String,
    val normalizedText: String,
    val confidence: Float,
    val flaggedRegions: Int,
    val status: String,
    val createdAt: String,
    val sourceUri: String? = null,
)
