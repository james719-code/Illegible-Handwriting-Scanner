package com.enon.writingai.feature.capture.camera

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ImageCaptureHelper(
    private val context: Context,
) {
    fun createOutputFile(): File {
        val captureDirectory = File(context.filesDir, "captures")
        if (!captureDirectory.exists()) {
            captureDirectory.mkdirs()
        }

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return File(captureDirectory, "capture_${timestamp}.jpg")
    }

    fun displayName(file: File): String = file.name
}
