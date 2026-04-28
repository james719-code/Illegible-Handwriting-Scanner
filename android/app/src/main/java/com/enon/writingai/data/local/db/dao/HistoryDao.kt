package com.enon.writingai.data.local.db.dao

import com.enon.writingai.data.local.db.entity.ScanResultEntity

interface HistoryDao {
    fun getAll(): List<ScanResultEntity>
    fun upsert(entity: ScanResultEntity)
    fun delete(id: String)
}
