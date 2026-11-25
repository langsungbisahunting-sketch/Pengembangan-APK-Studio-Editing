package com.example.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.example.ai.AiTaskResult
import com.example.ai.VeloCutAiService
import com.example.data.local.ExportEntity
import com.example.data.local.VeloCutDatabase
import com.example.data.repository.ProjectRepository
import com.example.media.AudioExtractionHelper
import com.example.media.MediaMetadataHelper
import com.example.media.VideoExportManager
import com.example.model.AspectRatioPreset
import com.example.model.AudioTrackItem
import com.example.model.CanvasFitMode
import com.example.model.ChromaKeyConfig
import com.example.model.ColorAdjustments
import com.example.model.ExportBitrate
import com.example.model.ExportFrameRate
import com.example.model.ExportResolution
import com.example.model.FilterPreset
import com.example.model.KeyframePoint
import com.example.model.PipOverlayItem
import com.example.model.ProjectTimelineData
import com.example.model.SpeedCurvePreset
import com.example.model.TextOverlayItem
import com.example.model.TransitionType
import com.example.model.VideoClip
import com.example.model.VideoEffectPreset
import com.example.model.VocalSeparationMode
import com.example.model.VoiceEffectPreset
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

enum class EditorToolTab(val label: String) {
    MAIN("Utama"),
    EDIT("Edit"),
    AUDIO("Audio"),
    TEXT("Teks & AI"),
    PIP("Overlay"),
    EFFECTS("Efek"),
    FILTER("Filter"),
    ADJUST("Sesuaikan"),
    CHROMA("Chroma"),
    KEYFRAME("Keyframe"),
    RATIO("Rasio"),
    TRANSITION("Transisi"),
    CANVAS("Kanvas")
}

sealed interface ExportState {
    data object Idle : ExportState
    data class Exporting(val progress: Float) : ExportState
    data class Success(val exportEntity: ExportEntity) : ExportState
    data class Error(val message: String) : ExportState
}

sealed interface AiTaskState {
    data object Idle : AiTaskState
    data class Loading(val label: String) : AiTaskState
    data class Error(val message: String) : AiTaskState
}

data class EditorUiState(
    val isLoading: Boolean = true,
    val projectId: Long = 0L,
    val projectTitle: String = "Proyek Baru",
    val timeline: ProjectTimelineData = ProjectTimelineData(),
    val currentPositionMs: Long = 0L,
    val isPlaying: Boolean = false,
    val selectedClipIndex: Int = 0,
    val selectedTextId: String? = null,
    val selectedPipId: String? = null,
    val activeToolTab: EditorToolTab = EditorToolTab.MAIN,
    val timelineZoom: Float = 1.0f,
    val isPreviewMuted: Boolean = false,
    val exportResolution: ExportResolution = ExportResolution.FHD_1080,
    val exportFrameRate: ExportFrameRate = ExportFrameRate.FPS_30,
    val exportBitrate: ExportBitrate = ExportBitrate.STANDARD,
    val exportState: ExportState = ExportState.Idle,
    val aiTaskState: AiTaskState = AiTaskState.Idle,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val statusBanner: String? = null
) {
    val selectedClip: VideoClip?
        get() = timeline.clips.getOrNull(selectedClipIndex)

    val selectedTextOverlay: TextOverlayItem?
        get() = timeline.textOverlays.firstOrNull { it.id == selectedTextId }

    val selectedPipOverlay: PipOverlayItem?
        get() = timeline.pipOverlays.firstOrNull { it.id == selectedPipId }
}

