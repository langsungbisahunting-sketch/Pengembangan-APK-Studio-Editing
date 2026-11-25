package com.example.media

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer

object AudioExtractionHelper {

    suspend fun extractAudioToPersistentM4a(
        context: Context,
        sourceUri: Uri,
        baseTitle: String
    ): File? = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, "extracted_audio").apply { mkdirs() }
        val safe = baseTitle.replace(Regex("[^a-zA-Z0-9_-]"), "_").ifBlank { "audio" }
        val outFile = File(dir, "${safe}_${System.currentTimeMillis()}.m4a")
        if (remuxAudioTrack(context, sourceUri, outFile)) {
            outFile
        } else {
            outFile.delete()
            null
        }
    }

    suspend fun extractAudioToTempM4a(
        context: Context,
        sourceUri: Uri
    ): File? = withContext(Dispatchers.IO) {
        val tempFile = File(context.cacheDir, "ai_audio_${System.currentTimeMillis()}.m4a")
        if (remuxAudioTrack(context, sourceUri, tempFile)) {
            tempFile
        } else {
            tempFile.delete()
            null
        }
    }

    private fun remuxAudioTrack(context: Context, uri: Uri, outputFile: File): Boolean {
        val extractor = MediaExtractor()
        var muxer: MediaMuxer? = null
        return try {
            extractor.setDataSource(context, uri, null)
            var audioTrackIndex = -1
            var audioFormat: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME).orEmpty()
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    audioFormat = format
                    break
                }
            }
            if (audioTrackIndex < 0 || audioFormat == null) return false

            extractor.selectTrack(audioTrackIndex)
            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val muxerTrackIndex = muxer.addTrack(audioFormat)
            muxer.start()

            val maxBufSize = if (audioFormat.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                audioFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE).coerceAtLeast(64 * 1024)
            } else {
                256 * 1024
            }
            val buffer = ByteBuffer.allocate(maxBufSize)
            val bufferInfo = MediaCodec.BufferInfo()

            while (true) {
                bufferInfo.offset = 0
                bufferInfo.size = extractor.readSampleData(buffer, 0)
                if (bufferInfo.size < 0) {
                    bufferInfo.size = 0
                    break
                }
                bufferInfo.presentationTimeUs = extractor.sampleTime
                bufferInfo.flags = extractor.sampleFlags
                muxer.writeSampleData(muxerTrackIndex, buffer, bufferInfo)
                extractor.advance()
            }
            true
        } catch (_: Throwable) {
            false
        } finally {
            try {
                muxer?.stop()
                muxer?.release()
            } catch (_: Throwable) {
            }
            try {
                extractor.release()
            } catch (_: Throwable) {
            }
        }
    }
}
