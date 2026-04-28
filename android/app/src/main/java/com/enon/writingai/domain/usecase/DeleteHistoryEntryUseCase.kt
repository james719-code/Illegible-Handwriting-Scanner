package com.enon.writingai.domain.usecase

import com.enon.writingai.domain.repository.HistoryRepository

class DeleteHistoryEntryUseCase(
    private val historyRepository: HistoryRepository,
) {
    operator fun invoke(id: String) {
        historyRepository.deleteResult(id)
    }
}
