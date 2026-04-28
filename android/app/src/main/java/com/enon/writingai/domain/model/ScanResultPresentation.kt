package com.enon.writingai.domain.model

import java.util.Locale

fun ScanResult.hasUsableOcrText(): Boolean {
    val normalizedText = text.normalizedText.toComparisonKey()
    if (normalizedText.isBlank()) {
        return false
    }

    val sourceKey = sourceName.substringBeforeLast('.').toComparisonKey()
    return normalizedText != sourceKey
}

fun ScanResult.hasUsableAnalysis(): Boolean = hasUsableOcrText() && text.confidence > 0f

fun ScanResult.displayTitle(): String {
    val cleaned = sourceName
        .substringBeforeLast('.')
        .replace('_', ' ')
        .replace('-', ' ')
        .trim()

    val normalized = cleaned.toComparisonKey()
    return when {
        normalized.isBlank() -> "Saved scan"
        normalized.matches(Regex("capture \\d{8} \\d{6}")) -> "Captured document"
        normalized.matches(Regex("import \\d{8} \\d{6}")) -> "Imported document"
        else -> cleaned.toTitleCase()
    }
}

private fun String.toComparisonKey(): String {
    return lowercase(Locale.US)
        .replace(Regex("[^a-z0-9]+"), " ")
        .trim()
}

private fun String.toTitleCase(): String {
    return split(Regex("\\s+"))
        .filter { it.isNotBlank() }
        .joinToString(" ") { part ->
            part.replaceFirstChar { char ->
                if (char.isLowerCase()) {
                    char.titlecase(Locale.US)
                } else {
                    char.toString()
                }
            }
        }
}
