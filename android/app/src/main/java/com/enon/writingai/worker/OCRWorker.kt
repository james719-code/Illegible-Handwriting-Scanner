package com.enon.writingai.worker

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf

class OCRWorker(
    appContext: Context,
    params: WorkerParameters,
) : Worker(appContext, params) {
    override fun doWork(): Result {
        return Result.success(
            workDataOf(
                "status" to "queued",
                "message" to "OCR worker is queued and ready to process the latest capture.",
            ),
        )
    }
}
