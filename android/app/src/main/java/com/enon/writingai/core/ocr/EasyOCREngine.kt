package com.enon.writingai.core.ocr

import com.enon.writingai.domain.model.HandwritingSample
import com.enon.writingai.domain.model.OCRText

class EasyOCREngine : OCREngine {
    override val name: String = "Default AI"

    override fun recognize(sample: HandwritingSample): OCRText {
        return OCRText(
            rawText = "",
            normalizedText = "",
            confidence = 0f,
        )
    }
}
