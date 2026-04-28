package com.enon.writingai.feature.preprocessing

import androidx.lifecycle.ViewModel

class PreprocessViewModel : ViewModel() {
    val steps = listOf(
        "Boost contrast to recover faint pen strokes before recognition.",
        "Reduce paper grain and camera noise for cleaner character edges.",
        "Apply binarization to improve OCR stability on uneven lighting.",
    )
}
