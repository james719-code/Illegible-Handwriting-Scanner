package com.enon.writingai.domain.usecase

import com.enon.writingai.domain.model.HandwritingSample
import com.enon.writingai.domain.repository.OCRRepository

class DetectIllegibleTextUseCase(
    private val ocrRepository: OCRRepository,
) {
    operator fun invoke(sample: HandwritingSample): Int = ocrRepository.detectIllegibleText(sample)
}
