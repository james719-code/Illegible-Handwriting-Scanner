package com.enon.writingai.core.image

import com.enon.writingai.domain.model.HandwritingSample

object ImageCleaner {
    fun apply(sample: HandwritingSample): HandwritingSample {
        return sample.copy(preprocessingNotes = sample.preprocessingNotes + "Image cleaned")
    }
}
