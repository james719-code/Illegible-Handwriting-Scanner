package com.enon.writingai.domain.usecase

import com.enon.writingai.domain.model.HandwritingSample
import com.enon.writingai.domain.repository.ImageRepository

class ImportImageUseCase(
    private val imageRepository: ImageRepository,
) {
    operator fun invoke(sourceUri: String): HandwritingSample = imageRepository.importImage(sourceUri)
}
