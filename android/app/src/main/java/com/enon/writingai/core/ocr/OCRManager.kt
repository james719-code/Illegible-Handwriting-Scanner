package com.enon.writingai.core.ocr

import com.enon.writingai.domain.model.HandwritingSample
import com.enon.writingai.domain.model.OCRText

interface OCREngine {
    val name: String
    fun recognize(sample: HandwritingSample): OCRText
}

class OCRManager(
    private val primaryEngine: OCREngine,
    private val fallbackEngine: OCREngine,
) {
    fun run(sample: HandwritingSample): OCRText {
        return runCatching {
            primaryEngine.recognize(sample)
        }.getOrElse { exception ->
            if (exception is MissingLocalOcrModelException) {
                fallbackEngine.recognize(sample)
            } else {
                throw exception
            }
        }.takeIf { it.hasUsableText() }
            ?: fallbackEngine.recognize(sample)
    }
}

class MissingLocalOcrModelException(message: String) : IllegalStateException(message)

private fun OCRText.hasUsableText(): Boolean = normalizedText.isNotBlank() || rawText.isNotBlank()
