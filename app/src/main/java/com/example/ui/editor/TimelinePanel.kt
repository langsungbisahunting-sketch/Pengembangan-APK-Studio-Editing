package com.example.ui.editor

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesomeMotion
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.media.MediaMetadataHelper
import com.example.model.ProjectTimelineData
import com.example.model.SpeedCurvePreset
import com.example.model.TransitionType
import com.example.model.VideoClip
import com.example.ui.theme.AmberAudio
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.NeonCoral
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCard
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletOverlay
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun TimelinePanel(
    timeline: ProjectTimelineData,
    currentPositionMs: Long,
    isPlaying: Boolean,
    selectedClipIndex: Int,
    selectedTextId: String?,
    selectedPipId: String?,
    timelineZoom: Float,
    onSeekTo: (Long) -> Unit,
    onSelectClip: (Int) -> Unit,
    onTrimClip: (Long, Long) -> Unit,
    onTapTransition: (Int) -> Unit,
    onSelectText: (String) -> Unit,
    onSelectPip: (String) -> Unit,
    onAddMediaClick: () -> Unit,
    onAddAudioClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val density = LocalDensity.current
    val dpPerSecond = 48.dp * timelineZoom
    val pxPerSecond = with(density) { dpPerSecond.toPx() }
    val pxPerMs = pxPerSecond / 1000f
    val totalDurationMs = timeline.totalDurationMs.coerceAtLeast(1000L)

    LaunchedEffect(currentPositionMs, isPlaying, timelineZoom) {
        val targetScroll = (currentPositionMs * pxPerMs).roundToInt()
        if (isPlaying || (!scrollState.isScrollInProgress && abs(scrollState.value - targetScroll) > 6)) {
            scrollState.scrollTo(targetScroll.coerceIn(0, scrollState.maxValue))
        }
    }

    LaunchedEffect(scrollState.value, scrollState.isScrollInProgress) {
        if (scrollState.isScrollInProgress && !isPlaying && pxPerMs > 0f) {
            val posMs = (scrollState.value / pxPerMs).toLong().coerceIn(0L, timeline.totalDurationMs)
            onSeekTo(posMs)
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .background(StudioSurface)
            .border(1.dp, StudioBorder)
    ) {
        val halfViewport = maxWidth / 2f

        Row(
            modifier = Modifier
                .fillMaxSize()
                .horizontalScroll(scrollState)
        ) {
            Spacer(modifier = Modifier.width(halfViewport))

            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // 1. Time Ruler
                TimelineRuler(
                    totalDurationMs = totalDurationMs,
                    dpPerSecond = dpPerSecond
                )

                // 2. Main Video Track (V1)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.height(62.dp)
                ) {
                    timeline.clips.forEachIndexed { index, clip ->
                        val widthDp = ((clip.effectiveDurationMs / 1000f) * dpPerSecond.value).dp
                            .coerceAtLeast(56.dp)
                        val isSelected = index == selectedClipIndex
                        VideoClipTrackItem(
                            clip = clip,
                            widthDp = widthDp,
                            dpPerSecond = dpPerSecond,
                            isSelected = isSelected,
                            onClick = { onSelectClip(index) },
                            onTrimDeltaStart = { deltaPx ->
                                val deltaMs = ((deltaPx / pxPerMs) * clip.effectiveSpeedAverage).toLong()
                                onTrimClip(clip.trimStartMs + deltaMs, clip.trimEndMs)
                            },
                            onTrimDeltaEnd = { deltaPx ->
                                val deltaMs = ((deltaPx / pxPerMs) * clip.effectiveSpeedAverage).toLong()
                                onTrimClip(clip.trimStartMs, clip.trimEndMs + deltaMs)
                            },
                            modifier = Modifier.testTag("timeline_clip_$index")
                        )

                        if (index < timeline.clips.lastIndex) {
                            val hasTransition = clip.transitionOut != TransitionType.NONE
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 2.dp)
                                    .size(22.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (hasTransition) ElectricCyan else StudioCard)
                                    .border(1.dp, StudioBorder, RoundedCornerShape(6.dp))
                                    .clickable { onTapTransition(index) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AutoAwesomeMotion,
                                    contentDescription = "Transisi",
                                    tint = if (hasTransition) Color(0xFF041318) else TextSecondary,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Surface(
                        onClick = onAddMediaClick,
                        shape = RoundedCornerShape(10.dp),
                        color = StudioCard,
                        border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("timeline_add_clip_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = "Tambah Klip",
                                tint = ElectricCyan,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                // 3. Overlay / PiP Track (P1)
                if (timeline.pipOverlays.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .width(((totalDurationMs / 1000f) * dpPerSecond.value).dp.coerceAtLeast(160.dp))
                            .height(26.dp)
                    ) {
                        timeline.pipOverlays.forEach { pip ->
                            val offsetDp = ((pip.startMs / 1000f) * dpPerSecond.value).dp
                            val itemWidthDp = (((pip.endMs - pip.startMs) / 1000f) * dpPerSecond.value).dp
                                .coerceAtLeast(56.dp)
                            val selected = pip.id == selectedPipId
                            Surface(
                                onClick = { onSelectPip(pip.id) },
                                shape = RoundedCornerShape(7.dp),
                                color = if (selected) EmeraldSuccess else EmeraldSuccess.copy(alpha = 0.35f),
                                border = BorderStroke(1.dp, if (selected) Color.White else EmeraldSuccess),
                                modifier = Modifier
                                    .offset(x = offsetDp)
                                    .width(itemWidthDp)
                                    .fillMaxHeight()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Layers,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "PiP: ${pip.displayName}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                // 4. Text & Caption Track (T1)
                if (timeline.textOverlays.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .width(((totalDurationMs / 1000f) * dpPerSecond.value).dp.coerceAtLeast(160.dp))
                            .height(26.dp)
                    ) {
                        timeline.textOverlays.forEach { item ->
                            val offsetDp = ((item.startMs / 1000f) * dpPerSecond.value).dp
                            val itemWidthDp = (((item.endMs - item.startMs) / 1000f) * dpPerSecond.value).dp
                                .coerceAtLeast(52.dp)
                            val selected = item.id == selectedTextId
                            Surface(
                                onClick = { onSelectText(item.id) },
                                shape = RoundedCornerShape(7.dp),
                                color = if (selected) VioletOverlay else VioletOverlay.copy(alpha = 0.35f),
                                border = BorderStroke(1.dp, if (selected) Color.White else VioletOverlay),
                                modifier = Modifier
                                    .offset(x = offsetDp)
                                    .width(itemWidthDp)
                                    .fillMaxHeight()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.TextFields,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = item.text,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                // 5. Audio Track (A1)
                val totalTrackWidthDp = ((totalDurationMs / 1000f) * dpPerSecond.value).dp.coerceAtLeast(160.dp)
                if (timeline.audioTrack != null) {
                    Surface(
                        onClick = onAddAudioClick,
                        shape = RoundedCornerShape(8.dp),
                        color = AmberAudio.copy(alpha = 0.18f),
                        border = BorderStroke(1.dp, AmberAudio.copy(alpha = 0.7f)),
                        modifier = Modifier
                            .width(totalTrackWidthDp)
                            .height(30.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val barW = 3.dp.toPx()
                                val gap = 3.dp.toPx()
                                val step = barW + gap
                                var x = 4.dp.toPx()
                                var i = 0
                                while (x < size.width - 4.dp.toPx()) {
                                    val amp = (0.25f + 0.65f * abs(sin(i * 0.55f))) * size.height * 0.75f
                                    drawRoundRect(
                                        color = AmberAudio.copy(alpha = 0.55f),
                                        topLeft = Offset(x, (size.height - amp) / 2f),
                                        size = Size(barW, amp),
                                        cornerRadius = CornerRadius(2f, 2f)
                                    )
                                    x += step
                                    i++
                                }
                            }
                            Row(
                                modifier = Modifier
                                    .align(Alignment.CenterStart)
                                    .padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.MusicNote,
                                    contentDescription = null,
                                    tint = AmberAudio,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = timeline.audioTrack.title,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                } else {
                    Surface(
                        onClick = onAddAudioClick,
                        shape = RoundedCornerShape(8.dp),
                        color = StudioSurfaceElevated,
                        border = BorderStroke(1.dp, StudioBorder),
                        modifier = Modifier
                            .height(28.dp)
                            .testTag("timeline_add_audio_pill")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Filled.MusicNote, null, tint = AmberAudio, modifier = Modifier.size(14.dp))
                            Text(
                                text = "+ Tambah / Ekstrak Musik",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(halfViewport))
        }

        // Fixed Center Playhead
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxHeight()
                .width(14.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(ElectricCyan)
            )
            Box(
                modifier = Modifier
                    .padding(top = 6.dp)
                    .width(2.dp)
                    .fillMaxHeight()
                    .background(Color.White)
            )
        }
    }
}

@Composable
private fun TimelineRuler(
    totalDurationMs: Long,
    dpPerSecond: Dp
) {
    val totalSec = ((totalDurationMs / 1000L) + 2).toInt().coerceAtLeast(5)
    val totalWidth = (totalSec * dpPerSecond.value).dp

    Box(
        modifier = Modifier
            .width(totalWidth)
            .height(18.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val secPx = dpPerSecond.toPx()
            for (s in 0..totalSec) {
                val x = s * secPx
                drawLine(
                    color = TextMuted,
                    start = Offset(x, size.height * 0.45f),
                    end = Offset(x, size.height),
                    strokeWidth = 1.5f
                )
                if (s < totalSec) {
                    val halfX = x + secPx * 0.5f
                    drawLine(
                        color = TextMuted.copy(alpha = 0.45f),
                        start = Offset(halfX, size.height * 0.72f),
                        end = Offset(halfX, size.height),
                        strokeWidth = 1f
                    )
                }
            }
        }
        for (s in 0..totalSec step 2) {
            Text(
                text = "${s}s",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = TextMuted,
                modifier = Modifier.offset(x = (s * dpPerSecond.value + 3f).dp)
            )
        }
    }
}

@Composable
private fun VideoClipTrackItem(
    clip: VideoClip,
    widthDp: Dp,
    dpPerSecond: Dp,
    isSelected: Boolean,
    onClick: () -> Unit,
    onTrimDeltaStart: (Float) -> Unit,
    onTrimDeltaEnd: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val thumbCount = (widthDp.value / 44f).toInt().coerceIn(2, 8)
    var bitmaps by remember(clip.id, clip.trimStartMs, clip.trimEndMs, thumbCount) {
        mutableStateOf<List<Bitmap>>(emptyList())
    }

    LaunchedEffect(clip.id, clip.trimStartMs, clip.trimEndMs, thumbCount) {
        bitmaps = MediaMetadataHelper.extractClipThumbnails(context, clip, thumbCount)
    }

    Box(
        modifier = modifier
            .width(widthDp)
            .fillMaxHeight()
            .clip(RoundedCornerShape(10.dp))
            .background(StudioCard)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) ElectricCyan else StudioBorder,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
    ) {
        if (bitmaps.isNotEmpty()) {
            Row(modifier = Modifier.fillMaxSize()) {
                bitmaps.forEach { bmp ->
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.18f))
        )

        // Speed / Curve / Mute badges
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(horizontal = if (isSelected) 16.dp else 6.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (clip.speedCurve != SpeedCurvePreset.NORMAL) {
                Surface(shape = RoundedCornerShape(4.dp), color = NeonCoral) {
                    Text(
                        text = clip.speedCurve.label,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            } else if (clip.speed != 1.0f) {
                Surface(shape = RoundedCornerShape(4.dp), color = NeonCoral) {
                    Text(
                        text = "${clip.speed}x",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }
            if (clip.volume <= 0.01f && !clip.isImage) {
                Surface(shape = RoundedCornerShape(4.dp), color = Color.Black.copy(alpha = 0.7f)) {
                    Icon(
                        imageVector = Icons.Filled.VolumeOff,
                        contentDescription = "Bisukan",
                        tint = Color.White,
                        modifier = Modifier
                            .padding(2.dp)
                            .size(10.dp)
                    )
                }
            }
        }

        // Keyframe Diamond Markers on bottom of clip
        clip.keyframes.forEach { kf ->
            val kfX = ((kf.timeOffsetMs / 1000f) * dpPerSecond.value).dp
                .coerceIn(14.dp, (widthDp - 14.dp).coerceAtLeast(14.dp))
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = kfX - 4.dp, y = (-4).dp)
                    .size(8.dp)
                    .rotate(45f)
                    .background(ElectricCyan)
                    .border(1.dp, Color.White)
            )
        }

        Surface(
            shape = RoundedCornerShape(4.dp),
            color = Color.Black.copy(alpha = 0.68f),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = if (isSelected) 16.dp else 6.dp, vertical = 4.dp)
        ) {
            Text(
                text = String.format(java.util.Locale.US, "%.1fs", clip.effectiveDurationMs / 1000f),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = Color.White,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
            )
        }

        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .width(14.dp)
                    .fillMaxHeight()
                    .background(ElectricCyan)
                    .pointerInput(clip.id) {
                        detectHorizontalDragGestures { change, dragAmount ->
                            change.consume()
                            onTrimDeltaStart(dragAmount)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(22.dp)
                        .background(Color(0xFF041318), RoundedCornerShape(2.dp))
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(14.dp)
                    .fillMaxHeight()
                    .background(ElectricCyan)
                    .pointerInput(clip.id) {
                        detectHorizontalDragGestures { change, dragAmount ->
                            change.consume()
                            onTrimDeltaEnd(dragAmount)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(22.dp)
                        .background(Color(0xFF041318), RoundedCornerShape(2.dp))
                )
            }
        }
    }
}
