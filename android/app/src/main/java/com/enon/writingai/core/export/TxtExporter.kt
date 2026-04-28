package com.enon.writingai.core.export

import android.content.Context
import com.enon.writingai.core.file.FileManager
import com.enon.writingai.domain.model.ScanResult
import java.io.File

class TxtExporter(
    context: Context,
    private val fileManager: FileManager = FileManager(context),
) {
    fun buildExportName(baseName: String): String = fileManager.exportFileName(baseName, "txt")

    fun export(result: ScanResult): File {
        val file = fileManager.createExportFile(result.sourceName, "txt")
        file.outputStream().bufferedWriter().use { writer ->
            writer.appendLine(result.sourceName)
            writer.appendLine("Created: ${result.createdAt}")
            writer.appendLine("Confidence: ${"%.1f".format(result.text.confidence * 100f)}%")
            writer.appendLine()
            writer.appendLine(result.text.normalizedText.ifBlank { result.text.rawText })
        }
        return file
    }
}
