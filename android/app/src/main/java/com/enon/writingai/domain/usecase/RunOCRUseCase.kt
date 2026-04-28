package com.enon.writingai.domain.usecase

import com.enon.writingai.domain.model.HandwritingSample
import com.enon.writingai.domain.model.OCRText
import com.enon.writingai.domain.repository.OCRRepository

class RunOCRUseCase(
    private val ocrRepository: OCRRepository,
) {
    operator fun invoke(sample: HandwritingSample): OCRText = ocrRepository.runOCR(sample)
}
