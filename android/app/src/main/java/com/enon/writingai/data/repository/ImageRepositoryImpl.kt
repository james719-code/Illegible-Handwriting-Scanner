package com.enon.writingai.data.repository

import com.enon.writingai.core.constants.AppConstants
import com.enon.writingai.core.image.Binarizer
import com.enon.writingai.core.image.ContrastEnhancer
import com.enon.writingai.core.image.ImageCleaner
import com.enon.writingai.core.image.NoiseReducer
import com.enon.writingai.core.utils.IdGenerator
import com.enon.writingai.data.local.cache.ProcessingCache
import com.enon.writingai.domain.model.HandwritingSample
import com.enon.writingai.domain.repository.ImageRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ImageRepositoryImpl(
    private val processingCache: ProcessingCache,
) : ImageRepository {
    override fun captureImage(
        sourceUri: String?,
        displayName: String?,
    ): HandwritingSample {
        val defaultFileName = "${AppConstants.CAPTURE_FILE_PREFIX}_${timestampStamp()}.jpg"
        val resolvedName = displayName ?: defaultFileName
        val resolvedUri = sourceUri ?: "file:///captures/$resolvedName"
        return HandwritingSample(
            id = IdGenerator.next("sample"),
            displayName = resolvedName,
            sourceUri = resolvedUri,
            importedFromGallery = false,
        ).also { processingCache.currentSample = it }
    }

    override fun importImage(sourceUri: String): HandwritingSample {
        val fallbackName = "${AppConstants.IMPORTED_FILE_PREFIX}_${timestampStamp()}.jpg"
        val displayName = sourceUri.substringAfterLast('/').ifBlank { fallbackName }
        return HandwritingSample(
            id = IdGenerator.next("sample"),
            displayName = displayName,
            sourceUri = sourceUri,
            importedFromGallery = true,
        ).also { processingCache.currentSample = it }
    }

    override fun preprocessImage(
        sample: HandwritingSample,
        onStage: ((label: String, progress: Int) -> Unit)?,
    ): HandwritingSample {
        onStage?.invoke("Cleaning image", 16)
        val cleaned = ImageCleaner.apply(sample)

        onStage?.invoke("Enhancing contrast", 34)
        val contrasted = ContrastEnhancer.apply(cleaned)

        onStage?.invoke("Reducing noise", 52)
        val denoised = NoiseReducer.apply(contrasted)

        onStage?.invoke("Applying binarization", 66)
        val processed = Binarizer.apply(denoised)

        processingCache.currentSample = processed
        return processed
    }

    private fun timestampStamp(): String {
        return SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    }
}
