package com.enon.writingai.core.ocr

import android.content.Context
import com.enon.writingai.data.source.ModelAssetSource
import com.enon.writingai.domain.model.HandwritingSample
import com.enon.writingai.domain.model.OCRText

class TrainedModelOcrEngine(
    private val context: Context,
) : OCREngine {
    override val name: String = "Default AI"

    override fun recognize(sample: HandwritingSample): OCRText {
        if (!hasBundledModel()) {
            throw MissingLocalOcrModelException("Default AI in use. Local handwriting model is not bundled.")
        }

        return OCRText(
            rawText = "",
            normalizedText = "",
            confidence = 0f,
        )
    }

    private fun hasBundledModel(): Boolean {
        return runCatching {
            context.assets.open(modelAssetPath).use { true }
            context.assets.open(vocabularyAssetPath).use { true }
        }.getOrDefault(false)
    }

    companion object {
        val modelAssetPath: String = ModelAssetSource.HANDWRITING_OCR_MODEL
        val vocabularyAssetPath: String = ModelAssetSource.HANDWRITING_OCR_VOCABULARY
    }
}
