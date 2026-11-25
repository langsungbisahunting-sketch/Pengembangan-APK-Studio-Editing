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
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.FileCopy
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PanToolAlt
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.AspectRatioPreset
import com.example.model.AudioTrackRole
import com.example.model.CanvasFitMode
import com.example.model.ChromaKeyConfig
import com.example.model.ColorAdjustments
import com.example.model.FilterPreset
import com.example.model.PanZoomPreset
import com.example.model.PipOverlayItem
import com.example.model.SpeedCurvePreset
import com.example.model.TextOverlayItem
import com.example.model.TransitionType
import com.example.model.TtsVoiceStyle
import com.example.model.VideoEffectPreset
import com.example.model.VocalSeparationMode
import com.example.model.VoiceEffectPreset
import com.example.ui.common.formatTimecodeMs
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.EnergyOrange
import com.example.ui.theme.EnergyOrangeSoft
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.PureWhite
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
    onCropClip: (Float) -> Unit,
    onSetPanZoom: (PanZoomPreset) -> Unit,
    onResetClipTransform: () -> Unit,
    onSpeedChange: (Float) -> Unit,
    onSpeedCurveChange: (SpeedCurvePreset) -> Unit,
    onToggleSmoothSlowMo: () -> Unit,
    onToggleReverseClip: () -> Unit,
    onClipVolumeChange: (Float) -> Unit,
    onExtractAudio: () -> Unit,
    onRotateClip: () -> Unit,
    onMirrorClip: () -> Unit,
    onDuplicateClip: () -> Unit,
    onMoveClip: (Int) -> Unit,
    onDeleteClip: () -> Unit,
    onSelectAudioRole: (AudioTrackRole) -> Unit,
    onPickAudio: () -> Unit,
    onSplitVocalAndMusic: () -> Unit,
    onAudioVolumeChange: (AudioTrackRole, Float) -> Unit,
    onAudioTrimChange: (AudioTrackRole, Long, Long) -> Unit,
    onShiftAudioOffset: (AudioTrackRole, Long) -> Unit,
    onAudioVoiceEffectChange: (AudioTrackRole, VoiceEffectPreset) -> Unit,
    onAudioVocalModeChange: (AudioTrackRole, VocalSeparationMode) -> Unit,
    onToggleAudioNoiseReduction: (AudioTrackRole) -> Unit,
    onToggleAudioDucking: (AudioTrackRole) -> Unit,
    onClipVoiceEffectChange: (VoiceEffectPreset) -> Unit,
    onClipVocalModeChange: (VocalSeparationMode) -> Unit,
    onToggleClipNoiseReduction: () -> Unit,
    onToggleAudioFade: (AudioTrackRole, Boolean?, Boolean?) -> Unit,
    onRemoveAudio: (AudioTrackRole) -> Unit,
    onSynthesizeTtsNarration: (String, TtsVoiceStyle) -> Unit,
    onPolishNarrationScript: (String, (String) -> Unit) -> Unit,
    onAddText: (String, Long, Long, String, Boolean, Boolean) -> Unit,
    onUpdateSelectedText: ((TextOverlayItem) -> TextOverlayItem) -> Unit,
    onUpdateTextById: (String, (TextOverlayItem) -> TextOverlayItem) -> Unit,
    onSelectText: (String?) -> Unit,
    onSeekTo: (Long) -> Unit,
    onDeleteSelectedText: () -> Unit,
    onGenerateAutoCaptions: (String) -> Unit,
    onAutoTranscribeAndTranslate: (String) -> Unit,
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
    onSelectCanvasColor: (Long) -> Unit,
    onPickCanvasImage: () -> Unit
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
                onCropClip = onCropClip,
                onSetPanZoom = onSetPanZoom,
                onResetClipTransform = onResetClipTransform,
                onSpeedChange = onSpeedChange,
                onSpeedCurveChange = onSpeedCurveChange,
                onToggleSmoothSlowMo = onToggleSmoothSlowMo,
                onToggleReverseClip = onToggleReverseClip,
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
                onSelectAudioRole = onSelectAudioRole,
                onPickAudio = onPickAudio,
                onExtractAudio = onExtractAudio,
                onSplitVocalAndMusic = onSplitVocalAndMusic,
                onAudioVolumeChange = onAudioVolumeChange,
                onAudioTrimChange = onAudioTrimChange,
                onShiftAudioOffset = onShiftAudioOffset,
                onAudioVoiceEffectChange = onAudioVoiceEffectChange,
                onAudioVocalModeChange = onAudioVocalModeChange,
                onToggleAudioNoiseReduction = onToggleAudioNoiseReduction,
                onToggleAudioDucking = onToggleAudioDucking,
                onClipVoiceEffectChange = onClipVoiceEffectChange,
                onClipVocalModeChange = onClipVocalModeChange,
                onToggleClipNoiseReduction = onToggleClipNoiseReduction,
                onToggleFade = onToggleAudioFade,
                onRemoveAudio = onRemoveAudio,
                onSynthesizeTtsNarration = onSynthesizeTtsNarration,
                onPolishNarrationScript = onPolishNarrationScript
            )

            EditorToolTab.TEXT -> TextAndAiCaptionInspector(
                uiState = uiState,
                onAddText = onAddText,
                onUpdateSelected = onUpdateSelectedText,
                onUpdateTextById = onUpdateTextById,
                onSelectText = onSelectText,
                onSeekTo = onSeekTo,
                onDeleteSelected = onDeleteSelectedText,
                onGenerateAutoCaptions = onGenerateAutoCaptions,
                onAutoTranscribeAndTranslate = onAutoTranscribeAndTranslate,
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
                onSelectFitMode = onSelectFitMode,
                onPickCanvasImage = onPickCanvasImage
            )

            EditorToolTab.TRANSITION -> TransitionInspector(
                selectedTransition = uiState.selectedClip?.transitionOut ?: TransitionType.NONE,
                onSelectTransition = onSelectTransition
            )

            EditorToolTab.CANVAS -> CanvasBgInspector(
                selectedColorHex = uiState.timeline.canvasBgHex,
                hasCustomImage = !uiState.timeline.canvasBgImageUri.isNullOrBlank(),
                onSelectColor = onSelectCanvasColor,
                onPickCanvasImage = onPickCanvasImage
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
    onCropClip: (Float) -> Unit,
    onSetPanZoom: (PanZoomPreset) -> Unit,
    onResetClipTransform: () -> Unit,
    onSpeedChange: (Float) -> Unit,
    onSpeedCurveChange: (SpeedCurvePreset) -> Unit,
    onToggleSmoothSlowMo: () -> Unit,
    onToggleReverseClip: () -> Unit,
    onClipVolumeChange: (Float) -> Unit,
    onExtractAudio: () -> Unit,
    onRotateClip: () -> Unit,
    onMirrorClip: () -> Unit,
    onDuplicateClip: () -> Unit,
    onMoveClip: (Int) -> Unit,
    onDeleteClip: () -> Unit
) {
    val clip = uiState.selectedClip
    var subMode by remember { mutableIntStateOf(0) } // 0=Actions, 1=Trim, 2=Speed/Curve, 3=Volume, 4=Crop, 5=Pan&Zoom

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
                            color = EnergyOrange
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
                            thumbColor = EnergyOrange,
                            activeTrackColor = EnergyOrange
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
                            color = EnergyOrange,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(onClick = { subMode = 0 }) {
                            Text("Kembali", color = TextSecondary)
                        }
                    }

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
                                drawCircle(GoldAccent, radius = 3.5.dp.toPx(), center = Offset(x, y))
                            }
                            drawPath(path, color = EnergyOrange, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round))
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
                                    selectedContainerColor = EnergyOrangeSoft,
                                    selectedLabelColor = EnergyOrange
                                )
                            )
                        }
                    }

                    Text("Kecepatan Normal (0.1x - 10x)", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Slider(
                        value = clip.speed,
                        onValueChange = onSpeedChange,
                        valueRange = 0.1f..10.0f,
                        colors = SliderDefaults.colors(thumbColor = EnergyOrange, activeTrackColor = EnergyOrange)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Smooth Slow-Mo (Interpolasi Halus)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary
                        )
                        Switch(
                            checked = clip.smoothSlowMo,
                            onCheckedChange = { onToggleSmoothSlowMo() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = PureWhite,
                                checkedTrackColor = EnergyOrange
                            )
                        )
                    }
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
                            color = GoldAccent
                        )
                        TextButton(onClick = { subMode = 0 }) {
                            Text("Kembali", color = TextSecondary)
                        }
                    }
                    Slider(
                        value = clip.volume,
                        onValueChange = onClipVolumeChange,
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(thumbColor = GoldAccent, activeTrackColor = GoldAccent)
                    )
                }

                4 -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Crop / Zoom Panel: ${String.format(java.util.Locale.US, "%.2fx", clip.cropScale)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = EnergyOrange,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(onClick = { subMode = 0 }) {
                            Text("Kembali", color = TextSecondary)
                        }
                    }
                    Slider(
                        value = clip.cropScale,
                        onValueChange = onCropClip,
                        valueRange = 1.0f..2.5f,
                        colors = SliderDefaults.colors(thumbColor = EnergyOrange, activeTrackColor = EnergyOrange)
                    )
                    TextButton(onClick = onResetClipTransform) {
                        Text("Reset Posisi & Cubit Kanvas", color = GoldAccent)
                    }
                }

                5 -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Pan & Zoom (Gerak Kamera Ken Burns)",
                            style = MaterialTheme.typography.labelMedium,
                            color = GoldAccent,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(onClick = { subMode = 0 }) {
                            Text("Kembali", color = TextSecondary)
                        }
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(PanZoomPreset.entries) { pz ->
                            FilterChip(
                                selected = clip.panZoom == pz,
                                onClick = { onSetPanZoom(pz) },
                                label = { Text(pz.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = EnergyOrangeSoft,
                                    selectedLabelColor = EnergyOrange
                                )
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = onResetClipTransform,
                        border = BorderStroke(1.dp, GoldAccent),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Reset Posisi, Cubit & Putar Klip", color = GoldAccent)
                    }
                }
            }
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                MiniActionPill("Bagi", Icons.Filled.ContentCut, EnergyOrange, "edit_split_button") {
                    onSplitClip()
                }
            }
            item {
                MiniActionPill("Pangkas", Icons.Filled.ContentCut, PureWhite, "edit_trim_button") {
                    subMode = if (subMode == 1) 0 else 1
                }
            }
            item {
                MiniActionPill("Pan & Zoom", Icons.Filled.PanToolAlt, GoldAccent, "edit_pan_zoom_button") {
                    subMode = if (subMode == 5) 0 else 5
                }
            }
            item {
                MiniActionPill("Crop", Icons.Filled.Crop, EnergyOrange, "edit_crop_button") {
                    subMode = if (subMode == 4) 0 else 4
                }
            }
            item {
                MiniActionPill("Kecepatan", Icons.Filled.Speed, EnergyOrange, "edit_speed_button") {
                    subMode = if (subMode == 2) 0 else 2
                }
            }
            item {
                val revActive = clip?.isReversed == true
                MiniActionPill(
                    label = if (revActive) "Reverse ON" else "Reverse",
                    icon = Icons.Filled.FastRewind,
                    tint = if (revActive) EnergyOrange else PureWhite,
                    tag = "edit_reverse_button"
                ) {
                    onToggleReverseClip()
                }
            }
            item {
                MiniActionPill("Volume", Icons.Filled.VolumeUp, GoldAccent, "edit_volume_button") {
                    subMode = if (subMode == 3) 0 else 3
                }
            }
            item {
                MiniActionPill("Ekstrak Audio", Icons.Filled.Audiotrack, GoldAccent, "edit_extract_audio_button") {
                    onExtractAudio()
                }
            }
            item {
                MiniActionPill("Putar 90°", Icons.Filled.RotateRight, PureWhite, "edit_rotate_button") {
                    onRotateClip()
                }
            }
            item {
                MiniActionPill("Cermin", Icons.Filled.Flip, PureWhite, "edit_mirror_button") {
                    onMirrorClip()
                }
            }
            item {
                MiniActionPill("Duplikat", Icons.Filled.FileCopy, PureWhite, "edit_duplicate_button") {
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
                MiniActionPill("Hapus", Icons.Filled.DeleteOutline, CrimsonAlert, "edit_delete_button") {
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
                FilterPreset.NONE -> listOf(Color(0xFF23232A), Color(0xFF3E3E4A))
                FilterPreset.KOMIK_POP -> listOf(Color(0xFFFF6B00), Color(0xFFFFC107))
                FilterPreset.KOMIK_NOIR -> listOf(Color(0xFF0B0B0D), Color(0xFFF5F5F7))
                FilterPreset.CINEMATIC -> listOf(Color(0xFF16161A), Color(0xFF4A5568))
                FilterPreset.WARM_SUN -> listOf(Color(0xFF8C3B00), Color(0xFFFF9E44))
                FilterPreset.COOL_MINT -> listOf(Color(0xFF15293A), Color(0xFF4682B4))
                FilterPreset.VIVID -> listOf(Color(0xFFC41E3A), Color(0xFFFF6B00))
                FilterPreset.BW_CLASSIC -> listOf(Color(0xFF1F1F1F), Color(0xFF9E9E9E))
                FilterPreset.SEPIA -> listOf(Color(0xFF4A3728), Color(0xFFA68A64))
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
                            color = if (selected) EnergyOrange else StudioBorder,
                            shape = RoundedCornerShape(12.dp)
                        )
                )
                Text(
                    text = preset.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (selected) EnergyOrange else TextSecondary,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
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
            .height(190.dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Color Grading Komik Narasi", style = MaterialTheme.typography.labelLarge, color = PureWhite)
            TextButton(onClick = { onUpdate(ColorAdjustments()) }) {
                Text("Reset", color = CrimsonAlert)
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
        AdjustSliderRow("Highlights", adjustments.highlights, -0.4f..0.4f) {
            onUpdate(adjustments.copy(highlights = it))
        }
        AdjustSliderRow("Shadows", adjustments.shadows, -0.4f..0.4f) {
            onUpdate(adjustments.copy(shadows = it))
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
            modifier = Modifier.width(86.dp)
        )
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(thumbColor = EnergyOrange, activeTrackColor = EnergyOrange)
        )
    }
}

@Composable
private fun AspectRatioInspector(
    selectedRatio: AspectRatioPreset,
    selectedFitMode: CanvasFitMode,
    onSelectRatio: (AspectRatioPreset) -> Unit,
    onSelectFitMode: (CanvasFitMode) -> Unit,
    onPickCanvasImage: () -> Unit
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
                    color = if (selected) EnergyOrangeSoft else StudioCard,
                    border = BorderStroke(1.5.dp, if (selected) EnergyOrange else StudioBorder),
                    modifier = Modifier.testTag("ratio_chip_${ratio.name.lowercase()}")
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = ratio.label,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (selected) EnergyOrange else PureWhite,
                            fontWeight = FontWeight.Bold
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
        LazyRow(
            contentPadding = PaddingValues(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            item {
                Text("Mode Kanvas:", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
            }
            items(CanvasFitMode.entries) { mode ->
                val active = mode == selectedFitMode
                FilterChip(
                    selected = active,
                    onClick = {
                        if (mode == CanvasFitMode.CUSTOM_IMAGE) {
                            onPickCanvasImage()
                        } else {
                            onSelectFitMode(mode)
                        }
                    },
                    label = { Text(mode.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = EnergyOrangeSoft,
                        selectedLabelColor = EnergyOrange
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
                color = if (selected) EnergyOrangeSoft else StudioCard,
                border = BorderStroke(1.5.dp, if (selected) EnergyOrange else StudioBorder)
            ) {
                Text(
                    text = transition.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) EnergyOrange else TextPrimary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
        }
    }
}

@Composable
private fun CanvasBgInspector(
    selectedColorHex: Long,
    hasCustomImage: Boolean,
    onSelectColor: (Long) -> Unit,
    onPickCanvasImage: () -> Unit
) {
    val swatches = listOf(
        0xFF0B0B0DL,
        0xFF16161AL,
        0xFFFFFFFFL,
        0xFFFF6B00L,
        0xFF28190BL,
        0xFF1E2430L
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Latar Kanvas:", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
            swatches.forEach { hex ->
                val selected = hex == selectedColorHex && !hasCustomImage
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(hex))
                        .border(
                            width = if (selected) 2.5.dp else 1.dp,
                            color = if (selected) EnergyOrange else StudioBorder,
                            shape = CircleShape
                        )
                        .clickable { onSelectColor(hex) }
                )
            }
        }
        OutlinedButton(
            onClick = onPickCanvasImage,
            border = BorderStroke(1.dp, EnergyOrange),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Filled.Image, contentDescription = null, tint = EnergyOrange, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (hasCustomImage) "Ganti Gambar Latar Kustom" else "Pilih Gambar Latar Kustom",
                color = EnergyOrange,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
