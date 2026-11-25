package com.example.ui.editor

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.NeonCoral
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCard
import com.example.ui.theme.StudioObsidian
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.AiTaskState
import com.example.viewmodel.EditorToolTab
import com.example.viewmodel.EditorViewModel
import com.example.viewmodel.ExportState

@OptIn(UnstableApi::class)
@Composable
fun EditorScreen(
    projectId: Long,
    viewModel: EditorViewModel,
    onNavigateBack: () -> Unit
) {
    LaunchedEffect(projectId) {
        viewModel.loadProject(projectId)
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showRenameDialog by remember { mutableStateOf(false) }
    var showExportSheet by remember { mutableStateOf(false) }
    var isFullscreenPreview by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.statusBanner) {
        uiState.statusBanner?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearBanner()
        }
    }

    BackHandler {
        when {
            isFullscreenPreview -> isFullscreenPreview = false
            uiState.activeToolTab != EditorToolTab.MAIN -> viewModel.setActiveToolTab(EditorToolTab.MAIN)
            else -> {
                viewModel.saveNow()
                onNavigateBack()
            }
        }
    }

    val appendMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10)
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.appendMediaUris(uris)
        }
    }

    val pipPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { viewModel.addPipOverlayFromUri(it) }
    }

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.setAudioTrackFromUri(it) }
    }

    Scaffold(
        containerColor = StudioObsidian,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = ElectricCyan)
            }
            return@Scaffold
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(StudioObsidian)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (!isFullscreenPreview) {
                    EditorTopBar(
                        title = uiState.projectTitle,
                        canUndo = uiState.canUndo,
                        canRedo = uiState.canRedo,
                        resolution = uiState.exportResolution,
                        onBack = {
                            viewModel.saveNow()
                            onNavigateBack()
                        },
                        onTitleClick = { showRenameDialog = true },
                        onUndo = viewModel::undo,
                        onRedo = viewModel::redo,
                        onResolutionClick = { showExportSheet = true },
                        onExportClick = { showExportSheet = true }
                    )
                }

                EditorPreviewStage(
                    timeline = uiState.timeline,
                    currentPositionMs = uiState.currentPositionMs,
                    isPlaying = uiState.isPlaying,
                    isPreviewMuted = uiState.isPreviewMuted,
                    selectedTextId = uiState.selectedTextId,
                    selectedPipId = uiState.selectedPipId,
                    isFullscreen = isFullscreenPreview,
                    onTogglePlayPause = viewModel::togglePlayPause,
                    onSeekTo = { viewModel.seekTo(it, fromUser = true) },
                    onPlaybackTick = { viewModel.seekTo(it, fromUser = false) },
                    onPlaybackEnded = { viewModel.setPlaying(false) },
                    onToggleMute = viewModel::togglePreviewMute,
                    onToggleFullscreen = { isFullscreenPreview = !isFullscreenPreview },
                    onSelectText = viewModel::selectTextOverlay,
                    onMoveText = viewModel::moveTextOverlayPosition,
                    onSelectPip = viewModel::selectPipOverlay,
                    onMovePip = viewModel::movePipOverlay,
                    modifier = Modifier.weight(if (isFullscreenPreview) 1f else 0.48f)
                )

                if (!isFullscreenPreview) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(30.dp)
                            .background(StudioSurface)
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Timeline · ${uiState.timeline.clips.size} klip · ${uiState.timeline.aspectRatio.label}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { viewModel.setTimelineZoom(uiState.timelineZoom - 0.3f) },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(Icons.Filled.ZoomOut, "Perkecil Timeline", tint = TextSecondary, modifier = Modifier.size(16.dp))
                            }
                            Text(
                                text = "${String.format(java.util.Locale.US, "%.1fx", uiState.timelineZoom)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = ElectricCyan
                            )
                            IconButton(
                                onClick = { viewModel.setTimelineZoom(uiState.timelineZoom + 0.3f) },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(Icons.Filled.ZoomIn, "Perbesar Timeline", tint = TextSecondary, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    TimelinePanel(
                        timeline = uiState.timeline,
                        currentPositionMs = uiState.currentPositionMs,
                        isPlaying = uiState.isPlaying,
                        selectedClipIndex = uiState.selectedClipIndex,
                        selectedTextId = uiState.selectedTextId,
                        selectedPipId = uiState.selectedPipId,
                        timelineZoom = uiState.timelineZoom,
                        onSeekTo = { viewModel.seekTo(it, fromUser = true) },
                        onSelectClip = viewModel::selectClip,
                        onTrimClip = viewModel::updateSelectedClipTrim,
                        onTapTransition = { index ->
                            viewModel.selectClip(index)
                            viewModel.setActiveToolTab(EditorToolTab.TRANSITION)
                        },
                        onSelectText = { id -> viewModel.selectTextOverlay(id) },
                        onSelectPip = { id -> viewModel.selectPipOverlay(id) },
                        onAddMediaClick = {
                            appendMediaLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                            )
                        },
                        onAddAudioClick = {
                            if (uiState.timeline.audioTrack == null) {
                                viewModel.setActiveToolTab(EditorToolTab.AUDIO)
                            } else {
                                viewModel.setActiveToolTab(EditorToolTab.AUDIO)
                            }
                        },
                        modifier = Modifier
                            .weight(0.36f)
                            .testTag("editor_timeline_panel")
                    )

                    EditorSubPanelContainer(
                        uiState = uiState,
                        onSplitClip = viewModel::splitClipAtPlayhead,
                        onTrimClip = viewModel::updateSelectedClipTrim,
                        onSpeedChange = viewModel::updateSelectedClipSpeed,
                        onSpeedCurveChange = viewModel::updateSelectedClipSpeedCurve,
                        onClipVolumeChange = viewModel::updateSelectedClipVolume,
                        onExtractAudio = viewModel::extractAudioFromSelectedClip,
                        onRotateClip = viewModel::rotateSelectedClip,
                        onMirrorClip = viewModel::toggleMirrorSelectedClip,
                        onDuplicateClip = viewModel::duplicateSelectedClip,
                        onMoveClip = viewModel::moveSelectedClip,
                        onDeleteClip = viewModel::deleteSelectedClip,
                        onPickAudio = { audioPickerLauncher.launch(arrayOf("audio/*")) },
                        onAudioVolumeChange = viewModel::updateAudioVolume,
                        onAudioVoiceEffectChange = viewModel::updateAudioVoiceEffect,
                        onAudioVocalModeChange = viewModel::updateAudioVocalMode,
                        onToggleAudioNoiseReduction = viewModel::toggleAudioNoiseReduction,
                        onClipVoiceEffectChange = viewModel::updateSelectedClipVoiceEffect,
                        onClipVocalModeChange = viewModel::updateSelectedClipVocalMode,
                        onToggleClipNoiseReduction = viewModel::toggleSelectedClipNoiseReduction,
                        onToggleAudioFade = viewModel::toggleAudioFade,
                        onRemoveAudio = viewModel::removeAudioTrack,
                        onAddText = { txt, fg, bg, styleId ->
                            viewModel.addTextOverlay(txt, fg, bg, styleId)
                        },
                        onUpdateSelectedText = viewModel::updateSelectedTextOverlay,
                        onDeleteSelectedText = viewModel::deleteSelectedTextOverlay,
                        onGenerateAutoCaptions = viewModel::generateAutoCaptions,
                        onTranslateCaptions = viewModel::translateAllCaptions,
                        onPickPipOverlay = {
                            pipPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                            )
                        },
                        onUpdateSelectedPip = viewModel::updateSelectedPipOverlay,
                        onDeleteSelectedPip = viewModel::deleteSelectedPipOverlay,
                        onSelectVideoEffect = viewModel::setVideoEffect,
                        onSelectFilter = viewModel::setFilterPreset,
                        onUpdateAdjustments = viewModel::updateColorAdjustments,
                        onUpdateChromaKey = viewModel::updateSelectedClipChromaKey,
                        onAddKeyframe = viewModel::addOrUpdateClipKeyframe,
                        onClearKeyframes = viewModel::clearSelectedClipKeyframes,
                        onSelectRatio = viewModel::setAspectRatio,
                        onSelectFitMode = viewModel::setCanvasFitMode,
                        onSelectTransition = viewModel::setSelectedClipTransition,
                        onSelectCanvasColor = viewModel::setCanvasBackground
                    )

                    EditorBottomToolDock(
                        activeTab = uiState.activeToolTab,
                        onSelectTab = viewModel::setActiveToolTab,
                        onBackToMain = { viewModel.setActiveToolTab(EditorToolTab.MAIN) }
                    )
                }
            }

            // Transparent AI Loading / Error Overlay
            when (val aiState = uiState.aiTaskState) {
                is AiTaskState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.65f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = StudioCard,
                            border = BorderStroke(1.dp, ElectricCyan)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(color = ElectricCyan)
                                Text(aiState.label, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            }
                        }
                    }
                }
                is AiTaskState.Error -> {
                    AlertDialog(
                        onDismissRequest = viewModel::clearAiError,
                        containerColor = StudioCard,
                        title = { Text("Proses AI Gagal", color = NeonCoral) },
                        text = { Text(aiState.message, color = TextSecondary) },
                        confirmButton = {
                            TextButton(onClick = viewModel::clearAiError) {
                                Text("Tutup", color = ElectricCyan)
                            }
                        }
                    )
                }
                AiTaskState.Idle -> {}
            }
        }
    }

    if (showRenameDialog) {
        var titleDraft by remember(uiState.projectTitle) { mutableStateOf(uiState.projectTitle) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            containerColor = StudioCard,
            title = { Text("Ubah Nama Proyek", color = TextPrimary) },
            text = {
                OutlinedTextField(
                    value = titleDraft,
                    onValueChange = { titleDraft = it },
                    singleLine = true,
                    label = { Text("Judul") }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.renameProject(titleDraft)
                        showRenameDialog = false
                    }
                ) {
                    Text("Simpan", color = ElectricCyan)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Batal", color = TextSecondary)
                }
            }
        )
    }

    if (showExportSheet) {
        ExportBottomSheet(
            resolution = uiState.exportResolution,
            frameRate = uiState.exportFrameRate,
            bitrate = uiState.exportBitrate,
            exportState = uiState.exportState,
            onSelectResolution = viewModel::setExportResolution,
            onSelectFrameRate = viewModel::setExportFrameRate,
            onSelectBitrate = viewModel::setExportBitrate,
            onStartExport = viewModel::startExport,
            onCancelExport = viewModel::cancelExport,
            onSaveToGallery = viewModel::saveExportToGallery,
            onShare = viewModel::shareExportFile,
            onDismiss = {
                if (uiState.exportState !is ExportState.Exporting) {
                    viewModel.resetExportState()
                    showExportSheet = false
                }
            }
        )
    }
}
