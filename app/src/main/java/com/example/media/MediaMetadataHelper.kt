package com.example.media

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import android.util.LruCache
import com.example.model.VideoClip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ProbedAudio(
    val uriString: String,
    val title: String,
    val durationMs: Long
)

object MediaMetadataHelper {

    // Tuned cache size to prevent OOM on entry-level 2-4GB RAM devices
    private val frameCache = object : LruCache<String, Bitmap>(36) {}

    fun takePersistablePermissionIfPossible(context: Context, uri: Uri) {
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: SecurityException) {
            // Photo Picker URIs may not support persistable grants on all OS builds
        } catch (_: Throwable) {
        }
    }

    suspend fun probeMediaToClip(context: Context, uri: Uri): VideoClip = withContext(Dispatchers.IO) {
        takePersistablePermissionIfPossible(context, uri)
        val mime = context.contentResolver.getType(uri).orEmpty()
        val displayName = queryDisplayName(context, uri) ?: "Klip"
        val isImage = mime.startsWith("image/")

        if (isImage) {
            return@withContext VideoClip(
                uriString = uri.toString(),
                displayName = displayName,
                isImage = true,
                sourceDurationMs = 4000L,
                trimStartMs = 0L,
                trimEndMs = 4000L
            )
        }

        val durationMs = extractDurationMs(context, uri).coerceAtLeast(1000L)
        VideoClip(
            uriString = uri.toString(),
            displayName = displayName,
            isImage = false,
            sourceDurationMs = durationMs,
            trimStartMs = 0L,
            trimEndMs = durationMs
        )
    }

    suspend fun probeAudio(context: Context, uri: Uri): ProbedAudio = withContext(Dispatchers.IO) {
        takePersistablePermissionIfPossible(context, uri)
        val title = queryDisplayName(context, uri) ?: "Musik Latar"
        val durationMs = extractDurationMs(context, uri).coerceAtLeast(5000L)
        ProbedAudio(
            uriString = uri.toString(),
            title = title,
            durationMs = durationMs
        )
    }

    suspend fun extractClipThumbnails(
        context: Context,
        clip: VideoClip,
        count: Int
    ): List<Bitmap> = withContext(Dispatchers.IO) {
        val safeCount = count.coerceIn(1, 10)
        val uri = Uri.parse(clip.uriString)
        val result = mutableListOf<Bitmap>()

        if (clip.isImage) {
            val cacheKey = "${clip.uriString}_img"
            val cached = frameCache.get(cacheKey)
            val bmp = cached ?: decodeScaledImage(context, uri, 96, 96)?.also {
                frameCache.put(cacheKey, it)
            }
            if (bmp != null) {
                repeat(safeCount) { result.add(bmp) }
            }
            return@withContext result
        }

        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            val spanMs = (clip.trimEndMs - clip.trimStartMs).coerceAtLeast(300L)
            for (i in 0 until safeCount) {
                val timeMs = clip.trimStartMs + (spanMs * i) / safeCount.coerceAtLeast(1)
                val bucketMs = (timeMs / 250L) * 250L
                val cacheKey = "${clip.uriString}_$bucketMs"
                val cached = frameCache.get(cacheKey)
                if (cached != null) {
                    result.add(cached)
                } else {
                    val frame = retriever.getFrameAtTime(
                        timeMs * 1000L,
                        MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                    )
                    if (frame != null) {
                        val scaled = scaleCenterCrop(frame, 80, 80)
                        if (scaled != frame) frame.recycle()
                        frameCache.put(cacheKey, scaled)
                        result.add(scaled)
                    }
                }
            }
        } catch (_: Throwable) {
        } finally {
            try {
                retriever.release()
            } catch (_: Throwable) {
            }
        }
        result
    }

    private fun extractDurationMs(context: Context, uri: Uri): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            durationStr?.toLongOrNull() ?: 5000L
        } catch (_: Throwable) {
            5000L
        } finally {
            try {
                retriever.release()
            } catch (_: Throwable) {
            }
        }
    }

    private fun queryDisplayName(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) cursor.getString(idx) else null
                } else null
            }
        } catch (_: Throwable) {
            null
        }
    }

    private fun decodeScaledImage(
        context: Context,
        uri: Uri,
        reqW: Int,
        reqH: Int
    ): Bitmap? {
        return try {
            val opts = BitmapFactory.Options().apply {
                inSampleSize = 4
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val raw = BitmapFactory.decodeStream(stream, null, opts) ?: return null
                val scaled = scaleCenterCrop(raw, reqW, reqH)
                if (scaled != raw) raw.recycle()
                scaled
            }
        } catch (_: Throwable) {
            null
        }
    }

    private fun scaleCenterCrop(source: Bitmap, targetW: Int, targetH: Int): Bitmap {
        val srcW = source.width.coerceAtLeast(1)
        val srcH = source.height.coerceAtLeast(1)
        val scale = maxOf(targetW.toFloat() / srcW, targetH.toFloat() / srcH)
        val scaledW = (srcW * scale).toInt().coerceAtLeast(1)
        val scaledH = (srcH * scale).toInt().coerceAtLeast(1)
        val resized = Bitmap.createScaledBitmap(source, scaledW, scaledH, true)
        val x = ((scaledW - targetW) / 2).coerceAtLeast(0)
        val y = ((scaledH - targetH) / 2).coerceAtLeast(0)
        val cropped = Bitmap.createBitmap(
            resized,
            x,
            y,
            targetW.coerceAtMost(resized.width - x),
            targetH.coerceAtMost(resized.height - y)
        )
        if (cropped != resized) resized.recycle()
        return cropped
    }
}
