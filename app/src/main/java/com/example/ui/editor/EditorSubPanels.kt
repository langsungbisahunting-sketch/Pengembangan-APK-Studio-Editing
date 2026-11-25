package com.example.ui.editor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FileCopy
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.AspectRatioPreset
import com.example.model.CanvasFitMode
import com.example.model.ChromaKeyConfig
import com.example.model.ColorAdjustments
import com.example.model.FilterPreset
import com.example.model.PipOverlayItem
import com.example.model.SpeedCurvePreset
import com.example.model.TextOverlayItem
import com.example.model.TransitionType
import com.example.model.VideoEffectPreset
import com.example.model.VocalSeparationMode
import com.example.model.VoiceEffectPreset
import com.example.ui.common.formatTimecodeMs
import com.example.ui.theme.AmberAudio
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.NeonCoral
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCard
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.EditorToolTab
import com.example.viewmodel.EditorUiState

@Composable
fun EditorSubPanelContainer(
    uiState: EditorUiState,
    onSplitClip: () -> Unit,
    onTrimClip: (Long, Long) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onSpeedCurveChange: (SpeedCurvePreset) -> Unit,
    onClipVolumeChange: (Float) -> Unit,
    onExtractAudio: () -> Unit,
    onRotateClip: () -> Unit,
    onMirrorClip: () -> Unit,
    onDuplicateClip: () -> Unit,
    onMoveClip: (Int) -> Unit,
    onDeleteClip: () -> Unit,
    onPickAudio: () -> Unit,
    onAudioVolumeChange: (Float) -> Unit,
    onAudioVoiceEffectChange: (VoiceEffectPreset) -> Unit,
    onAudioVocalModeChange: (VocalSeparationMode) -> Unit,
    onToggleAudioNoiseReduction: () -> Unit,
    onClipVoiceEffectChange: (VoiceEffectPreset) -> Unit,
    onClipVocalModeChange: (VocalSeparationMode) -> Unit,
    onToggleClipNoiseReduction: () -> Unit,
    onToggleAudioFade: (Boolean?, Boolean?) -> Unit,
    onRemoveAudio: () -> Unit,
    onAddText: (String, Long, Long, String) -> Unit,
    onUpdateSelectedText: ((TextOverlayItem) -> TextOverlayItem) -> Unit,
    onDeleteSelectedText: () -> Unit,
    onGenerateAutoCaptions: (String) -> Unit,
    onTranslateCaptions: (String) -> Unit,
    onPickPipOverlay: () -> Unit,
    onUpdateSelectedPip: ((PipOverlayItem) -> PipOverlayItem) -> Unit,
    onDeleteSelectedPip: () -> Unit,
    onSelectVideoEffect: (VideoEffectPreset) -> Unit,
    onSelectFilter: (FilterPreset) -> Unit,
    onUpdateAdjustments: (ColorAdjustments) -> Unit,
    onUpdateChromaKey: (ChromaKeyConfig) -> Unit,
    onAddKeyframe: (Float, Float, Float, Float, Float) -> Unit,
    onClearKeyframes: () -> Unit,
    onSelectRatio: (AspectRatioPreset) -> Unit,
    onSelectFitMode: (CanvasFitMode) -> Unit,
    onSelectTransition: (TransitionType) -> Unit,
    onSelectCanvasColor: (Long) -> Unit
) {
    if (uiState.activeToolTab == EditorToolTab.MAIN) return

    Surface(
        color = StudioSurfaceElevated,
        border = BorderStroke(1.dp, StudioBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        when (uiState.activeToolTab) {
            EditorToolTab.EDIT -> ClipEditInspector(
                uiState = uiState,
                onSplitClip = onSplitClip,
                onTrimClip = onTrimClip,
                onSpeedChange = onSpeedChange,
                onSpeedCurveChange = onSpeedCurveChange,
                onClipVolumeChange = onClipVolumeChange,
                onExtractAudio = onExtractAudio,
                onRotateClip = onRotateClip,
                onMirrorClip = onMirrorClip,
                onDuplicateClip = onDuplicateClip,
                onMoveClip = onMoveClip,
                onDeleteClip = onDeleteClip
            )

            EditorToolTab.AUDIO -> AudioAndVoiceInspector(
                uiState = uiState,
                onPickAudio = onPickAudio,
                onExtractAudio = onExtractAudio,
                onAudioVolumeChange = onAudioVolumeChange,
                onAudioVoiceEffectChange = onAudioVoiceEffectChange,
                onAudioVocalModeChange = onAudioVocalModeChange,
                onToggleAudioNoiseReduction = onToggleAudioNoiseReduction,
                onClipVoiceEffectChange = onClipVoiceEffectChange,
                onClipVocalModeChange = onClipVocalModeChange,
                onToggleClipNoiseReduction = onToggleClipNoiseReduction,
                onToggleFade = onToggleAudioFade,
                onRemoveAudio = onRemoveAudio
            )

            EditorToolTab.TEXT -> TextAndAiCaptionInspector(
                uiState = uiState,
                onAddText = onAddText,
                onUpdateSelected = onUpdateSelectedText,
                onDeleteSelected = onDeleteSelectedText,
                onGenerateAutoCaptions = onGenerateAutoCaptions,
                onTranslateCaptions = onTranslateCaptions
            )

            EditorToolTab.PIP -> PipOverlayInspector(
                uiState = uiState,
                onPickPip = onPickPipOverlay,
                onUpdateSelectedPip = onUpdateSelectedPip,
                onDeleteSelectedPip = onDeleteSelectedPip
            )

            EditorToolTab.EFFECTS -> VideoEffectsInspector(
                selectedEffect = uiState.timeline.videoEffect,
                onSelectEffect = onSelectVideoEffect
            )

            EditorToolTab.FILTER -> FilterInspector(
                selectedFilter = uiState.timeline.filterPreset,
                onSelectFilter = onSelectFilter
            )

            EditorToolTab.ADJUST -> ColorAdjustInspector(
                adjustments = uiState.timeline.adjustments,
                onUpdate = onUpdateAdjustments
            )

            EditorToolTab.CHROMA -> ChromaKeyInspector(
                uiState = uiState,
                onUpdateChromaKey = onUpdateChromaKey
            )

            EditorToolTab.KEYFRAME -> KeyframeInspector(
                uiState = uiState,
                onAddKeyframe = onAddKeyframe,
                onClearKeyframes = onClearKeyframes
            )

            EditorToolTab.RATIO -> AspectRatioInspector(
                selectedRatio = uiState.timeline.aspectRatio,
                selectedFitMode = uiState.timeline.canvasFitMode,
                onSelectRatio = onSelectRatio,
                onSelectFitMode = onSelectFitMode
            )

            EditorToolTab.TRANSITION -> TransitionInspector(
                selectedTransition = uiState.selectedClip?.transitionOut ?: TransitionType.NONE,
                onSelectTransition = onSelectTransition
            )

            EditorToolTab.CANVAS -> CanvasBgInspector(
                selectedColorHex = uiState.timeline.canvasBgHex,
                onSelectColor = onSelectCanvasColor
            )

            EditorToolTab.MAIN -> {}
        }
    }
}

@Composable
private fun ClipEditInspector(
    uiState: EditorUiState,
    onSplitClip: () -> Unit,
    onTrimClip: (Long, Long) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onSpeedCurveChange: (SpeedCurvePreset) -> Unit,
    onClipVolumeChange: (Float) -> Unit,
    onExtractAudio: () -> Unit,
    onRotateClip: () -> Unit,
    onMirrorClip: () -> Unit,
    onDuplicateClip: () -> Unit,
    onMoveClip: (Int) -> Unit,
    onDeleteClip: () -> Unit
) {
    val clip = uiState.selectedClip
    var subMode by remember { mutableIntStateOf(0) } // 0=Actions, 1=Trim, 2=Speed/Curve, 3=Volume

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (clip != null) {
            when (subMode) {
                1 -> {
                    val maxDur = clip.sourceDurationMs.coerceAtLeast(1000L).toFloat()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Pangkas: ${formatTimecodeMs(clip.trimStartMs)} - ${formatTimecodeMs(clip.trimEndMs)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = ElectricCyan
                        )
                        TextButton(onClick = { subMode = 0 }) {
                            Text("Kembali", color = TextSecondary)
                        }
                    }
                    RangeSlider(
                        value = clip.trimStartMs.toFloat()..clip.trimEndMs.toFloat().coerceAtMost(maxDur),
                        onValueChange = { range ->
                            onTrimClip(range.start.toLong(), range.endInclusive.toLong())
                        },
                        valueRange = 0f..maxDur,
                        colors = SliderDefaults.colors(
                            thumbColor = ElectricCyan,
                            activeTrackColor = ElectricCyan
                        )
                    )
                }

                2 -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (clip.speedCurve == SpeedCurvePreset.NORMAL) {
                                "Kecepatan: ${String.format(java.util.Locale.US, "%.1fx", clip.speed)}"
                            } else {
                                "Kurva: ${clip.speedCurve.label}"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = NeonCoral
                        )
                        TextButton(onClick = { subMode = 0 }) {
                            Text("Kembali", color = TextSecondary)
                        }
                    }

                    // Speed Curve Graph Preview
                    if (clip.speedCurve != SpeedCurvePreset.NORMAL) {
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(StudioCard)
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            val pts = clip.speedCurve.points
                            val path = Path()
                            pts.forEachIndexed { i, spd ->
                                val x = (i.toFloat() / (pts.size - 1)) * size.width
                                val y = size.height - ((spd / 3.0f).coerceIn(0.05f, 1f) * size.height)
                                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                                drawCircle(ElectricCyan, radius = 3.5.dp.toPx(), center = Offset(x, y))
                            }
                            drawPath(path, color = NeonCoral, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round))
                        }
                    }

                    Text("Kurva Kecepatan", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(SpeedCurvePreset.entries) { curve ->
                            val selected = clip.speedCurve == curve
                            FilterChip(
                                selected = selected,
                                onClick = { onSpeedCurveChange(curve) },
                                label = { Text(curve.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ElectricCyan.copy(alpha = 0.2f),
                                    selectedLabelColor = ElectricCyan
                                )
                            )
                        }
                    }

                    Text("Kecepatan Normal (0.1x - 10x)", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Slider(
                        value = clip.speed,
                        onValueChange = onSpeedChange,
                        valueRange = 0.1f..10.0f,
                        colors = SliderDefaults.colors(thumbColor = NeonCoral, activeTrackColor = NeonCoral)
                    )
                }

                3 -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Volume Klip: ${(clip.volume * 100).toInt()}%",
                            style = MaterialTheme.typography.labelMedium,
                            color = ElectricCyan
                        )
                        TextButton(onClick = { subMode = 0 }) {
                            Text("Kembali", color = TextSecondary)
                        }
                    }
                    Slider(
                        value = clip.volume,
                        onValueChange = onClipVolumeChange,
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(thumbColor = ElectricCyan, activeTrackColor = ElectricCyan)
                    )
                }
            }
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                MiniActionPill("Bagi", Icons.Filled.ContentCut, ElectricCyan, "edit_split_button") {
                    onSplitClip()
                }
            }
            item {
                MiniActionPill("Pangkas", Icons.Filled.ContentCut, TextPrimary, "edit_trim_button") {
                    subMode = if (subMode == 1) 0 else 1
                }
            }
            item {
                MiniActionPill("Kecepatan", Icons.Filled.Speed, NeonCoral, "edit_speed_button") {
                    subMode = if (subMode == 2) 0 else 2
                }
            }
            item {
                MiniActionPill("Volume", Icons.Filled.VolumeUp, AmberAudio, "edit_volume_button") {
                    subMode = if (subMode == 3) 0 else 3
                }
            }
            item {
                MiniActionPill("Ekstrak Audio", Icons.Filled.Audiotrack, AmberAudio, "edit_extract_audio_button") {
                    onExtractAudio()
                }
            }
            item {
                MiniActionPill("Putar 90°", Icons.Filled.RotateRight, TextPrimary, "edit_rotate_button") {
                    onRotateClip()
                }
            }
            item {
                MiniActionPill("Cermin", Icons.Filled.Flip, TextPrimary, "edit_mirror_button") {
                    onMirrorClip()
                }
            }
            item {
                MiniActionPill("Duplikat", Icons.Filled.FileCopy, TextPrimary, "edit_duplicate_button") {
                    onDuplicateClip()
                }
            }
            item {
                MiniActionPill("Geser <", Icons.AutoMirrored.Filled.KeyboardArrowLeft, TextSecondary, "edit_move_left_button") {
                    onMoveClip(-1)
                }
            }
            item {
                MiniActionPill("Geser >", Icons.AutoMirrored.Filled.KeyboardArrowRight, TextSecondary, "edit_move_right_button") {
                    onMoveClip(1)
                }
            }
            item {
                MiniActionPill("Hapus", Icons.Filled.DeleteOutline, NeonCoral, "edit_delete_button") {
                    onDeleteClip()
                }
            }
        }
    }
}

