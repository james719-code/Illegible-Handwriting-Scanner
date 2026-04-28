package com.enon.writingai.core.image

import com.enon.writingai.domain.model.HandwritingSample

object NoiseReducer {
    fun apply(sample: HandwritingSample): HandwritingSample {
        return sample.copy(preprocessingNotes = sample.preprocessingNotes + "Noise reduced")
    }
}
