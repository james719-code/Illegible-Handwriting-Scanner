package com.enon.writingai.core.ml.detector

import com.enon.writingai.domain.model.HandwritingSample

class RegionDetector {
    fun detect(sample: HandwritingSample): DetectionResult {
        return DetectionResult(
            flaggedRegionCount = 0,
            summary = "",
        )
    }
}
