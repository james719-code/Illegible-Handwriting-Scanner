package com.enon.writingai.core.image

import com.enon.writingai.domain.model.HandwritingSample

object Binarizer {
    fun apply(sample: HandwritingSample): HandwritingSample {
        return sample.copy(preprocessingNotes = sample.preprocessingNotes + "Binarization applied")
    }
}
