package com.enon.writingai.domain.usecase

import com.enon.writingai.domain.model.HandwritingSample
import com.enon.writingai.domain.repository.ImageRepository

class CaptureImageUseCase(
    private val imageRepository: ImageRepository,
) {
    operator fun invoke(
        sourceUri: String? = null,
        displayName: String? = null,
    ): HandwritingSample = imageRepository.captureImage(sourceUri, displayName)
}
