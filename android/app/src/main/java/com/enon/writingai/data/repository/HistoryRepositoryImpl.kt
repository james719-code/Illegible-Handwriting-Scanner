package com.enon.writingai.data.repository

import com.enon.writingai.data.local.db.dao.HistoryDao
import com.enon.writingai.data.mapper.ScanResultMapper
import com.enon.writingai.domain.model.ScanResult
import com.enon.writingai.domain.repository.HistoryRepository

class HistoryRepositoryImpl(
    private val historyDao: HistoryDao,
) : HistoryRepository {
    override fun getHistory(): List<ScanResult> {
        return historyDao.getAll().map(ScanResultMapper::toDomain)
    }

    override fun saveResult(result: ScanResult) {
        historyDao.upsert(ScanResultMapper.toEntity(result))
    }

    override fun deleteResult(id: String) {
        historyDao.delete(id)
    }
}
