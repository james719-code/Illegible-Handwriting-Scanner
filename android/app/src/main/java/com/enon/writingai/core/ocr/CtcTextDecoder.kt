package com.enon.writingai.core.ocr

import android.content.Context
import com.enon.writingai.data.source.ModelAssetSource
import org.json.JSONObject

data class OcrVocabulary(
    val tokens: List<String>,
    val blankIndex: Int,
)

object CtcTextDecoder {
    fun loadVocabulary(context: Context): OcrVocabulary {
        val payload = context.assets.open(ModelAssetSource.HANDWRITING_OCR_VOCABULARY)
            .bufferedReader()
            .use { it.readText() }
        val json = JSONObject(payload)
        val tokensJson = json.getJSONArray("tokens")
        val tokens = List(tokensJson.length()) { index -> tokensJson.getString(index) }
        return OcrVocabulary(
            tokens = tokens,
            blankIndex = json.optInt("blank_index", tokens.size),
        )
    }

    fun decode(
        probabilities: Array<FloatArray>,
        vocabulary: OcrVocabulary,
    ): String {
        val builder = StringBuilder()
        var previousIndex = -1
        val padIndex = vocabulary.tokens.indexOf("<PAD>")
        val unknownIndex = vocabulary.tokens.indexOf("<UNK>")

        probabilities.forEach { timestep ->
            val index = timestep.indices.maxBy { timestep[it] }
            if (index == previousIndex) {
                return@forEach
            }
            previousIndex = index
            when (index) {
                vocabulary.blankIndex, padIndex -> Unit
                unknownIndex -> builder.append('?')
                in vocabulary.tokens.indices -> builder.append(vocabulary.tokens[index])
            }
        }

        return builder.toString().trimEnd()
    }
}
