package com.enon.writingai.domain.usecase

import com.enon.writingai.domain.model.ScanResult
import com.enon.writingai.domain.repository.HistoryRepository

class SaveResultUseCase(
    private val historyRepository: HistoryRepository,
) {
    operator fun invoke(result: ScanResult) {
        historyRepository.saveResult(result)
    }
}
