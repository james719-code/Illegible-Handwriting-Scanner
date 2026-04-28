package com.enon.writingai.core.file

import android.content.Context
import java.io.File

class FileManager(
    private val context: Context,
) {
    fun exportFileName(baseName: String, extension: String): String {
        val safeBaseName = baseName
            .ifBlank { "scan_result" }
            .replace(Regex("[^A-Za-z0-9._-]+"), "_")
            .trim('_')
            .ifBlank { "scan_result" }

        return "$safeBaseName.$extension"
    }

    fun createExportFile(baseName: String, extension: String): File {
        val exportDirectory = File(context.cacheDir, EXPORT_DIRECTORY).apply {
            mkdirs()
        }
        return File(exportDirectory, exportFileName(baseName, extension))
    }

    private companion object {
        const val EXPORT_DIRECTORY = "exports"
    }
}