@Composable
private fun MiniActionPill(
    label: String,
    icon: ImageVector,
    tint: Color,
    tag: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = StudioCard,
        border = BorderStroke(1.dp, StudioBorder),
        modifier = Modifier.testTag(tag)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(18.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = TextPrimary)
        }
    }
}

@Composable
private fun FilterInspector(
    selectedFilter: FilterPreset,
    onSelectFilter: (FilterPreset) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(FilterPreset.entries) { preset ->
            val selected = preset == selectedFilter
            val previewColors = when (preset) {
                FilterPreset.NONE -> listOf(Color(0xFF2B3245), Color(0xFF4C5673))
                FilterPreset.CINEMATIC -> listOf(Color(0xFF0E293D), Color(0xFF3E6C84))
                FilterPreset.WARM_SUN -> listOf(Color(0xFF7B3E19), Color(0xFFE09F3E))
                FilterPreset.COOL_MINT -> listOf(Color(0xFF114B5F), Color(0xFF00E5FF))
                FilterPreset.VIVID -> listOf(Color(0xFF9E1946), Color(0xFFFF3B6B))
                FilterPreset.BW_CLASSIC -> listOf(Color(0xFF1F1F1F), Color(0xFF9E9E9E))
                FilterPreset.SEPIA -> listOf(Color(0xFF4A3728), Color(0xFFA68A64))
                FilterPreset.CYBER -> listOf(Color(0xFF3B0A45), Color(0xFF00E5FF))
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onSelectFilter(preset) }
                    .testTag("filter_chip_${preset.name.lowercase()}")
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Brush.linearGradient(previewColors))
                        .border(
                            width = if (selected) 2.dp else 1.dp,
                            color = if (selected) ElectricCyan else StudioBorder,
                            shape = RoundedCornerShape(12.dp)
                        )
                )
                Text(
                    text = preset.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (selected) ElectricCyan else TextSecondary
                )
            }
        }
    }
}

