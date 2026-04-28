package com.enon.writingai.data.mapper

import com.enon.writingai.data.local.db.entity.ScanResultEntity
import com.enon.writingai.domain.model.OCRText
import com.enon.writingai.domain.model.ProcessingStatus
import com.enon.writingai.domain.model.ScanResult

object ScanResultMapper {
    fun toDomain(entity: ScanResultEntity): ScanResult {
        return ScanResult(
            id = entity.id,
            sourceName = entity.sourceName,
            sourceUri = entity.sourceUri,
            text = OCRText(
                rawText = entity.rawText,
                normalizedText = entity.normalizedText,
                confidence = entity.confidence,
            ),
            flaggedRegions = entity.flaggedRegions,
            status = ProcessingStatus.valueOf(entity.status),
            createdAt = entity.createdAt,
        )
    }

    fun toEntity(result: ScanResult): ScanResultEntity {
        return ScanResultEntity(
            id = result.id,
            sourceName = result.sourceName,
            rawText = result.text.rawText,
            normalizedText = result.text.normalizedText,
            confidence = result.text.confidence,
            flaggedRegions = result.flaggedRegions,
            status = result.status.name,
            createdAt = result.createdAt,
            sourceUri = result.sourceUri,
        )
    }
}
