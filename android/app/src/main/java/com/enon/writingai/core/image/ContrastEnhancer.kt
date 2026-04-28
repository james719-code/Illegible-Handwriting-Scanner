package com.enon.writingai.core.image

import com.enon.writingai.domain.model.HandwritingSample

object ContrastEnhancer {
    fun apply(sample: HandwritingSample): HandwritingSample {
        return sample.copy(preprocessingNotes = sample.preprocessingNotes + "Contrast enhanced")
    }
}
