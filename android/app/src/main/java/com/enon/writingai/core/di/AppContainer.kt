package com.enon.writingai.core.di

import android.app.Application
import com.enon.writingai.BuildConfig
import com.enon.writingai.core.ml.classifier.LegibilityClassifier
import com.enon.writingai.core.ml.detector.RegionDetector
import com.enon.writingai.core.ocr.GeminiImageOcrEngine
import com.enon.writingai.core.ocr.OCRManager
import com.enon.writingai.core.ocr.TrainedModelOcrEngine
import com.enon.writingai.data.local.cache.ProcessingCache
import com.enon.writingai.data.local.db.AppDatabase
import com.enon.writingai.data.repository.HistoryRepositoryImpl
import com.enon.writingai.data.repository.ImageRepositoryImpl
import com.enon.writingai.data.repository.OCRRepositoryImpl
import com.enon.writingai.domain.repository.HistoryRepository
import com.enon.writingai.domain.repository.ImageRepository
import com.enon.writingai.domain.repository.OCRRepository
import com.enon.writingai.domain.usecase.CaptureImageUseCase
import com.enon.writingai.domain.usecase.DeleteHistoryEntryUseCase
import com.enon.writingai.domain.usecase.DetectIllegibleTextUseCase
import com.enon.writingai.domain.usecase.GetHistoryUseCase
import com.enon.writingai.domain.usecase.ImportImageUseCase
import com.enon.writingai.domain.usecase.PreprocessImageUseCase
import com.enon.writingai.domain.usecase.RunOCRUseCase
import com.enon.writingai.domain.usecase.SaveResultUseCase
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class AppContainer(
    application: Application,
) {
    private val networkClient = OkHttpClient.Builder()
        .connectTimeout(BuildConfig.GEMINI_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(BuildConfig.GEMINI_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(BuildConfig.GEMINI_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()
    private val appDatabase = AppDatabase(application)
    private val processingCache = ProcessingCache()
    private val ocrManager = OCRManager(
        primaryEngine = TrainedModelOcrEngine(application),
        fallbackEngine = GeminiImageOcrEngine(
            context = application,
            client = networkClient,
            apiKey = BuildConfig.GEMINI_API_KEY,
            model = BuildConfig.GEMINI_MODEL,
        ),
    )
    private val legibilityClassifier = LegibilityClassifier()
    private val regionDetector = RegionDetector()

    val imageRepository: ImageRepository = ImageRepositoryImpl(processingCache)
    val ocrRepository: OCRRepository = OCRRepositoryImpl(
        ocrManager = ocrManager,
        legibilityClassifier = legibilityClassifier,
        regionDetector = regionDetector,
    )
    val historyRepository: HistoryRepository = HistoryRepositoryImpl(appDatabase.historyDao)

    val captureImageUseCase = CaptureImageUseCase(imageRepository)
    val importImageUseCase = ImportImageUseCase(imageRepository)
    val preprocessImageUseCase = PreprocessImageUseCase(imageRepository)
    val detectIllegibleTextUseCase = DetectIllegibleTextUseCase(ocrRepository)
    val runOCRUseCase = RunOCRUseCase(ocrRepository)
    val saveResultUseCase = SaveResultUseCase(historyRepository)
    val getHistoryUseCase = GetHistoryUseCase(historyRepository)
    val deleteHistoryEntryUseCase = DeleteHistoryEntryUseCase(historyRepository)
    val processingCacheStore: ProcessingCache = processingCache

    init {
        // Keep a reference to the application for future DI expansion.
        application.packageName
    }
}
