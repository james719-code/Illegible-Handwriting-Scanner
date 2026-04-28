package com.enon.writingai.domain.model

data class HandwritingSample(
    val id: String,
    val displayName: String,
    val sourceUri: String,
    val importedFromGallery: Boolean,
    val preprocessingNotes: List<String> = emptyList(),
)
