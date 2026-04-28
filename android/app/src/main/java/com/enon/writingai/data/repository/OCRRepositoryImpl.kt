package com.enon.writingai.data.repository

import com.enon.writingai.core.ml.classifier.LegibilityClassifier
import com.enon.writingai.core.ml.detector.RegionDetector
import com.enon.writingai.core.ocr.OCRManager
import com.enon.writingai.domain.model.HandwritingSample
import com.enon.writingai.domain.model.OCRText
import com.enon.writingai.domain.repository.OCRRepository

class OCRRepositoryImpl(
    private val ocrManager: OCRManager,
    private val legibilityClassifier: LegibilityClassifier,
    private val regionDetector: RegionDetector,
) : OCRRepository {
    override fun detectIllegibleText(sample: HandwritingSample): Int {
        val classifierOutput = legibilityClassifier.classify(sample)
        val detectionResult = regionDetector.detect(sample)
        return if (classifierOutput.illegibilityScore > 0.5f) {
            detectionResult.flaggedRegionCount + 1
        } else {
            detectionResult.flaggedRegionCount
        }
    }

    override fun runOCR(sample: HandwritingSample): OCRText = ocrManager.run(sample)
}