@Composable
private fun ColorAdjustInspector(
    adjustments: ColorAdjustments,
    onUpdate: (ColorAdjustments) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Color Grading Manual", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
            TextButton(onClick = { onUpdate(ColorAdjustments()) }) {
                Text("Reset", color = NeonCoral)
            }
        }
        AdjustSliderRow("Kecerahan", adjustments.brightness, -0.4f..0.4f) {
            onUpdate(adjustments.copy(brightness = it))
        }
        AdjustSliderRow("Kontras", adjustments.contrast, -0.4f..0.4f) {
            onUpdate(adjustments.copy(contrast = it))
        }
        AdjustSliderRow("Saturasi", adjustments.saturation, -0.8f..0.8f) {
            onUpdate(adjustments.copy(saturation = it))
        }
        AdjustSliderRow("Exposure", adjustments.exposure, -0.4f..0.4f) {
            onUpdate(adjustments.copy(exposure = it))
        }
        AdjustSliderRow("Sharpen", adjustments.sharpen, 0f..1f) {
            onUpdate(adjustments.copy(sharpen = it))
        }
        AdjustSliderRow("Vignette", adjustments.vignette, 0f..0.85f) {
            onUpdate(adjustments.copy(vignette = it))
        }
    }
}

@Composable
private fun AdjustSliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary,
            modifier = Modifier.width(82.dp)
        )
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(thumbColor = ElectricCyan, activeTrackColor = ElectricCyan)
        )
    }
}

