package com.enon.writingai.domain.repository

import com.enon.writingai.domain.model.ScanResult

interface HistoryRepository {
    fun getHistory(): List<ScanResult>
    fun saveResult(result: ScanResult)
    fun deleteResult(id: String)
}
