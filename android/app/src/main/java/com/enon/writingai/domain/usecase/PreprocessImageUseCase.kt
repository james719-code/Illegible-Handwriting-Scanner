package com.enon.writingai.domain.usecase

import com.enon.writingai.domain.model.HandwritingSample
import com.enon.writingai.domain.repository.ImageRepository

class PreprocessImageUseCase(
    private val imageRepository: ImageRepository,
) {
    operator fun invoke(
        sample: HandwritingSample,
        onStage: ((label: String, progress: Int) -> Unit)? = null,
    ): HandwritingSample = imageRepository.preprocessImage(sample, onStage)
}
