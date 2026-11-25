package com.example.media

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.ChannelMixingAudioProcessor
import androidx.media3.common.audio.ChannelMixingMatrix
import androidx.media3.common.audio.SonicAudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Presentation
import androidx.media3.transformer.Composition
import androidx.media3.transformer.DefaultEncoderFactory
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import androidx.media3.transformer.VideoEncoderSettings
import com.example.model.ExportBitrate
import com.example.model.ExportFrameRate
import com.example.model.ExportResolution
import com.example.model.ProjectTimelineData
import com.example.model.VocalSeparationMode
import com.example.model.VoiceEffectPreset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream

@UnstableApi
class VideoExportManager(private val context: Context) {

    private var activeTransformer: Transformer? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var progressRunnable: Runnable? = null

    fun startExport(
        projectTitle: String,
        timeline: ProjectTimelineData,
        resolution: ExportResolution,
        frameRate: ExportFrameRate,
        bitrate: ExportBitrate,
        onProgress: (Float) -> Unit,
        onSuccess: (File, Long) -> Unit,
        onError: (String) -> Unit
    ) {
        if (timeline.clips.isEmpty()) {
            onError("Tambahkan minimal 1 klip sebelum mengekspor.")
            return
        }

        val exportDir = File(context.filesDir, "exports").apply { mkdirs() }
        val safeName = projectTitle.replace(Regex("[^a-zA-Z0-9_-]"), "_").ifBlank { "VeloCut" }
        val outFile = File(exportDir, "${safeName}_${System.currentTimeMillis()}.mp4")

        val editedItems = timeline.clips.map { clip ->
            val mediaItemBuilder = MediaItem.Builder().setUri(Uri.parse(clip.uriString))
            if (clip.isImage) {
                mediaItemBuilder.setImageDurationMs(clip.effectiveDurationMs)
            } else {
                mediaItemBuilder.setClippingConfiguration(
                    MediaItem.ClippingConfiguration.Builder()
                        .setStartPositionMs(clip.trimStartMs)
                        .setEndPositionMs(clip.trimEndMs.coerceAtLeast(clip.trimStartMs + 300L))
                        .build()
                )
            }

            val videoEffects = mutableListOf<Effect>().apply {
                addAll(
                    Media3EffectsFactory.buildClipVideoEffects(
                        clip = clip,
                        filter = timeline.filterPreset,
                        videoEffect = timeline.videoEffect,
                        adjustments = timeline.adjustments
                    )
                )
                add(Presentation.createForHeight(resolution.heightPx))
            }

            val audioProcessors = mutableListOf<AudioProcessor>()
            if (!clip.isImage) {
                val targetSpeed = clip.effectiveSpeedAverage
                val pitch = clip.voiceEffect.pitchMultiplier
                if (targetSpeed != 1.0f || pitch != 1.0f) {
                    audioProcessors.add(
                        SonicAudioProcessor().apply {
                            setSpeed(targetSpeed)
                            setPitch(pitch)
                        }
                    )
                }
                if (clip.voiceEffect != VoiceEffectPreset.NONE ||
                    clip.vocalMode != VocalSeparationMode.ORIGINAL ||
                    clip.noiseReduction
                ) {
                    audioProcessors.add(
                        VoiceEffectsAudioProcessor(
                            voiceEffect = clip.voiceEffect,
                            vocalMode = clip.vocalMode,
                            noiseReduction = clip.noiseReduction
                        )
                    )
                }
                if (clip.volume < 0.99f && clip.volume > 0.01f) {
                    audioProcessors.add(
                        ChannelMixingAudioProcessor().apply {
                            putChannelMixingMatrix(
                                ChannelMixingMatrix.createForConstantGain(2, 2).scaleBy(clip.volume)
                            )
                        }
                    )
                }
            }

            val builder = EditedMediaItem.Builder(mediaItemBuilder.build())
                .setEffects(Effects(audioProcessors, videoEffects))
            if (clip.isImage) {
                builder.setFrameRate(frameRate.fps)
            } else if (clip.volume <= 0.01f) {
                builder.setRemoveAudio(true)
            }
            builder.build()
        }

        val videoSequence = EditedMediaItemSequence.withAudioAndVideoFrom(editedItems)
        val sequences = mutableListOf(videoSequence)

        timeline.audioTrack?.let { bgAudio ->
            val bgProcessors = mutableListOf<AudioProcessor>()
            if (bgAudio.voiceEffect.pitchMultiplier != 1.0f) {
                bgProcessors.add(
                    SonicAudioProcessor().apply {
                        setPitch(bgAudio.voiceEffect.pitchMultiplier)
                    }
                )
            }
            if (bgAudio.voiceEffect != VoiceEffectPreset.NONE ||
                bgAudio.vocalMode != VocalSeparationMode.ORIGINAL ||
                bgAudio.noiseReduction
            ) {
                bgProcessors.add(
                    VoiceEffectsAudioProcessor(
                        voiceEffect = bgAudio.voiceEffect,
                        vocalMode = bgAudio.vocalMode,
                        noiseReduction = bgAudio.noiseReduction
                    )
                )
            }
            bgProcessors.add(
                ChannelMixingAudioProcessor().apply {
                    putChannelMixingMatrix(
                        ChannelMixingMatrix.createForConstantGain(2, 2)
                            .scaleBy(bgAudio.volume.coerceIn(0.05f, 1f))
                    )
                }
            )
            val audioItem = EditedMediaItem.Builder(MediaItem.fromUri(Uri.parse(bgAudio.uriString)))
                .setEffects(Effects(bgProcessors, emptyList()))
                .build()
            val audioSequence = EditedMediaItemSequence.withAudioFrom(listOf(audioItem))
                .buildUpon()
                .setIsLooping(true)
                .build()
            sequences.add(audioSequence)
        }

        val compositionEffects = buildList {
            Media3EffectsFactory.buildTextOverlayEffect(timeline.textOverlays)?.let { add(it) }
        }

        val composition = Composition.Builder(sequences)
            .setEffects(Effects(emptyList(), compositionEffects))
            .build()

        val encoderFactory = DefaultEncoderFactory.Builder(context)
            .setRequestedVideoEncoderSettings(
                VideoEncoderSettings.Builder()
                    .setBitrate(bitrate.bps)
                    .build()
            )
            .build()

        val transformer = Transformer.Builder(context)
            .setVideoMimeType(MimeTypes.VIDEO_H264)
            .setEncoderFactory(encoderFactory)
            .addListener(object : Transformer.Listener {
                override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                    stopProgressPolling()
                    activeTransformer = null
                    onProgress(1f)
                    onSuccess(outFile, outFile.length())
                }

                override fun onError(
                    composition: Composition,
                    exportResult: ExportResult,
                    exportException: ExportException
                ) {
                    stopProgressPolling()
                    activeTransformer = null
                    outFile.delete()
                    onError(exportException.localizedMessage ?: "Ekspor video gagal diproses.")
                }
            })
            .build()

