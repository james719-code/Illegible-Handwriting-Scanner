package com.enon.writingai.core.export

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.enon.writingai.core.file.FileManager
import com.enon.writingai.domain.model.ScanResult
import java.io.File

class PdfExporter(
    context: Context,
    private val fileManager: FileManager = FileManager(context),
) {
    fun buildExportName(baseName: String): String = fileManager.exportFileName(baseName, "pdf")

    fun export(result: ScanResult): File {
        val file = fileManager.createExportFile(result.sourceName, "pdf")
        val document = PdfDocument()
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 12f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        }
        val metaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 10f
        }

        try {
            var pageNumber = 1
            var page = document.startPage(newPageInfo(pageNumber))
            var canvas = page.canvas
            var y = PAGE_MARGIN

            canvas.drawText(result.sourceName, PAGE_MARGIN, y, titlePaint)
            y += 22f
            canvas.drawText("Created: ${result.createdAt}", PAGE_MARGIN, y, metaPaint)
            y += 16f
            canvas.drawText("Confidence: ${"%.1f".format(result.text.confidence * 100f)}%", PAGE_MARGIN, y, metaPaint)
            y += 26f

            val lines = result.text.normalizedText.ifBlank { result.text.rawText }.lineSequence()
            lines.forEach { sourceLine ->
                var remaining = sourceLine.ifBlank { " " }
                while (remaining.isNotEmpty()) {
                    if (y > PAGE_HEIGHT - PAGE_MARGIN) {
                        document.finishPage(page)
                        pageNumber += 1
                        page = document.startPage(newPageInfo(pageNumber))
                        canvas = page.canvas
                        y = PAGE_MARGIN
                    }
                    val count = bodyPaint.breakText(
                        remaining,
                        true,
                        PAGE_WIDTH - (PAGE_MARGIN * 2),
                        null,
                    )
                    canvas.drawText(remaining.take(count), PAGE_MARGIN, y, bodyPaint)
                    remaining = remaining.drop(count)
                    y += 16f
                }
            }

            document.finishPage(page)
            file.outputStream().buffered().use { output ->
                document.writeTo(output)
            }
        } finally {
            document.close()
        }

        return file
    }

    private fun newPageInfo(pageNumber: Int): PdfDocument.PageInfo {
        return PdfDocument.PageInfo.Builder(PAGE_WIDTH.toInt(), PAGE_HEIGHT.toInt(), pageNumber).create()
    }

    private companion object {
        const val PAGE_WIDTH = 595f
        const val PAGE_HEIGHT = 842f
        const val PAGE_MARGIN = 48f
    }
}
