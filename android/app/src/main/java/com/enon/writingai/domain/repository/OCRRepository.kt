package com.enon.writingai.domain.repository

import com.enon.writingai.domain.model.HandwritingSample
import com.enon.writingai.domain.model.OCRText

interface OCRRepository {
    fun detectIllegibleText(sample: HandwritingSample): Int
    fun runOCR(sample: HandwritingSample): OCRText
}
