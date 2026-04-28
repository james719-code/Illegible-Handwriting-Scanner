package com.enon.writingai.core.ocr

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import com.enon.writingai.domain.model.HandwritingSample
import com.enon.writingai.domain.model.OCRText
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class GeminiImageOcrEngine(
    private val context: Context,
    private val client: OkHttpClient,
    private val apiKey: String,
    private val model: String,
) : OCREngine {
    override val name: String = "Default AI"

    override fun recognize(sample: HandwritingSample): OCRText {
        val resolvedApiKey = apiKey.trim()
        val resolvedModel = model.trim().ifBlank { DEFAULT_MODEL }
        if (resolvedApiKey.isBlank()) {
            throw DefaultAiUnavailableException("Default AI is not configured.")
        }

        val uri = Uri.parse(sample.sourceUri)
        val imageBytes = readImageBytes(uri)
        val mimeType = resolveMimeType(uri, sample.displayName)
        val requestBody = buildRequestBody(imageBytes, mimeType)
        val request = Request.Builder()
            .url("$GEMINI_ENDPOINT_PREFIX/$resolvedModel:generateContent")
            .header("x-goog-api-key", resolvedApiKey)
            .header("Content-Type", "application/json")
            .post(requestBody)
            .build()

        client.newCall(request).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw DefaultAiUnavailableException("Default AI request failed (${response.code}).")
            }

            val json = runCatching { JSONObject(responseBody) }.getOrElse {
                throw DefaultAiUnavailableException("Default AI returned an unreadable response.")
            }
            val text = extractText(json)
            return OCRText(
                rawText = text,
                normalizedText = normalizeText(text),
                confidence = DEFAULT_CONFIDENCE,
            )
        }
    }

    private fun buildRequestBody(
        imageBytes: ByteArray,
        mimeType: String,
    ) = JSONObject()
        .put(
            "contents",
            JSONArray()
                .put(
                    JSONObject()
                        .put(
                            "parts",
                            JSONArray()
                                .put(
                                    JSONObject()
                                        .put(
                                            "inline_data",
                                            JSONObject()
                                                .put("mime_type", mimeType)
                                                .put(
                                                    "data",
                                                    Base64.encodeToString(imageBytes, Base64.NO_WRAP),
                                                ),
                                        ),
                                )
                                .put(JSONObject().put("text", OCR_PROMPT)),
                        ),
                ),
        )
        .put(
            "generationConfig",
            JSONObject()
                .put("temperature", 0)
                .put("candidateCount", 1),
        )
        .toString()
        .toRequestBody(JSON_MEDIA_TYPE)

    private fun extractText(json: JSONObject): String {
        val candidates = json.optJSONArray("candidates")
        if (candidates == null || candidates.length() == 0) {
            val blockReason = json.optJSONObject("promptFeedback")?.optString("blockReason").orEmpty()
            if (blockReason.isNotBlank()) {
                throw DefaultAiUnavailableException("Default AI blocked the image or prompt ($blockReason).")
            }
            return ""
        }

        val firstCandidate = candidates?.optJSONObject(0)
        val finishReason = firstCandidate?.optString("finishReason").orEmpty()
        if (finishReason in BLOCKING_FINISH_REASONS) {
            throw DefaultAiUnavailableException("Default AI stopped text extraction ($finishReason).")
        }

        val content = firstCandidate?.optJSONObject("content")
        val parts = content?.optJSONArray("parts")
        if (parts == null || parts.length() == 0) {
            return ""
        }

        return buildString {
            for (index in 0 until parts.length()) {
                val text = parts.optJSONObject(index)?.optString("text").orEmpty().trim()
                if (text.isNotBlank()) {
                    if (isNotEmpty()) appendLine()
                    append(text)
                }
            }
        }
    }

    private fun readImageBytes(uri: Uri): ByteArray {
        return runCatching {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        }.getOrNull()
            ?: throw DefaultAiUnavailableException("Default AI could not read the selected image.")
    }

    private fun resolveMimeType(uri: Uri, displayName: String): String {
        val resolverMimeType = context.contentResolver.getType(uri)
        if (resolverMimeType?.startsWith("image/") == true) {
            return resolverMimeType
        }

        val name = displayName.ifBlank { resolveDisplayName(uri) }.lowercase()
        return when {
            name.endsWith(".png") -> "image/png"
            name.endsWith(".webp") -> "image/webp"
            name.endsWith(".heic") -> "image/heic"
            else -> "image/jpeg"
        }
    }

    private fun resolveDisplayName(uri: Uri): String {
        if (uri.scheme == "content") {
            val projection = arrayOf(OpenableColumns.DISPLAY_NAME)
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                val nameColumn = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameColumn >= 0 && cursor.moveToFirst()) {
                    return cursor.getString(nameColumn)
                }
            }
        }
        return uri.lastPathSegment.orEmpty()
    }

    private fun normalizeText(value: String): String {
        return value.lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .joinToString(separator = "\n")
    }

    private companion object {
        const val OCR_PROMPT =
            "You are an OCR engine for scanned handwritten pages. Extract every readable word from the image. Preserve line breaks where possible. Do not describe the image, do not summarize, and do not add labels. Return only the transcribed text. If no text is readable, return an empty response."
        const val DEFAULT_MODEL = "gemini-2.5-flash"
        const val GEMINI_ENDPOINT_PREFIX = "https://generativelanguage.googleapis.com/v1beta/models"
        const val DEFAULT_CONFIDENCE = 0.85f
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        val BLOCKING_FINISH_REASONS = setOf(
            "SAFETY",
            "RECITATION",
            "LANGUAGE",
            "BLOCKLIST",
            "PROHIBITED_CONTENT",
            "SPII",
            "IMAGE_SAFETY",
        )
    }
}

class DefaultAiUnavailableException(message: String) : IllegalStateException(message)
