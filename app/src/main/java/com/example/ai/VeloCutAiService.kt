package com.example.ai

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.media.AudioExtractionHelper
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

data class CaptionSegment(
    val startMs: Long,
    val endMs: Long,
    val text: String,
    val originalText: String? = null
)

sealed interface AiTaskResult<out T> {
    data class Success<T>(val data: T) : AiTaskResult<T>
    data class Error(val message: String) : AiTaskResult<Nothing>
}

class KomikNarasiAiService {

    private val captionSchema = Schema.obj(
        properties = mapOf(
            "captions" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "startMs" to Schema.integer(description = "Start timestamp in milliseconds"),
                        "endMs" to Schema.integer(description = "End timestamp in milliseconds"),
                        "text" to Schema.string(description = "Concise spoken subtitle phrase")
                    )
                ),
                description = "Time-synchronized subtitle segments"
            )
        )
    )

    private val speechTranslateSchema = Schema.obj(
        properties = mapOf(
            "captions" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "startMs" to Schema.integer(description = "Start timestamp in milliseconds"),
                        "endMs" to Schema.integer(description = "End timestamp in milliseconds"),
                        "originalText" to Schema.string(description = "Detected original spoken text"),
                        "translatedText" to Schema.string(description = "Natural translation in target language")
                    )
                ),
                description = "Time-synchronized speech-to-translate subtitle segments"
            )
        )
    )

    private val translateSchema = Schema.obj(
        properties = mapOf(
            "translations" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "id" to Schema.string(description = "Original segment ID"),
                        "translatedText" to Schema.string(description = "Translated subtitle text")
                    )
                )
            )
        )
    )

    private val captionModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = captionSchema
            }
        )
    }

    private val speechTranslateModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = speechTranslateSchema
            }
        )
    }

    private val translateModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = translateSchema
            }
        )
    }

    private val scriptModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash"
        )
    }

    suspend fun polishComicNarrationScript(rawDraft: String): AiTaskResult<String> =
        withContext(Dispatchers.IO) {
            val clean = rawDraft.trim()
            if (clean.isEmpty()) {
                return@withContext AiTaskResult.Error("Tulis ide cerita atau dialog komik terlebih dahulu.")
            }
            try {
                val response = withTimeout(45_000L) {
                    scriptModel.generateContent(
                        "Ubah teks berikut menjadi naskah narasi komik berbahasa Indonesia yang dramatis, mengalir, padat, dan siap dibacakan oleh narator video pendek (maksimal 2-3 kalimat, tanpa tanda kurung atau catatan panggung):\n$clean"
                    )
                }
                val resultText = response.text?.trim()
                if (resultText.isNullOrEmpty()) {
                    AiTaskResult.Error("AI tidak mengembalikan naskah narasi.")
                } else {
                    AiTaskResult.Success(resultText)
                }
            } catch (e: TimeoutCancellationException) {
                AiTaskResult.Error("Pembuatan naskah AI melebihi batas waktu.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Script polish failed", e)
                AiTaskResult.Error(e.localizedMessage ?: "Gagal menyusun naskah narasi AI.")
            }
        }

    private suspend fun extractAudioBytes(context: Context, mediaUri: Uri): Pair<ByteArray, String>? {
        val audioFile = AudioExtractionHelper.extractAudioToTempM4a(context, mediaUri)
        return when {
            audioFile != null && audioFile.length() in 1L..18_000_000L -> {
                val data = audioFile.readBytes()
                audioFile.delete()
                data to "audio/mp4"
            }
            else -> {
                audioFile?.delete()
                val fallbackBytes = context.contentResolver.openInputStream(mediaUri)?.use { input ->
                    val raw = input.readBytes()
                    if (raw.size <= 18_000_000) raw else null
                } ?: return null
                val mime = context.contentResolver.getType(mediaUri) ?: "video/mp4"
                fallbackBytes to mime
            }
        }
    }

    suspend fun generateAutoCaptions(
        context: Context,
        mediaUri: Uri,
        clipStartOffsetMs: Long,
        clipDurationMs: Long,
        languageHint: String = "Indonesian or English"
    ): AiTaskResult<List<CaptionSegment>> = withContext(Dispatchers.IO) {
        try {
            val bytes = extractAudioBytes(context, mediaUri)
                ?: return@withContext AiTaskResult.Error(
                    "Trek audio tidak ditemukan atau ukuran media melebihi batas 18MB."
                )

            val response = withTimeout(60_000L) {
                captionModel.generateContent(
                    content {
                        inlineData(bytes.first, bytes.second)
                        text(
                            "Transcribe all spoken speech in this audio into short, punchy video subtitle segments " +
                                "(max 7 words per segment). Primary language hint: $languageHint. " +
                                "Clip duration is ${clipDurationMs}ms. Return startMs and endMs relative to 0..${clipDurationMs}ms."
                        )
                    }
                )
            }

            val rawJson = response.text ?: return@withContext AiTaskResult.Error(
                "AI tidak mengembalikan hasil transkripsi."
            )
            val root = JSONObject(rawJson)
            val arr = root.getJSONArray("captions")
            val segments = (0 until arr.length()).mapNotNull { idx ->
                val obj = arr.getJSONObject(idx)
                val text = obj.optString("text").trim()
                if (text.isEmpty()) return@mapNotNull null
                val s = obj.optLong("startMs", idx * 2000L).coerceIn(0L, clipDurationMs)
                val e = obj.optLong("endMs", s + 2000L).coerceIn(s + 400L, clipDurationMs + 500L)
                CaptionSegment(
                    startMs = clipStartOffsetMs + s,
                    endMs = clipStartOffsetMs + e,
                    text = text
                )
            }
            if (segments.isEmpty()) {
                AiTaskResult.Error("Tidak ada ucapan yang terdeteksi di klip ini.")
            } else {
                AiTaskResult.Success(segments)
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Auto-caption timed out", e)
            AiTaskResult.Error("Auto-Caption melebihi batas waktu 60 detik. Coba lagi.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Auto-caption failed", e)
            AiTaskResult.Error(e.localizedMessage ?: "Gagal memproses Auto-Caption.")
        }
    }

    /**
     * Suara Otomatis ke Penerjemah:
     * Detects spoken audio in the clip, produces synchronized subtitle segments, and immediately
     * translates them into targetLanguage while preserving the original spoken transcript.
     */
    suspend fun transcribeAndTranslateVideo(
        context: Context,
        mediaUri: Uri,
        clipStartOffsetMs: Long,
        clipDurationMs: Long,
        targetLanguage: String
    ): AiTaskResult<List<CaptionSegment>> = withContext(Dispatchers.IO) {
        try {
            val bytes = extractAudioBytes(context, mediaUri)
                ?: return@withContext AiTaskResult.Error(
                    "Trek suara tidak ditemukan pada media ini atau ukuran melebihi 18MB."
                )

            val response = withTimeout(60_000L) {
                speechTranslateModel.generateContent(
                    content {
                        inlineData(bytes.first, bytes.second)
                        text(
                            "Listen to the spoken dialogue/narration in this audio. " +
                                "Segment the speech into concise, time-synced subtitle phrases (max 8 words per segment) " +
                                "within 0..${clipDurationMs}ms. For each segment, return 'originalText' in the spoken language " +
                                "and 'translatedText' translated naturally into $targetLanguage."
                        )
                    }
                )
            }

            val rawJson = response.text ?: return@withContext AiTaskResult.Error(
                "AI tidak mengembalikan hasil deteksi suara & terjemahan."
            )
            val root = JSONObject(rawJson)
            val arr = root.getJSONArray("captions")
            val segments = (0 until arr.length()).mapNotNull { idx ->
                val obj = arr.getJSONObject(idx)
                val translated = obj.optString("translatedText").trim()
                val original = obj.optString("originalText").trim()
                val finalText = translated.ifEmpty { original }
                if (finalText.isEmpty()) return@mapNotNull null
                val s = obj.optLong("startMs", idx * 2000L).coerceIn(0L, clipDurationMs)
                val e = obj.optLong("endMs", s + 2000L).coerceIn(s + 400L, clipDurationMs + 500L)
                CaptionSegment(
                    startMs = clipStartOffsetMs + s,
                    endMs = clipStartOffsetMs + e,
                    text = finalText,
                    originalText = original.ifEmpty { null }
                )
            }
            if (segments.isEmpty()) {
                AiTaskResult.Error("Tidak ada ucapan yang terdeteksi untuk diterjemahkan.")
            } else {
                AiTaskResult.Success(segments)
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Speech-to-translate timed out", e)
            AiTaskResult.Error("Deteksi & terjemahan suara melebihi batas waktu.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Speech-to-translate failed", e)
            AiTaskResult.Error(e.localizedMessage ?: "Gagal memproses suara ke penerjemah.")
        }
    }

    suspend fun translateCaptions(
        items: List<Pair<String, String>>,
        targetLanguage: String
    ): AiTaskResult<Map<String, String>> = withContext(Dispatchers.IO) {
        if (items.isEmpty()) {
            return@withContext AiTaskResult.Error("Belum ada teks subtitle untuk diterjemahkan.")
        }
        try {
            val formattedInput = items.joinToString("\n") { (id, text) ->
                "ID=$id | TEXT=$text"
            }
            val response = withTimeout(60_000L) {
                translateModel.generateContent(
                    "Translate each subtitle line below naturally into $targetLanguage while preserving tone and brevity:\n$formattedInput"
                )
            }
            val rawJson = response.text ?: return@withContext AiTaskResult.Error(
                "AI mengembalikan respons kosong saat menerjemahkan."
            )
            val root = JSONObject(rawJson)
            val arr = root.getJSONArray("translations")
            val map = mutableMapOf<String, String>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val id = obj.optString("id")
                val translated = obj.optString("translatedText").trim()
                if (id.isNotBlank() && translated.isNotBlank()) {
                    map[id] = translated
                }
            }
            AiTaskResult.Success(map)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Translation timed out", e)
            AiTaskResult.Error("Terjemahan melebihi batas waktu 60 detik.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Translation failed", e)
            AiTaskResult.Error(e.localizedMessage ?: "Gagal menerjemahkan teks.")
        }
    }

    companion object {
        private const val TAG = "KomikNarasiAiService"
    }
}

typealias VeloCutAiService = KomikNarasiAiService