        activeTransformer = transformer
        transformer.start(composition, outFile.absolutePath)
        startProgressPolling(transformer, onProgress)
    }

    fun cancelExport() {
        stopProgressPolling()
        try {
            activeTransformer?.cancel()
        } catch (_: Throwable) {
        }
        activeTransformer = null
    }

    private fun startProgressPolling(transformer: Transformer, onProgress: (Float) -> Unit) {
        stopProgressPolling()
        val holder = ProgressHolder()
        val runnable = object : Runnable {
            override fun run() {
                val t = activeTransformer ?: return
                val state = t.getProgress(holder)
                if (state == Transformer.PROGRESS_STATE_AVAILABLE) {
                    onProgress((holder.progress / 100f).coerceIn(0f, 0.99f))
                }
                if (state != Transformer.PROGRESS_STATE_NOT_STARTED) {
                    mainHandler.postDelayed(this, 200L)
                }
            }
        }
        progressRunnable = runnable
        mainHandler.postDelayed(runnable, 200L)
    }

    private fun stopProgressPolling() {
        progressRunnable?.let { mainHandler.removeCallbacks(it) }
        progressRunnable = null
    }

    companion object {
        suspend fun saveToGalleryMediaStore(
            context: Context,
            sourceFile: File,
            title: String
        ): Uri? = withContext(Dispatchers.IO) {
            if (!sourceFile.exists()) return@withContext null
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, "$title.mp4")
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(
                        MediaStore.Video.Media.RELATIVE_PATH,
                        "${Environment.DIRECTORY_MOVIES}/VeloCut"
                    )
                    put(MediaStore.Video.Media.IS_PENDING, 1)
                }
            }

            val resolver = context.contentResolver
            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            }

            val uri = resolver.insert(collection, values) ?: return@withContext null
            try {
                resolver.openOutputStream(uri)?.use { out ->
                    FileInputStream(sourceFile).use { input ->
                        input.copyTo(out)
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    values.clear()
                    values.put(MediaStore.Video.Media.IS_PENDING, 0)
                    resolver.update(uri, values, null, null)
                }
                uri
            } catch (_: Throwable) {
                resolver.delete(uri, null, null)
                null
            }
        }
    }
}
