package com.example.media

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.example.model.TtsVoiceStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import java.util.UUID
import kotlin.coroutines.resume

data class TtsNarrationOutput(
    val uriString: String,
    val title: String,
    val durationMs: Long
)

object TtsNarrationHelper {

    suspend fun synthesizeNarrationToAudio(
        context: Context,
        scriptText: String,
        style: TtsVoiceStyle
    ): TtsNarrationOutput? = withContext(Dispatchers.IO) {
        val clean = scriptText.trim()
        if (clean.isEmpty()) return@withContext null

        val ttsDir = File(context.filesDir, "tts_narasi").apply { mkdirs() }
        val outFile = File(ttsDir, "kn_narasi_${System.currentTimeMillis()}.wav")
        val utteranceId = "kn_${UUID.randomUUID()}"

        val success = suspendCancellableCoroutine<Boolean> { cont ->
            var engine: TextToSpeech? = null
            engine = TextToSpeech(context.applicationContext) { status ->
                if (status != TextToSpeech.SUCCESS) {
                    engine?.shutdown()
                    if (cont.isActive) cont.resume(false)
                    return@TextToSpeech
                }
                val tts = engine ?: run {
                    if (cont.isActive) cont.resume(false)
                    return@TextToSpeech
                }
                val idLocale = Locale("id", "ID")
                val langRes = tts.setLanguage(idLocale)
                if (langRes == TextToSpeech.LANG_MISSING_DATA || langRes == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts.setLanguage(Locale.getDefault())
                }
                tts.setPitch(style.pitch)
                tts.setSpeechRate(style.rate)

                tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    override fun onDone(id: String?) {
                        tts.shutdown()
                        if (cont.isActive) cont.resume(outFile.exists() && outFile.length() > 44L)
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(id: String?) {
                        tts.shutdown()
                        if (cont.isActive) cont.resume(false)
                    }

                    override fun onError(id: String?, errorCode: Int) {
                        tts.shutdown()
                        if (cont.isActive) cont.resume(false)
                    }
                })

                val res = tts.synthesizeToFile(clean, Bundle(), outFile, utteranceId)
                if (res != TextToSpeech.SUCCESS) {
                    tts.shutdown()
                    if (cont.isActive) cont.resume(false)
                }
            }
            cont.invokeOnCancellation {
                try {
                    engine?.shutdown()
                } catch (_: Throwable) {
                }
            }
        }

        if (!success) {
            outFile.delete()
            return@withContext null
        }

        val uri = Uri.fromFile(outFile)
        val probed = MediaMetadataHelper.probeAudio(context, uri)
        TtsNarrationOutput(
            uriString = uri.toString(),
            title = "Narasi AI (${style.label})",
            durationMs = probed.durationMs.coerceAtLeast(1500L)
        )
    }
}
