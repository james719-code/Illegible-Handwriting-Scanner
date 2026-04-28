package com.enon.writingai.domain.usecase

import com.enon.writingai.domain.model.ScanResult
import com.enon.writingai.domain.repository.HistoryRepository

class GetHistoryUseCase(
    private val historyRepository: HistoryRepository,
) {
    operator fun invoke(): List<ScanResult> = historyRepository.getHistory()
}
