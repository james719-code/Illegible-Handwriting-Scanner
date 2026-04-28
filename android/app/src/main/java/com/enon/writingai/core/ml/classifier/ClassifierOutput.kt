package com.enon.writingai.core.ml.classifier

data class ClassifierOutput(
    val illegibilityScore: Float,
    val recommendation: String,
)
