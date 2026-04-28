package com.enon.writingai.domain.repository

import com.enon.writingai.domain.model.HandwritingSample

interface ImageRepository {
    fun captureImage(
        sourceUri: String? = null,
        displayName: String? = null,
    ): HandwritingSample
    fun importImage(sourceUri: String): HandwritingSample
    fun preprocessImage(
        sample: HandwritingSample,
        onStage: ((label: String, progress: Int) -> Unit)? = null,
    ): HandwritingSample
}
