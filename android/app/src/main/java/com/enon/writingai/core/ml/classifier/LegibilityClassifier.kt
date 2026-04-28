package com.enon.writingai.core.ml.classifier

import com.enon.writingai.domain.model.HandwritingSample

class LegibilityClassifier {
    fun classify(sample: HandwritingSample): ClassifierOutput {
        return ClassifierOutput(
            illegibilityScore = 0f,
            recommendation = "",
        )
    }
}
