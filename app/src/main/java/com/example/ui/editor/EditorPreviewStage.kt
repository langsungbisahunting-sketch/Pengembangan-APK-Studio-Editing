package com.example.ui.editor

import android.content.Context
import android.net.Uri
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.media.Media3EffectsFactory
import com.example.media.VoiceEffectsAudioProcessor
import com.example.model.AudioTrackItem
import com.example.model.BlendModePreset
import com.example.model.CanvasFitMode
import com.example.model.PanZoomPreset
import com.example.model.ProjectTimelineData
import com.example.model.SpeedCurvePreset
import com.example.model.TextAnimationType
import com.example.model.TextFontStyle
import com.example.model.VideoEffectPreset
import com.example.model.interpolateKeyframes
import com.example.ui.common.buildComposeColorMatrix
import com.example.ui.common.formatTimecodeMs
import com.example.ui.theme.EnergyOrange
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.OutfitFontFamily
import com.example.ui.theme.PlusJakartaFontFamily
import com.example.ui.theme.PureWhite
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCard
import com.example.ui.theme.StudioObsidian
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin

@OptIn(UnstableApi::class)
@Composable
fun EditorPreviewStage(
    timeline: ProjectTimelineData,
    currentPositionMs: Long,
    isPlaying: Boolean,
    isPreviewMuted: Boolean,
    selectedTextId: String?,
    selectedPipId: String?,
    isFullscreen: Boolean,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onPlaybackTick: (Long) -> Unit,
    onPlaybackEnded: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleFullscreen: () -> Unit,
    onTransformClip: (Float, Float, Float, Float) -> Unit,
    onSelectText: (String?) -> Unit,
    onTransformText: (String, Float, Float, Float, Float) -> Unit,
    onSelectPip: (String?) -> Unit,
    onTransformPip: (String, Float, Float, Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val latestOnTick by rememberUpdatedState(onPlaybackTick)
    val latestOnEnded by rememberUpdatedState(onPlaybackEnded)
    val latestTimeline by rememberUpdatedState(timeline)
    val latestIsPlaying by rememberUpdatedState(isPlaying)
    val latestPosition by rememberUpdatedState(currentPositionMs)

    val videoAudioProcessor = remember { VoiceEffectsAudioProcessor() }
    val vocalAudioProcessor = remember { VoiceEffectsAudioProcessor() }
    val musicAudioProcessor = remember { VoiceEffectsAudioProcessor() }

    val videoPlayer = remember {
        val rf = object : DefaultRenderersFactory(context) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean
            ): AudioSink {
                return DefaultAudioSink.Builder(context)
                    .setEnableFloatOutput(enableFloatOutput)
                    .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                    .setAudioProcessors(arrayOf(videoAudioProcessor))
                    .build()
            }
        }
        ExoPlayer.Builder(context, rf).build().apply {
            repeatMode = Player.REPEAT_MODE_OFF
        }
    }

    val vocalPlayer = remember {
        val rf = object : DefaultRenderersFactory(context) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean
            ): AudioSink {
                return DefaultAudioSink.Builder(context)
                    .setEnableFloatOutput(enableFloatOutput)
                    .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                    .setAudioProcessors(arrayOf(vocalAudioProcessor))
                    .build()
            }
        }
        ExoPlayer.Builder(context, rf).build().apply {
            repeatMode = Player.REPEAT_MODE_OFF
        }
    }

    val musicPlayer = remember {
        val rf = object : DefaultRenderersFactory(context) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean
            ): AudioSink {
                return DefaultAudioSink.Builder(context)
                    .setEnableFloatOutput(enableFloatOutput)
                    .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                    .setAudioProcessors(arrayOf(musicAudioProcessor))
                    .build()
            }
        }
        ExoPlayer.Builder(context, rf).build().apply {
            repeatMode = Player.REPEAT_MODE_OFF
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            videoPlayer.release()
            vocalPlayer.release()
            musicPlayer.release()
        }
    }

    val activeSlot = remember(timeline, currentPositionMs) {
        timeline.findClipAt(currentPositionMs)
    }
    val activeClipIndex = activeSlot?.first ?: -1
    val activeClip = timeline.clips.getOrNull(activeClipIndex)
    val activeOffsetEffectiveMs = activeSlot?.second ?: 0L
    val effectiveMusicTrack = timeline.musicTrack ?: timeline.audioTrack

    var preparedClipSignature by remember { mutableIntStateOf(0) }

    LaunchedEffect(
        activeClip?.id,
        activeClip?.uriString,
        activeClip?.trimStartMs,
        activeClip?.trimEndMs,
        activeClip?.rotationDegrees,
        activeClip?.isMirrored,
        activeClip?.cropScale,
        activeClip?.keyframes,
        timeline.filterPreset,
        timeline.videoEffect,
        timeline.adjustments
    ) {
        if (activeClip == null || activeClip.isImage) {
            videoPlayer.pause()
            return@LaunchedEffect
        }
        val mediaItem = MediaItem.Builder()
            .setUri(Uri.parse(activeClip.uriString))
            .setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(activeClip.trimStartMs)
                    .setEndPositionMs(activeClip.trimEndMs.coerceAtLeast(activeClip.trimStartMs + 300L))
                    .build()
            )
            .build()
        // Keep touch transform & PanZoom in Compose graphicsLayer for zero-flicker 60fps interaction
        val previewClip = activeClip.copy(
            userScale = 1f,
            posX = 0f,
            posY = 0f,
            freeRotationDeg = 0f,
            panZoom = PanZoomPreset.NONE
        )
        val effects = Media3EffectsFactory.buildClipVideoEffects(
            clip = previewClip,
            filter = timeline.filterPreset,
            videoEffect = timeline.videoEffect,
            adjustments = timeline.adjustments
        )
        videoPlayer.setVideoEffects(effects)
        videoPlayer.setMediaItem(mediaItem)
        videoPlayer.prepare()
        val targetPlayerPos = (activeOffsetEffectiveMs * activeClip.effectiveSpeedAverage).toLong()
            .coerceIn(0L, activeClip.trimmedSourceDurationMs)
        videoPlayer.seekTo(targetPlayerPos)
        preparedClipSignature++
    }

    LaunchedEffect(
        activeClip?.speed,
        activeClip?.speedCurve,
        activeClip?.volume,
        activeClip?.voiceEffect,
        activeClip?.vocalMode,
        activeClip?.noiseReduction,
        activeOffsetEffectiveMs,
        isPreviewMuted,
        preparedClipSignature
    ) {
        if (activeClip != null && !activeClip.isImage) {
            val progress = (activeOffsetEffectiveMs.toFloat() / activeClip.effectiveDurationMs.coerceAtLeast(1L))
                .coerceIn(0f, 1f)
            val liveSpeed = if (activeClip.speedCurve == SpeedCurvePreset.NORMAL) {
                activeClip.speed.coerceIn(0.1f, 8.0f)
            } else {
                activeClip.speedCurve.speedMultiplierAt(progress)
            }
            videoAudioProcessor.voiceEffect = activeClip.voiceEffect
            videoAudioProcessor.vocalMode = activeClip.vocalMode
            videoAudioProcessor.noiseReduction = activeClip.noiseReduction
            videoPlayer.playbackParameters = PlaybackParameters(
                liveSpeed,
                activeClip.voiceEffect.pitchMultiplier
            )
            videoPlayer.volume = if (isPreviewMuted) 0f else activeClip.volume.coerceIn(0f, 1f)
        }
    }

    // Prepare Track 1: Vocal / Narration
    LaunchedEffect(
        timeline.vocalTrack?.uriString,
        timeline.vocalTrack?.trimStartMs,
        timeline.vocalTrack?.trimEndMs
    ) {
        val vocal = timeline.vocalTrack
        if (vocal == null) {
            vocalPlayer.stop()
            vocalPlayer.clearMediaItems()
        } else {
            val item = MediaItem.Builder()
                .setUri(Uri.parse(vocal.uriString))
                .setClippingConfiguration(
                    MediaItem.ClippingConfiguration.Builder()
                        .setStartPositionMs(vocal.trimStartMs)
                        .setEndPositionMs(vocal.trimEndMs.coerceAtLeast(vocal.trimStartMs + 400L))
                        .build()
                )
                .build()
            vocalPlayer.setMediaItem(item)
            vocalPlayer.prepare()
        }
    }

    // Prepare Track 2: Music / BGM
    LaunchedEffect(
        effectiveMusicTrack?.uriString,
        effectiveMusicTrack?.trimStartMs,
        effectiveMusicTrack?.trimEndMs
    ) {
        val music = effectiveMusicTrack
        if (music == null) {
            musicPlayer.stop()
            musicPlayer.clearMediaItems()
        } else {
            val item = MediaItem.Builder()
                .setUri(Uri.parse(music.uriString))
                .setClippingConfiguration(
                    MediaItem.ClippingConfiguration.Builder()
                        .setStartPositionMs(music.trimStartMs)
                        .setEndPositionMs(music.trimEndMs.coerceAtLeast(music.trimStartMs + 400L))
                        .build()
                )
                .build()
            musicPlayer.setMediaItem(item)
            musicPlayer.prepare()
        }
    }

    fun isTrackActiveAt(track: AudioTrackItem?, posMs: Long): Boolean {
        if (track == null) return false
        return posMs in track.startOffsetMs until (track.startOffsetMs + track.effectiveDurationMs)
    }

    // Sync Vocal & Music DSP, Fade & Ducking Volume
    LaunchedEffect(
        timeline.vocalTrack,
        effectiveMusicTrack,
        currentPositionMs,
        isPreviewMuted
    ) {
        val vocal = timeline.vocalTrack
        val vocalActive = isTrackActiveAt(vocal, currentPositionMs)
        if (vocal != null) {
            vocalAudioProcessor.voiceEffect = vocal.voiceEffect
            vocalAudioProcessor.vocalMode = vocal.vocalMode
            vocalAudioProcessor.noiseReduction = vocal.noiseReduction
            vocalPlayer.playbackParameters = PlaybackParameters(1.0f, vocal.voiceEffect.pitchMultiplier)
            var v = if (isPreviewMuted || !vocalActive) 0f else vocal.volume.coerceIn(0f, 1f)
            val localPos = (currentPositionMs - vocal.startOffsetMs).coerceAtLeast(0L)
            if (vocal.fadeIn && localPos < 600L) v *= (localPos / 600f).coerceIn(0.1f, 1f)
            if (vocal.fadeOut && vocal.effectiveDurationMs - localPos < 600L) {
                v *= ((vocal.effectiveDurationMs - localPos) / 600f).coerceIn(0.1f, 1f)
            }
            vocalPlayer.volume = v
        } else {
            vocalPlayer.volume = 0f
        }

        val music = effectiveMusicTrack
        val musicActive = isTrackActiveAt(music, currentPositionMs)
        if (music != null) {
            musicAudioProcessor.voiceEffect = music.voiceEffect
            musicAudioProcessor.vocalMode = music.vocalMode
            musicAudioProcessor.noiseReduction = music.noiseReduction
            musicPlayer.playbackParameters = PlaybackParameters(1.0f, music.voiceEffect.pitchMultiplier)
            val duckFactor = if (vocalActive && music.autoDucking) 0.42f else 1.0f
            var m = if (isPreviewMuted || !musicActive) 0f else (music.volume * duckFactor).coerceIn(0f, 1f)
            val localPos = (currentPositionMs - music.startOffsetMs).coerceAtLeast(0L)
            if (music.fadeIn && localPos < 700L) m *= (localPos / 700f).coerceIn(0.1f, 1f)
            if (music.fadeOut && music.effectiveDurationMs - localPos < 700L) {
                m *= ((music.effectiveDurationMs - localPos) / 700f).coerceIn(0.1f, 1f)
            }
            musicPlayer.volume = m
        } else {
            musicPlayer.volume = 0f
        }
    }

    LaunchedEffect(currentPositionMs, isPlaying) {
        if (!isPlaying && activeClip != null && !activeClip.isImage) {
            val targetPlayerPos = (activeOffsetEffectiveMs * activeClip.effectiveSpeedAverage).toLong()
                .coerceIn(0L, activeClip.trimmedSourceDurationMs)
            if (abs(videoPlayer.currentPosition - targetPlayerPos) > 90L) {
                videoPlayer.seekTo(targetPlayerPos)
            }
        }
        val vocal = timeline.vocalTrack
        if (vocal != null) {
            if (isTrackActiveAt(vocal, currentPositionMs)) {
                val localPos = (currentPositionMs - vocal.startOffsetMs).coerceIn(0L, vocal.effectiveDurationMs)
                if (!isPlaying || abs(vocalPlayer.currentPosition - localPos) > 250L) {
                    vocalPlayer.seekTo(localPos)
                }
                if (isPlaying && !vocalPlayer.isPlaying) vocalPlayer.play()
            } else if (vocalPlayer.isPlaying) {
                vocalPlayer.pause()
            }
        }
        val music = effectiveMusicTrack
        if (music != null) {
            if (isTrackActiveAt(music, currentPositionMs)) {
                val localPos = (currentPositionMs - music.startOffsetMs).coerceIn(0L, music.effectiveDurationMs)
                if (!isPlaying || abs(musicPlayer.currentPosition - localPos) > 250L) {
                    musicPlayer.seekTo(localPos)
                }
                if (isPlaying && !musicPlayer.isPlaying) musicPlayer.play()
            } else if (musicPlayer.isPlaying) {
                musicPlayer.pause()
            }
        }
    }

    LaunchedEffect(isPlaying, activeClipIndex, preparedClipSignature) {
        if (isPlaying && activeClip != null) {
            if (activeClip.isImage) {
                videoPlayer.pause()
            } else {
                videoPlayer.play()
            }
            latestTimeline.vocalTrack?.let { vocal ->
                if (isTrackActiveAt(vocal, latestPosition)) {
                    vocalPlayer.seekTo((latestPosition - vocal.startOffsetMs).coerceIn(0L, vocal.effectiveDurationMs))
                    vocalPlayer.play()
                }
            }
            (latestTimeline.musicTrack ?: latestTimeline.audioTrack)?.let { music ->
                if (isTrackActiveAt(music, latestPosition)) {
                    musicPlayer.seekTo((latestPosition - music.startOffsetMs).coerceIn(0L, music.effectiveDurationMs))
                    musicPlayer.play()
                }
            }

            var lastFrameTime = System.currentTimeMillis()
            while (latestIsPlaying) {
                delay(33L)
                val now = System.currentTimeMillis()
                val delta = (now - lastFrameTime).coerceIn(1L, 120L)
                lastFrameTime = now

                val tl = latestTimeline
                val totalDur = tl.totalDurationMs
                val currentSlot = tl.findClipAt(latestPosition)
                val currentClip = currentSlot?.let { tl.clips.getOrNull(it.first) }

                val nextPos = if (currentClip != null && !currentClip.isImage && videoPlayer.isPlaying) {
                    val clipStart = tl.clipStartPositionMs(currentSlot.first)
                    val playerEffective = (videoPlayer.currentPosition / currentClip.effectiveSpeedAverage).toLong()
                    (clipStart + playerEffective).coerceAtLeast(latestPosition)
                } else {
                    latestPosition + delta
                }

                if (nextPos >= totalDur) {
                    latestOnTick(totalDur)
                    latestOnEnded()
                    break
                } else {
                    latestOnTick(nextPos)
                }
            }
        } else {
            videoPlayer.pause()
            vocalPlayer.pause()
            musicPlayer.pause()
        }
    }

    val pulseTransition = rememberInfiniteTransition(label = "stage_fx")
    val fxPhase by pulseTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.283f,
        animationSpec = infiniteRepeatable(tween(1800), RepeatMode.Restart),
        label = "fx_phase"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(StudioObsidian),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            if (timeline.clips.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = StudioCard,
                    border = BorderStroke(1.dp, StudioBorder),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.Movie, null, tint = TextSecondary, modifier = Modifier.size(36.dp))
                        Text("Belum ada klip di timeline", color = TextSecondary)
                    }
                }
            } else {
                val ratio = timeline.aspectRatio.widthRatio / timeline.aspectRatio.heightRatio
                val clipKeyframe = remember(activeClip?.keyframes, activeOffsetEffectiveMs) {
                    activeClip?.let { interpolateKeyframes(it.keyframes, activeOffsetEffectiveMs) }
                }

                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxHeight()
                        .aspectRatio(ratio, matchHeightConstraintsFirst = ratio < 1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (activeClip?.chromaKey?.enabled == true || activeClip?.chromaKey?.autoRemoveBg == true) {
                                Color(0xFF16161A)
                            } else {
                                Color(timeline.canvasBgHex)
                            }
                        )
                        .border(1.dp, StudioBorder, RoundedCornerShape(12.dp))
                ) {
                    val stageWidthPx = with(LocalDensity.current) { maxWidth.toPx() }.coerceAtLeast(1f)
                    val stageHeightPx = with(LocalDensity.current) { maxHeight.toPx() }.coerceAtLeast(1f)

                    // Optional Custom Canvas Image or Blur Background Fill
                    if (timeline.canvasFitMode == CanvasFitMode.CUSTOM_IMAGE && !timeline.canvasBgImageUri.isNullOrBlank()) {
                        AsyncImage(
                            model = Uri.parse(timeline.canvasBgImageUri),
                            contentDescription = "Latar kanvas kustom",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else if (timeline.canvasFitMode == CanvasFitMode.BLUR_BG && activeClip != null) {
                        AsyncImage(
                            model = Uri.parse(activeClip.uriString),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .blur(20.dp)
                                .alpha(0.45f)
                        )
                    }

                    // Live Pan & Zoom calculation
                    val clipProgress = if (activeClip != null) {
                        (activeOffsetEffectiveMs.toFloat() / activeClip.effectiveDurationMs.coerceAtLeast(1L)).coerceIn(0f, 1f)
                    } else 0f
                    val pzScale = when (activeClip?.panZoom) {
                        PanZoomPreset.ZOOM_IN -> 1f + 0.24f * clipProgress
                        PanZoomPreset.ZOOM_OUT -> 1.24f - 0.24f * clipProgress
                        PanZoomPreset.PAN_LEFT, PanZoomPreset.PAN_RIGHT -> 1.18f
                        else -> 1f
                    }
                    val pzOffsetX = when (activeClip?.panZoom) {
                        PanZoomPreset.PAN_LEFT -> (0.08f - 0.16f * clipProgress) * stageWidthPx
                        PanZoomPreset.PAN_RIGHT -> (-0.08f + 0.16f * clipProgress) * stageWidthPx
                        else -> 0f
                    }

                    val stageTouchModifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures {
                                onSelectText(null)
                                onSelectPip(null)
                            }
                        }
                        .pointerInput(activeClip?.id, stageWidthPx, stageHeightPx) {
                            detectTransformGestures { _, pan, zoom, rotation ->
                                onTransformClip(
                                    pan.x / stageWidthPx,
                                    pan.y / stageHeightPx,
                                    zoom,
                                    rotation
                                )
                            }
                        }

                    val mediaModifier = stageTouchModifier
                        .then(
                            if (timeline.videoEffect == VideoEffectPreset.BLUR) Modifier.blur(6.dp) else Modifier
                        )
                        .graphicsLayer {
                            if (activeClip != null) {
                                val userScale = activeClip.userScale.coerceIn(0.4f, 3.2f)
                                if (activeClip.isImage) {
                                    val kfScale = clipKeyframe?.scale ?: 1f
                                    val crop = activeClip.cropScale.coerceIn(1f, 2.5f)
                                    scaleX = kfScale * crop * userScale * pzScale * (if (activeClip.isMirrored) -1f else 1f)
                                    scaleY = kfScale * crop * userScale * pzScale
                                    rotationZ = activeClip.rotationDegrees.toFloat() + activeClip.freeRotationDeg + (clipKeyframe?.rotationDeg ?: 0f)
                                    translationX = (activeClip.posX + (clipKeyframe?.xOffset ?: 0f)) * stageWidthPx + pzOffsetX
                                    translationY = (activeClip.posY + (clipKeyframe?.yOffset ?: 0f)) * stageHeightPx
                                    alpha = clipKeyframe?.opacity ?: 1f
                                } else {
                                    scaleX = userScale * pzScale
                                    scaleY = userScale * pzScale
                                    rotationZ = activeClip.freeRotationDeg
                                    translationX = activeClip.posX * stageWidthPx + pzOffsetX
                                    translationY = activeClip.posY * stageHeightPx
                                    alpha = clipKeyframe?.opacity ?: 1f
                                }
                            }
                        }

                    if (activeClip?.isImage == true) {
                        val colorMatrix = remember(timeline.filterPreset, timeline.adjustments) {
                            buildComposeColorMatrix(timeline.filterPreset, timeline.adjustments)
                        }
                        AsyncImage(
                            model = Uri.parse(activeClip.uriString),
                            contentDescription = activeClip.displayName,
                            contentScale = ContentScale.Fit,
                            colorFilter = ColorFilter.colorMatrix(colorMatrix),
                            modifier = mediaModifier
                        )
                    } else {
                        AndroidView(
                            factory = { ctx ->
                                PlayerView(ctx).apply {
                                    useController = false
                                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                                    setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                                    layoutParams = ViewGroup.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                    player = videoPlayer
                                }
                            },
                            update = { view ->
                                if (view.player !== videoPlayer) {
                                    view.player = videoPlayer
                                }
                            },
                            modifier = mediaModifier
                        )
                    }

                    // Sparkle / Vignette / Chroma Badge Overlays
                    if (timeline.videoEffect == VideoEffectPreset.SPARKLE) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            for (i in 0..8) {
                                val px = ((i * 73 + 29) % 100) / 100f * size.width
                                val py = ((i * 41 + 17) % 100) / 100f * size.height
                                val r = (3f + 4f * abs(sin(fxPhase + i))).dp.toPx()
                                drawCircle(
                                    color = GoldAccent.copy(alpha = 0.72f),
                                    radius = r,
                                    center = Offset(px, py)
                                )
                            }
                        }
                    }

                    if (timeline.adjustments.vignette > 0.02f) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color.Black.copy(alpha = timeline.adjustments.vignette.coerceIn(0f, 0.85f))
                                        )
                                    )
                                )
                        )
                    }

                    if (activeClip?.chromaKey?.enabled == true || activeClip?.chromaKey?.autoRemoveBg == true) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color.Black.copy(alpha = 0.72f),
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(8.dp)
                        ) {
                            Text(
                                text = if (activeClip.chromaKey.autoRemoveBg) "Auto BG Aktif" else "Chroma Key Aktif",
                                style = MaterialTheme.typography.labelSmall,
                                color = EnergyOrange,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }

                    // Active PiP Overlays with Direct Drag, Pinch-Zoom & Rotate
                    val visiblePips = timeline.pipOverlays.filter {
                        currentPositionMs in it.startMs..it.endMs
                    }
                    visiblePips.forEach { pip ->
                        val pipSelected = pip.id == selectedPipId
                        val pipWidth = maxWidth * pip.scale.coerceIn(0.16f, 0.95f)
                        Box(
                            modifier = Modifier
                                .offset {
                                    IntOffset(
                                        x = ((pip.xFraction * stageWidthPx) - (stageWidthPx * pip.scale * 0.5f)).roundToInt(),
                                        y = ((pip.yFraction * stageHeightPx) - (stageWidthPx * pip.scale * 0.5f)).roundToInt()
                                    )
                                }
                                .size(pipWidth)
                                .graphicsLayer {
                                    rotationZ = pip.rotationDeg
                                }
                                .clip(RoundedCornerShape(8.dp))
                                .alpha(pip.opacity)
                                .then(
                                    if (pipSelected) Modifier.border(2.dp, EnergyOrange, RoundedCornerShape(8.dp))
                                    else Modifier.border(1.dp, PureWhite.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                )
                                .pointerInput(pip.id) {
                                    detectTapGestures { onSelectPip(pip.id) }
                                }
                                .pointerInput(pip.id, stageWidthPx, stageHeightPx) {
                                    detectTransformGestures { _, pan, zoom, rotation ->
                                        onTransformPip(
                                            pip.id,
                                            pan.x / stageWidthPx,
                                            pan.y / stageHeightPx,
                                            zoom,
                                            rotation
                                        )
                                    }
                                }
                        ) {
                            AsyncImage(
                                model = Uri.parse(pip.uriString),
                                contentDescription = pip.displayName,
                                contentScale = ContentScale.Crop,
                                colorFilter = when (pip.blendMode) {
                                    BlendModePreset.SCREEN -> ColorFilter.tint(Color.White.copy(alpha = 0.2f), BlendMode.Screen)
                                    BlendModePreset.MULTIPLY -> ColorFilter.tint(Color.Gray.copy(alpha = 0.3f), BlendMode.Multiply)
                                    BlendModePreset.OVERLAY -> ColorFilter.tint(EnergyOrange.copy(alpha = 0.16f), BlendMode.Overlay)
                                    BlendModePreset.LIGHTEN -> ColorFilter.tint(Color.White.copy(alpha = 0.22f), BlendMode.Lighten)
                                    BlendModePreset.DARKEN -> ColorFilter.tint(Color.Black.copy(alpha = 0.22f), BlendMode.Darken)
                                    BlendModePreset.NORMAL -> null
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                            if (pip.chromaEnabled) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color.Black.copy(alpha = 0.65f),
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(3.dp)
                                ) {
                                    Text(
                                        text = "CK",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                                        color = EnergyOrange,
                                        modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Active Text & Comic Sticker Overlays with Direct Drag, Pinch-Zoom & Rotate
                    val visibleTexts = timeline.textOverlays.filter {
                        currentPositionMs in it.startMs..it.endMs
                    }
                    visibleTexts.forEach { item ->
                        val isSelected = item.id == selectedTextId
                        val elapsed = (currentPositionMs - item.startMs).coerceAtLeast(0L)
                        val remaining = (item.endMs - currentPositionMs).coerceAtLeast(0L)
                        val animAlpha = when (item.animation) {
                            TextAnimationType.FADE -> (elapsed / 400f).coerceIn(0f, 1f)
                            TextAnimationType.FADE_OUT -> (remaining / 400f).coerceIn(0f, 1f)
                            else -> 1f
                        }
                        val animScale = when (item.animation) {
                            TextAnimationType.POP -> (0.7f + 0.3f * (elapsed / 300f).coerceIn(0f, 1f))
                            TextAnimationType.PULSE_LOOP -> 0.94f + 0.08f * sin(fxPhase)
                            else -> 1f
                        }
                        val animOffsetY = when (item.animation) {
                            TextAnimationType.SLIDE_UP -> (1f - (elapsed / 350f).coerceIn(0f, 1f)) * 28f
                            else -> 0f
                        }
                        val fontFamily = when (item.fontStyle) {
                            TextFontStyle.MODERN -> OutfitFontFamily
                            TextFontStyle.CLEAN -> PlusJakartaFontFamily
                            TextFontStyle.MONO -> JetBrainsMonoFontFamily
                        }

                        val shape = if (item.isSticker) {
                            RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 2.dp)
                        } else {
                            RoundedCornerShape(8.dp)
                        }

                        Box(
                            modifier = Modifier
                                .offset {
                                    IntOffset(
                                        x = ((item.xFraction * stageWidthPx) - 70f).roundToInt(),
                                        y = ((item.yFraction * stageHeightPx) - 24f + animOffsetY).roundToInt()
                                    )
                                }
                                .graphicsLayer {
                                    alpha = animAlpha
                                    scaleX = animScale * item.scale
                                    scaleY = animScale * item.scale
                                    rotationZ = item.curveDegrees + item.rotationDeg
                                }
                                .clip(shape)
                                .background(Color(item.bgColorHex))
                                .then(
                                    when {
                                        isSelected -> Modifier.border(1.5.dp, EnergyOrange, shape)
                                        item.isSticker || item.strokeEnabled -> Modifier.border(
                                            width = 1.dp,
                                            color = if (item.isSticker) GoldAccent else Color(item.strokeColorHex).copy(alpha = 0.55f),
                                            shape = shape
                                        )
                                        else -> Modifier
                                    }
                                )
                                .pointerInput(item.id) {
                                    detectTapGestures { onSelectText(item.id) }
                                }
                                .pointerInput(item.id, stageWidthPx, stageHeightPx) {
                                    detectTransformGestures { _, pan, zoom, rotation ->
                                        onTransformText(
                                            item.id,
                                            pan.x / stageWidthPx,
                                            pan.y / stageHeightPx,
                                            zoom,
                                            rotation
                                        )
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            val textBrush = if (item.useGradient) {
                                Brush.horizontalGradient(listOf(PureWhite, EnergyOrange, GoldAccent))
                            } else null
                            val textStyle = if (textBrush != null) {
                                TextStyle(
                                    brush = textBrush,
                                    fontFamily = fontFamily,
                                    fontSize = (item.fontSizeSp * 0.72f).sp,
                                    fontWeight = FontWeight.Bold,
                                    shadow = if (item.hasShadow) {
                                        Shadow(Color.Black.copy(alpha = 0.85f), Offset(2f, 2f), 4f)
                                    } else null
                                )
                            } else {
                                TextStyle(
                                    color = Color(item.colorHex),
                                    fontFamily = fontFamily,
                                    fontSize = (item.fontSizeSp * 0.72f).sp,
                                    fontWeight = FontWeight.Bold,
                                    shadow = if (item.hasShadow) {
                                        Shadow(Color.Black.copy(alpha = 0.85f), Offset(2f, 2f), 4f)
                                    } else null
                                )
                            }
                            Text(
                                text = item.text,
                                style = textStyle
                            )
                        }
                    }

                    // Live Watermark Preview when enabled
                    if (timeline.includeWatermark) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color.Black.copy(alpha = 0.55f),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(8.dp)
                        ) {
                            Text(
                                text = "Dibuat dengan Komik Narasi",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = PureWhite.copy(alpha = 0.85f),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        // Transport Controls Strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = formatTimecodeMs(currentPositionMs),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                    color = TextPrimary
                )
                Text(
                    text = " / ${formatTimecodeMs(timeline.totalDurationMs)}",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                    color = TextSecondary
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = { onSeekTo(0L) },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.SkipPrevious,
                        contentDescription = "Ke Awal",
                        tint = TextSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Surface(
                    onClick = onTogglePlayPause,
                    shape = CircleShape,
                    color = EnergyOrange,
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("editor_play_pause_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (isPlaying) "Jeda" else "Putar",
                            tint = PureWhite,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onToggleMute,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("editor_mute_button")
                ) {
                    Icon(
                        imageVector = if (isPreviewMuted) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
                        contentDescription = if (isPreviewMuted) "Suara Aktif" else "Bisukan Preview",
                        tint = if (isPreviewMuted) TextSecondary else EnergyOrange,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            IconButton(
                onClick = onToggleFullscreen,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("editor_fullscreen_button")
            ) {
                Icon(
                    imageVector = if (isFullscreen) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                    contentDescription = "Layar Penuh",
                    tint = TextSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