@Composable
private fun AspectRatioInspector(
    selectedRatio: AspectRatioPreset,
    selectedFitMode: CanvasFitMode,
    onSelectRatio: (AspectRatioPreset) -> Unit,
    onSelectFitMode: (CanvasFitMode) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(AspectRatioPreset.entries) { ratio ->
                val selected = ratio == selectedRatio
                Surface(
                    onClick = { onSelectRatio(ratio) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (selected) ElectricCyan.copy(alpha = 0.16f) else StudioCard,
                    border = BorderStroke(1.5.dp, if (selected) ElectricCyan else StudioBorder),
                    modifier = Modifier.testTag("ratio_chip_${ratio.name.lowercase()}")
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = ratio.label,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (selected) ElectricCyan else TextPrimary
                        )
                        Text(
                            text = ratio.subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Mode Kanvas:", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
            CanvasFitMode.entries.forEach { mode ->
                val active = mode == selectedFitMode
                FilterChip(
                    selected = active,
                    onClick = { onSelectFitMode(mode) },
                    label = { Text(mode.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ElectricCyan.copy(alpha = 0.18f),
                        selectedLabelColor = ElectricCyan
                    )
                )
            }
        }
    }
}

@Composable
private fun TransitionInspector(
    selectedTransition: TransitionType,
    onSelectTransition: (TransitionType) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(TransitionType.entries) { transition ->
            val selected = transition == selectedTransition
            Surface(
                onClick = { onSelectTransition(transition) },
                shape = RoundedCornerShape(12.dp),
                color = if (selected) ElectricCyan.copy(alpha = 0.16f) else StudioCard,
                border = BorderStroke(1.5.dp, if (selected) ElectricCyan else StudioBorder)
            ) {
                Text(
                    text = transition.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) ElectricCyan else TextPrimary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
        }
    }
}

@Composable
private fun CanvasBgInspector(
    selectedColorHex: Long,
    onSelectColor: (Long) -> Unit
) {
    val swatches = listOf(
        0xFF000000L,
        0xFF151924L,
        0xFFFFFFFFL,
        0xFF0A2E36L,
        0xFF3E1226L,
        0xFF21163DL
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Latar Kanvas:", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
        swatches.forEach { hex ->
            val selected = hex == selectedColorHex
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color(hex))
                    .border(
                        width = if (selected) 2.5.dp else 1.dp,
                        color = if (selected) ElectricCyan else StudioBorder,
                        shape = CircleShape
                    )
                    .clickable { onSelectColor(hex) }
            )
        }
    }
}