@UnstableApi
class EditorViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = VeloCutDatabase.getInstance(application).veloCutDao()
    private val repository = ProjectRepository(dao)
    private val exportManager = VideoExportManager(application)
    private val aiService = VeloCutAiService()

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    private val undoStack = ArrayDeque<ProjectTimelineData>()
    private val redoStack = ArrayDeque<ProjectTimelineData>()
    private var saveJob: Job? = null
    private var loadedProjectId: Long = -1L

    fun loadProject(projectId: Long) {
        if (loadedProjectId == projectId && !_uiState.value.isLoading) return
        loadedProjectId = projectId
        viewModelScope.launch {
            val entity = repository.getProject(projectId)
            if (entity != null) {
                val timeline = repository.decodeTimeline(entity.timelineJson)
                undoStack.clear()
                redoStack.clear()
                _uiState.update {
                    EditorUiState(
                        isLoading = false,
                        projectId = entity.id,
                        projectTitle = entity.title,
                        timeline = timeline,
                        selectedClipIndex = if (timeline.clips.isNotEmpty()) 0 else -1
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false, statusBanner = "Proyek tidak ditemukan") }
            }
        }
    }

    fun renameProject(newTitle: String) {
        val clean = newTitle.trim()
        if (clean.isEmpty()) return
        _uiState.update { it.copy(projectTitle = clean) }
        scheduleAutoSave()
    }

    fun setPlaying(playing: Boolean) {
        _uiState.update { it.copy(isPlaying = playing) }
    }

    fun togglePlayPause() {
        val state = _uiState.value
        if (state.timeline.clips.isEmpty()) return
        val total = state.timeline.totalDurationMs
        val resetPos = if (!state.isPlaying && state.currentPositionMs >= (total - 80L).coerceAtLeast(0L)) {
            0L
        } else state.currentPositionMs
        _uiState.update {
            it.copy(
                isPlaying = !it.isPlaying,
                currentPositionMs = resetPos
            )
        }
    }

    fun seekTo(positionMs: Long, fromUser: Boolean = true) {
        val total = _uiState.value.timeline.totalDurationMs.coerceAtLeast(0L)
        val clamped = positionMs.coerceIn(0L, total)
        val clipAtPos = _uiState.value.timeline.findClipAt(clamped)?.first ?: 0
        _uiState.update {
            it.copy(
                currentPositionMs = clamped,
                selectedClipIndex = if (fromUser) clipAtPos else it.selectedClipIndex
            )
        }
    }

    fun selectClip(index: Int) {
        val timeline = _uiState.value.timeline
        if (index !in timeline.clips.indices) return
        val clipStart = timeline.clipStartPositionMs(index)
        _uiState.update {
            it.copy(
                selectedClipIndex = index,
                selectedTextId = null,
                selectedPipId = null,
                currentPositionMs = clipStart,
                isPlaying = false
            )
        }
    }

    fun selectTextOverlay(id: String?) {
        _uiState.update {
            it.copy(
                selectedTextId = id,
                selectedPipId = if (id != null) null else it.selectedPipId,
                activeToolTab = if (id != null) EditorToolTab.TEXT else it.activeToolTab
            )
        }
    }

    fun selectPipOverlay(id: String?) {
        _uiState.update {
            it.copy(
                selectedPipId = id,
                selectedTextId = if (id != null) null else it.selectedTextId,
                activeToolTab = if (id != null) EditorToolTab.PIP else it.activeToolTab
            )
        }
    }

    fun setActiveToolTab(tab: EditorToolTab) {
        _uiState.update { it.copy(activeToolTab = tab) }
    }

    fun setTimelineZoom(zoom: Float) {
        _uiState.update { it.copy(timelineZoom = zoom.coerceIn(0.6f, 3.0f)) }
    }

    fun togglePreviewMute() {
        _uiState.update { it.copy(isPreviewMuted = !it.isPreviewMuted) }
    }

    fun clearBanner() {
        _uiState.update { it.copy(statusBanner = null) }
    }

    fun clearAiError() {
        _uiState.update { it.copy(aiTaskState = AiTaskState.Idle) }
    }

    fun undo() {
        val prev = undoStack.removeLastOrNull() ?: return
        redoStack.addLast(_uiState.value.timeline)
        _uiState.update {
            val safeIdx = it.selectedClipIndex.coerceIn(0, (prev.clips.size - 1).coerceAtLeast(0))
            it.copy(
                timeline = prev,
                selectedClipIndex = if (prev.clips.isEmpty()) -1 else safeIdx,
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty(),
                isPlaying = false
            )
        }
        scheduleAutoSave()
    }

    fun redo() {
        val next = redoStack.removeLastOrNull() ?: return
        undoStack.addLast(_uiState.value.timeline)
        _uiState.update {
            val safeIdx = it.selectedClipIndex.coerceIn(0, (next.clips.size - 1).coerceAtLeast(0))
            it.copy(
                timeline = next,
                selectedClipIndex = if (next.clips.isEmpty()) -1 else safeIdx,
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty(),
                isPlaying = false
            )
        }
        scheduleAutoSave()
    }

    private fun mutateTimeline(
        pausePlayback: Boolean = true,
        transform: (ProjectTimelineData) -> ProjectTimelineData
    ) {
        val current = _uiState.value.timeline
        val updated = transform(current)
        if (updated == current) return
        if (undoStack.size >= 25) undoStack.removeFirst()
        undoStack.addLast(current)
        redoStack.clear()

        val maxDur = updated.totalDurationMs.coerceAtLeast(0L)
        _uiState.update { state ->
            val safeClipIdx = when {
                updated.clips.isEmpty() -> -1
                state.selectedClipIndex !in updated.clips.indices -> updated.clips.lastIndex
                else -> state.selectedClipIndex
            }
            state.copy(
                timeline = updated,
                selectedClipIndex = safeClipIdx,
                currentPositionMs = state.currentPositionMs.coerceIn(0L, maxDur),
                isPlaying = if (pausePlayback) false else state.isPlaying,
                canUndo = undoStack.isNotEmpty(),
                canRedo = false
            )
        }
        scheduleAutoSave()
    }

    fun appendMediaUris(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            val context = getApplication<Application>()
            val newClips = uris.map { MediaMetadataHelper.probeMediaToClip(context, it) }
            mutateTimeline { timeline ->
                timeline.copy(clips = timeline.clips + newClips)
            }
            _uiState.update {
                it.copy(
                    selectedClipIndex = it.timeline.clips.lastIndex,
                    statusBanner = "${newClips.size} klip ditambahkan"
                )
            }
        }
    }

    fun splitClipAtPlayhead() {
        val state = _uiState.value
        val pos = state.currentPositionMs
        val found = state.timeline.findClipAt(pos) ?: return
        val (clipIndex, offsetEffectiveMs) = found
        val clip = state.timeline.clips[clipIndex]
        val offsetSourceMs = (offsetEffectiveMs * clip.effectiveSpeedAverage).toLong()
        val splitPointSourceMs = clip.trimStartMs + offsetSourceMs

        if (splitPointSourceMs - clip.trimStartMs < 350L || clip.trimEndMs - splitPointSourceMs < 350L) {
            _uiState.update { it.copy(statusBanner = "Geser playhead ke tengah klip untuk membagi") }
            return
        }

        val firstHalf = clip.copy(
            trimEndMs = splitPointSourceMs,
            transitionOut = TransitionType.NONE
        )
        val secondHalf = clip.copy(
            id = UUID.randomUUID().toString(),
            trimStartMs = splitPointSourceMs
        )

        mutateTimeline { timeline ->
            val mutable = timeline.clips.toMutableList()
            mutable[clipIndex] = firstHalf
            mutable.add(clipIndex + 1, secondHalf)
            timeline.copy(clips = mutable)
        }
        _uiState.update {
            it.copy(
                selectedClipIndex = clipIndex + 1,
                statusBanner = "Klip berhasil dibagi"
            )
        }
    }

    fun updateSelectedClipTrim(newStartMs: Long, newEndMs: Long) {
        val idx = _uiState.value.selectedClipIndex
        val clip = _uiState.value.selectedClip ?: return
        val minSpan = 400L
        val clampedStart = newStartMs.coerceIn(0L, (clip.sourceDurationMs - minSpan).coerceAtLeast(0L))
        val clampedEnd = newEndMs.coerceIn(
            (clampedStart + minSpan).coerceAtMost(clip.sourceDurationMs),
            clip.sourceDurationMs
        )
        mutateTimeline { timeline ->
            val list = timeline.clips.toMutableList()
            list[idx] = clip.copy(trimStartMs = clampedStart, trimEndMs = clampedEnd)
            timeline.copy(clips = list)
        }
    }

    fun updateSelectedClipSpeed(speed: Float) {
        val idx = _uiState.value.selectedClipIndex
        val clip = _uiState.value.selectedClip ?: return
        mutateTimeline { timeline ->
            val list = timeline.clips.toMutableList()
            list[idx] = clip.copy(
                speed = speed.coerceIn(0.1f, 10.0f),
                speedCurve = SpeedCurvePreset.NORMAL
            )
            timeline.copy(clips = list)
        }
    }

    fun updateSelectedClipSpeedCurve(curve: SpeedCurvePreset) {
        val idx = _uiState.value.selectedClipIndex
        val clip = _uiState.value.selectedClip ?: return
        mutateTimeline { timeline ->
            val list = timeline.clips.toMutableList()
            list[idx] = clip.copy(speedCurve = curve)
            timeline.copy(clips = list)
        }
    }

    fun updateSelectedClipVolume(volume: Float) {
        val idx = _uiState.value.selectedClipIndex
        val clip = _uiState.value.selectedClip ?: return
        mutateTimeline(pausePlayback = false) { timeline ->
            val list = timeline.clips.toMutableList()
            list[idx] = clip.copy(volume = volume.coerceIn(0f, 1f))
            timeline.copy(clips = list)
        }
    }

    fun updateSelectedClipVoiceEffect(effect: VoiceEffectPreset) {
        val idx = _uiState.value.selectedClipIndex
        val clip = _uiState.value.selectedClip ?: return
        mutateTimeline(pausePlayback = false) { timeline ->
            val list = timeline.clips.toMutableList()
            list[idx] = clip.copy(voiceEffect = effect)
            timeline.copy(clips = list)
        }
    }

    fun updateSelectedClipVocalMode(mode: VocalSeparationMode) {
        val idx = _uiState.value.selectedClipIndex
        val clip = _uiState.value.selectedClip ?: return
        mutateTimeline(pausePlayback = false) { timeline ->
            val list = timeline.clips.toMutableList()
            list[idx] = clip.copy(vocalMode = mode)
            timeline.copy(clips = list)
        }
    }

    fun toggleSelectedClipNoiseReduction() {
        val idx = _uiState.value.selectedClipIndex
        val clip = _uiState.value.selectedClip ?: return
        mutateTimeline(pausePlayback = false) { timeline ->
            val list = timeline.clips.toMutableList()
            list[idx] = clip.copy(noiseReduction = !clip.noiseReduction)
            timeline.copy(clips = list)
        }
    }

    fun extractAudioFromSelectedClip() {
        val idx = _uiState.value.selectedClipIndex
        val clip = _uiState.value.selectedClip ?: return
        if (clip.isImage) {
            _uiState.update { it.copy(statusBanner = "Klip gambar tidak memiliki trek audio") }
            return
        }
        viewModelScope.launch {
            val context = getApplication<Application>()
            val outFile = AudioExtractionHelper.extractAudioToPersistentM4a(
                context = context,
                sourceUri = Uri.parse(clip.uriString),
                baseTitle = clip.displayName.substringBeforeLast('.')
            )
            if (outFile == null) {
                _uiState.update { it.copy(statusBanner = "Video ini tidak memiliki trek audio") }
                return@launch
            }
            val audio = AudioTrackItem(
                uriString = Uri.fromFile(outFile).toString(),
                title = "Ekstrak: ${clip.displayName.substringBeforeLast('.')}",
                volume = clip.volume.coerceAtLeast(0.8f),
                durationMs = clip.sourceDurationMs
            )
            mutateTimeline { timeline ->
                val list = timeline.clips.toMutableList()
                list[idx] = clip.copy(volume = 0f)
                timeline.copy(clips = list, audioTrack = audio)
            }
            _uiState.update { it.copy(statusBanner = "Audio berhasil diekstrak ke trek A1") }
        }
    }

    fun rotateSelectedClip() {
        val idx = _uiState.value.selectedClipIndex
        val clip = _uiState.value.selectedClip ?: return
        mutateTimeline { timeline ->
            val list = timeline.clips.toMutableList()
            list[idx] = clip.copy(rotationDegrees = (clip.rotationDegrees + 90) % 360)
            timeline.copy(clips = list)
        }
    }

    fun toggleMirrorSelectedClip() {
        val idx = _uiState.value.selectedClipIndex
        val clip = _uiState.value.selectedClip ?: return
        mutateTimeline { timeline ->
            val list = timeline.clips.toMutableList()
            list[idx] = clip.copy(isMirrored = !clip.isMirrored)
            timeline.copy(clips = list)
        }
    }

    fun duplicateSelectedClip() {
        val idx = _uiState.value.selectedClipIndex
        val clip = _uiState.value.selectedClip ?: return
        mutateTimeline { timeline ->
            val list = timeline.clips.toMutableList()
            list.add(idx + 1, clip.copy(id = UUID.randomUUID().toString()))
            timeline.copy(clips = list)
        }
        _uiState.update { it.copy(selectedClipIndex = idx + 1, statusBanner = "Klip diduplikasi") }
    }

    fun moveSelectedClip(delta: Int) {
        val idx = _uiState.value.selectedClipIndex
        val clips = _uiState.value.timeline.clips
        val target = idx + delta
        if (idx !in clips.indices || target !in clips.indices) return
        mutateTimeline { timeline ->
            val list = timeline.clips.toMutableList()
            val item = list.removeAt(idx)
            list.add(target, item)
            timeline.copy(clips = list)
        }
        _uiState.update { it.copy(selectedClipIndex = target) }
    }

    fun deleteSelectedClip() {
        val idx = _uiState.value.selectedClipIndex
        val clips = _uiState.value.timeline.clips
        if (idx !in clips.indices) return
        if (clips.size <= 1) {
            _uiState.update { it.copy(statusBanner = "Minimal harus ada 1 klip di timeline") }
            return
        }
        mutateTimeline { timeline ->
            val list = timeline.clips.toMutableList()
            list.removeAt(idx)
            timeline.copy(clips = list)
        }
    }

    fun updateSelectedClipChromaKey(config: ChromaKeyConfig) {
        val idx = _uiState.value.selectedClipIndex
        val clip = _uiState.value.selectedClip ?: return
        mutateTimeline(pausePlayback = false) { timeline ->
            val list = timeline.clips.toMutableList()
            list[idx] = clip.copy(chromaKey = config)
            timeline.copy(clips = list)
        }
    }

    fun addOrUpdateClipKeyframe(
        scale: Float,
        rotationDeg: Float,
        opacity: Float,
        xOffset: Float,
        yOffset: Float
    ) {
        val state = _uiState.value
        val slot = state.timeline.findClipAt(state.currentPositionMs) ?: return
        val idx = slot.first
        val offsetMs = slot.second
        val clip = state.timeline.clips[idx]
        val existingIdx = clip.keyframes.indexOfFirst { kotlin.math.abs(it.timeOffsetMs - offsetMs) <= 200L }
        val point = KeyframePoint(
            timeOffsetMs = offsetMs,
            xOffset = xOffset,
            yOffset = yOffset,
            scale = scale,
            rotationDeg = rotationDeg,
            opacity = opacity
        )
        val updatedKeyframes = clip.keyframes.toMutableList().apply {
            if (existingIdx >= 0) set(existingIdx, point) else add(point)
        }.sortedBy { it.timeOffsetMs }

        mutateTimeline(pausePlayback = false) { timeline ->
            val list = timeline.clips.toMutableList()
            list[idx] = clip.copy(keyframes = updatedKeyframes)
            timeline.copy(clips = list)
        }
        _uiState.update { it.copy(statusBanner = "Keyframe ditambahkan pada ${offsetMs}ms") }
    }

    fun clearSelectedClipKeyframes() {
        val idx = _uiState.value.selectedClipIndex
        val clip = _uiState.value.selectedClip ?: return
        mutateTimeline(pausePlayback = false) { timeline ->
            val list = timeline.clips.toMutableList()
            list[idx] = clip.copy(keyframes = emptyList())
            timeline.copy(clips = list)
        }
    }

    fun setSelectedClipTransition(transition: TransitionType) {
        val idx = _uiState.value.selectedClipIndex
        val clip = _uiState.value.selectedClip ?: return
        mutateTimeline { timeline ->
            val list = timeline.clips.toMutableList()
            list[idx] = clip.copy(transitionOut = transition)
            timeline.copy(clips = list)
        }
    }

    fun setFilterPreset(preset: FilterPreset) {
        mutateTimeline(pausePlayback = false) { it.copy(filterPreset = preset) }
    }

    fun setVideoEffect(effect: VideoEffectPreset) {
        mutateTimeline(pausePlayback = false) { it.copy(videoEffect = effect) }
    }

    fun updateColorAdjustments(adjustments: ColorAdjustments) {
        mutateTimeline(pausePlayback = false) { it.copy(adjustments = adjustments) }
    }

    fun setAspectRatio(ratio: AspectRatioPreset) {
        mutateTimeline(pausePlayback = false) { it.copy(aspectRatio = ratio) }
    }

    fun setCanvasFitMode(mode: CanvasFitMode) {
        mutateTimeline(pausePlayback = false) { it.copy(canvasFitMode = mode) }
    }

    fun setCanvasBackground(colorHex: Long) {
        mutateTimeline(pausePlayback = false) { it.copy(canvasBgHex = colorHex) }
    }

    fun addTextOverlay(
        text: String,
        colorHex: Long = 0xFFFFFFFF,
        bgColorHex: Long = 0x88000000,
        styleId: String = "classic"
    ) {
        val clean = text.trim()
        if (clean.isEmpty()) return
        val total = _uiState.value.timeline.totalDurationMs.coerceAtLeast(3000L)
        val start = _uiState.value.currentPositionMs.coerceIn(0L, (total - 500L).coerceAtLeast(0L))
        val end = (start + 3000L).coerceAtMost(total)
        val item = TextOverlayItem(
            text = clean,
            startMs = start,
            endMs = end.coerceAtLeast(start + 500L),
            colorHex = colorHex,
            bgColorHex = bgColorHex,
            styleId = styleId
        )
        mutateTimeline(pausePlayback = false) { timeline ->
            timeline.copy(textOverlays = timeline.textOverlays + item)
        }
        _uiState.update { it.copy(selectedTextId = item.id) }
    }

    fun updateSelectedTextOverlay(transform: (TextOverlayItem) -> TextOverlayItem) {
        val id = _uiState.value.selectedTextId ?: return
        mutateTimeline(pausePlayback = false) { timeline ->
            timeline.copy(
                textOverlays = timeline.textOverlays.map {
                    if (it.id == id) transform(it) else it
                }
            )
        }
    }

    fun moveTextOverlayPosition(id: String, dxFraction: Float, dyFraction: Float) {
        _uiState.update { state ->
            val updated = state.timeline.copy(
                textOverlays = state.timeline.textOverlays.map {
                    if (it.id == id) {
                        it.copy(
                            xFraction = (it.xFraction + dxFraction).coerceIn(0.1f, 0.9f),
                            yFraction = (it.yFraction + dyFraction).coerceIn(0.1f, 0.9f)
                        )
                    } else it
                }
            )
            state.copy(timeline = updated, selectedTextId = id)
        }
        scheduleAutoSave()
    }

    fun deleteSelectedTextOverlay() {
        val id = _uiState.value.selectedTextId ?: return
        mutateTimeline(pausePlayback = false) { timeline ->
            timeline.copy(textOverlays = timeline.textOverlays.filterNot { it.id == id })
        }
        _uiState.update { it.copy(selectedTextId = null) }
    }

    fun generateAutoCaptions(languageHint: String = "Indonesian or English") {
        val state = _uiState.value
        val clip = state.selectedClip ?: state.timeline.clips.firstOrNull() ?: return
        val clipIdx = if (state.selectedClip != null) state.selectedClipIndex else 0
        val clipStart = state.timeline.clipStartPositionMs(clipIdx)

        viewModelScope.launch {
            _uiState.update { it.copy(aiTaskState = AiTaskState.Loading("AI sedang membuat Auto-Caption...")) }
            val result = aiService.generateAutoCaptions(
                context = getApplication(),
                mediaUri = Uri.parse(clip.uriString),
                clipStartOffsetMs = clipStart,
                clipDurationMs = clip.effectiveDurationMs,
                languageHint = languageHint
            )
            when (result) {
                is AiTaskResult.Success -> {
                    val generated = result.data.map { seg ->
                        TextOverlayItem(
                            text = seg.text,
                            startMs = seg.startMs,
                            endMs = seg.endMs,
                            xFraction = 0.5f,
                            yFraction = 0.80f,
                            fontSizeSp = 20f,
                            colorHex = 0xFFFFFFFF,
                            bgColorHex = 0xB3000000,
                            styleId = "classic"
                        )
                    }
                    mutateTimeline(pausePlayback = false) { timeline ->
                        timeline.copy(textOverlays = timeline.textOverlays + generated)
                    }
                    _uiState.update {
                        it.copy(
                            aiTaskState = AiTaskState.Idle,
                            statusBanner = "${generated.size} subtitle AI ditambahkan ke timeline"
                        )
                    }
                }
                is AiTaskResult.Error -> {
                    _uiState.update { it.copy(aiTaskState = AiTaskState.Error(result.message)) }
                }
            }
        }
    }

    fun translateAllCaptions(targetLanguage: String) {
        val currentOverlays = _uiState.value.timeline.textOverlays
        if (currentOverlays.isEmpty()) {
            _uiState.update { it.copy(statusBanner = "Tambahkan atau buat Auto-Caption terlebih dahulu") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(aiTaskState = AiTaskState.Loading("Menerjemahkan ke $targetLanguage...")) }
            val result = aiService.translateCaptions(
                items = currentOverlays.map { it.id to it.text },
                targetLanguage = targetLanguage
            )
            when (result) {
                is AiTaskResult.Success -> {
                    val map = result.data
                    mutateTimeline(pausePlayback = false) { timeline ->
                        timeline.copy(
                            textOverlays = timeline.textOverlays.map { item ->
                                val translated = map[item.id]
                                if (translated != null) item.copy(text = translated) else item
                            }
                        )
                    }
                    _uiState.update {
                        it.copy(
                            aiTaskState = AiTaskState.Idle,
                            statusBanner = "Subtitle diterjemahkan ke $targetLanguage"
                        )
                    }
                }
                is AiTaskResult.Error -> {
                    _uiState.update { it.copy(aiTaskState = AiTaskState.Error(result.message)) }
                }
            }
        }
    }

    fun addPipOverlayFromUri(uri: Uri) {
        viewModelScope.launch {
            val probed = MediaMetadataHelper.probeMediaToClip(getApplication(), uri)
            val total = _uiState.value.timeline.totalDurationMs.coerceAtLeast(4000L)
            val start = _uiState.value.currentPositionMs.coerceIn(0L, (total - 500L).coerceAtLeast(0L))
            val end = (start + probed.effectiveDurationMs).coerceAtMost(total).coerceAtLeast(start + 1000L)
            val pip = PipOverlayItem(
                uriString = probed.uriString,
                displayName = probed.displayName,
                isImage = probed.isImage,
                startMs = start,
                endMs = end
            )
            mutateTimeline(pausePlayback = false) { timeline ->
                timeline.copy(pipOverlays = timeline.pipOverlays + pip)
            }
            _uiState.update {
                it.copy(
                    selectedPipId = pip.id,
                    statusBanner = "Overlay PiP '${pip.displayName}' ditambahkan"
                )
            }
        }
    }

    fun movePipOverlay(id: String, dxFraction: Float, dyFraction: Float) {
        _uiState.update { state ->
            val updated = state.timeline.copy(
                pipOverlays = state.timeline.pipOverlays.map {
                    if (it.id == id) {
                        it.copy(
                            xFraction = (it.xFraction + dxFraction).coerceIn(0.1f, 0.9f),
                            yFraction = (it.yFraction + dyFraction).coerceIn(0.1f, 0.9f)
                        )
                    } else it
                }
            )
            state.copy(timeline = updated, selectedPipId = id)
        }
        scheduleAutoSave()
    }

    fun updateSelectedPipOverlay(transform: (PipOverlayItem) -> PipOverlayItem) {
        val id = _uiState.value.selectedPipId ?: return
        mutateTimeline(pausePlayback = false) { timeline ->
            timeline.copy(
                pipOverlays = timeline.pipOverlays.map {
                    if (it.id == id) transform(it) else it
                }
            )
        }
    }

    fun deleteSelectedPipOverlay() {
        val id = _uiState.value.selectedPipId ?: return
        mutateTimeline(pausePlayback = false) { timeline ->
            timeline.copy(pipOverlays = timeline.pipOverlays.filterNot { it.id == id })
        }
        _uiState.update { it.copy(selectedPipId = null) }
    }

    fun setAudioTrackFromUri(uri: Uri) {
        viewModelScope.launch {
            val probed = MediaMetadataHelper.probeAudio(getApplication(), uri)
            val audio = AudioTrackItem(
                uriString = probed.uriString,
                title = probed.title,
                durationMs = probed.durationMs
            )
            mutateTimeline(pausePlayback = false) { it.copy(audioTrack = audio) }
            _uiState.update { it.copy(statusBanner = "Musik '${probed.title}' ditambahkan") }
        }
    }

    fun updateAudioVolume(volume: Float) {
        val existing = _uiState.value.timeline.audioTrack ?: return
        mutateTimeline(pausePlayback = false) {
            it.copy(audioTrack = existing.copy(volume = volume.coerceIn(0f, 1f)))
        }
    }

    fun updateAudioVoiceEffect(effect: VoiceEffectPreset) {
        val existing = _uiState.value.timeline.audioTrack ?: return
        mutateTimeline(pausePlayback = false) {
            it.copy(audioTrack = existing.copy(voiceEffect = effect))
        }
    }

    fun updateAudioVocalMode(mode: VocalSeparationMode) {
        val existing = _uiState.value.timeline.audioTrack ?: return
        mutateTimeline(pausePlayback = false) {
            it.copy(audioTrack = existing.copy(vocalMode = mode))
        }
    }

    fun toggleAudioNoiseReduction() {
        val existing = _uiState.value.timeline.audioTrack ?: return
        mutateTimeline(pausePlayback = false) {
            it.copy(audioTrack = existing.copy(noiseReduction = !existing.noiseReduction))
        }
    }

    fun toggleAudioFade(fadeIn: Boolean? = null, fadeOut: Boolean? = null) {
        val existing = _uiState.value.timeline.audioTrack ?: return
        mutateTimeline(pausePlayback = false) {
            it.copy(
                audioTrack = existing.copy(
                    fadeIn = fadeIn ?: existing.fadeIn,
                    fadeOut = fadeOut ?: existing.fadeOut
                )
            )
        }
    }

    fun removeAudioTrack() {
        mutateTimeline(pausePlayback = false) { it.copy(audioTrack = null) }
    }

    fun setExportResolution(resolution: ExportResolution) {
        _uiState.update { it.copy(exportResolution = resolution) }
    }

    fun setExportFrameRate(frameRate: ExportFrameRate) {
        _uiState.update { it.copy(exportFrameRate = frameRate) }
    }

    fun setExportBitrate(bitrate: ExportBitrate) {
        _uiState.update { it.copy(exportBitrate = bitrate) }
    }

    fun startExport() {
        val state = _uiState.value
        if (state.exportState is ExportState.Exporting) return
        setPlaying(false)
        _uiState.update { it.copy(exportState = ExportState.Exporting(0.02f)) }

        exportManager.startExport(
            projectTitle = state.projectTitle,
            timeline = state.timeline,
            resolution = state.exportResolution,
            frameRate = state.exportFrameRate,
            bitrate = state.exportBitrate,
            onProgress = { progress ->
                _uiState.update { it.copy(exportState = ExportState.Exporting(progress)) }
            },
            onSuccess = { outFile, fileSize ->
                viewModelScope.launch {
                    val entity = ExportEntity(
                        title = state.projectTitle,
                        filePath = outFile.absolutePath,
                        resolutionLabel = "${state.exportResolution.badge}·${state.exportFrameRate.fps}FPS",
                        durationMs = state.timeline.totalDurationMs,
                        fileSizeBytes = fileSize
                    )
                    val newId = repository.recordExport(entity)
                    _uiState.update {
                        it.copy(exportState = ExportState.Success(entity.copy(id = newId)))
                    }
                }
            },
            onError = { message ->
                _uiState.update { it.copy(exportState = ExportState.Error(message)) }
            }
        )
    }

    fun cancelExport() {
        exportManager.cancelExport()
        _uiState.update { it.copy(exportState = ExportState.Idle) }
    }

    fun resetExportState() {
        _uiState.update { it.copy(exportState = ExportState.Idle) }
    }

    fun saveExportToGallery(export: ExportEntity) {
        viewModelScope.launch {
            val uri = VideoExportManager.saveToGalleryMediaStore(
                getApplication(),
                File(export.filePath),
                export.title
            )
            if (uri != null) {
                repository.updateExportMediaStoreUri(export.id, uri.toString())
                _uiState.update { it.copy(statusBanner = "Video disimpan ke Galeri (Movies/VeloCut)") }
            } else {
                _uiState.update { it.copy(statusBanner = "Gagal menyimpan video ke Galeri") }
            }
        }
    }

    fun shareExportFile(export: ExportEntity) {
        val context = getApplication<Application>()
        val file = File(export.filePath)
        if (!file.exists()) return
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "video/mp4"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            Intent.createChooser(intent, "Bagikan Video").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }

    private fun scheduleAutoSave() {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(300L)
            val state = _uiState.value
            if (state.projectId > 0L) {
                repository.saveProject(state.projectId, state.projectTitle, state.timeline)
            }
        }
    }

    fun saveNow() {
        val state = _uiState.value
        if (state.projectId > 0L) {
            viewModelScope.launch {
                repository.saveProject(state.projectId, state.projectTitle, state.timeline)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        exportManager.cancelExport()
    }
}
