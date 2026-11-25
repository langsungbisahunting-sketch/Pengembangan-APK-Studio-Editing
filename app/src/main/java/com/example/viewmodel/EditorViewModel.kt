package com.example.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.example.ai.AiTaskResult
import com.example.ai.KomikNarasiAiService
import com.example.data.local.ExportEntity
import com.example.data.local.VeloCutDatabase
import com.example.data.repository.ProjectRepository
import com.example.media.AudioExtractionHelper
import com.example.media.MediaMetadataHelper
import com.example.media.TtsNarrationHelper
import com.example.media.VideoExportManager
import com.example.model.AspectRatioPreset
import com.example.model.AudioTrackItem
import com.example.model.AudioTrackRole
import com.example.model.CanvasFitMode
import com.example.model.ChromaKeyConfig
import com.example.model.ColorAdjustments
import com.example.model.ExportBitrate
import com.example.model.ExportFrameRate
import com.example.model.ExportResolution
import com.example.model.FilterPreset
import com.example.model.KeyframePoint
import com.example.model.PanZoomPreset
import com.example.model.PipOverlayItem
import com.example.model.ProjectTimelineData
import com.example.model.SpeedCurvePreset
import com.example.model.TextOverlayItem
import com.example.model.TransitionType
import com.example.model.TtsVoiceStyle
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
import kotlin.math.abs

enum class EditorToolTab(val label: String) {
    MAIN("Utama"),
    EDIT("Edit"),
    AUDIO("Audio 2-Track"),
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
    val projectTitle: String = "Komik Narasi Proyek",
    val timeline: ProjectTimelineData = ProjectTimelineData(),
    val currentPositionMs: Long = 0L,
    val isPlaying: Boolean = false,
    val selectedClipIndex: Int = 0,
    val selectedTextId: String? = null,
    val selectedPipId: String? = null,
    val selectedAudioRole: AudioTrackRole = AudioTrackRole.MUSIC,
    val activeToolTab: EditorToolTab = EditorToolTab.MAIN,
    val timelineZoom: Float = 1.0f,
    val isPreviewMuted: Boolean = false,
    val exportResolution: ExportResolution = ExportResolution.HD_720,
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

    val activeAudioTrack: AudioTrackItem?
        get() = when (selectedAudioRole) {
            AudioTrackRole.VOCAL -> timeline.vocalTrack
            AudioTrackRole.MUSIC -> timeline.musicTrack
        }
}

@UnstableApi
class EditorViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = VeloCutDatabase.getInstance(application).veloCutDao()
    private val repository = ProjectRepository(dao)
    private val exportManager = VideoExportManager(application)
    private val aiService = KomikNarasiAiService()

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
                        selectedClipIndex = if (timeline.clips.isNotEmpty()) 0 else -1,
                        selectedAudioRole = if (timeline.vocalTrack != null) AudioTrackRole.VOCAL else AudioTrackRole.MUSIC
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
        val timeline = _uiState.value.timeline
        val total = timeline.totalDurationMs.coerceAtLeast(0L)
        var clamped = positionMs.coerceIn(0L, total)
        if (fromUser && timeline.snapToGrid && !_uiState.value.isPlaying) {
            val grid = ((clamped + 250L) / 500L) * 500L
            if (abs(grid - clamped) <= 110L) {
                clamped = grid.coerceIn(0L, total)
            }
        }
        val clipAtPos = timeline.findClipAt(clamped)?.first ?: 0
        _uiState.update {
            it.copy(
                currentPositionMs = clamped,
                selectedClipIndex = if (fromUser) clipAtPos else it.selectedClipIndex
            )
        }
    }

    fun toggleSnapToGrid() {
        mutateTimeline(pausePlayback = false) { it.copy(snapToGrid = !it.snapToGrid) }
        val enabled = _uiState.value.timeline.snapToGrid
        _uiState.update {
            it.copy(statusBanner = if (enabled) "Snap-to-Grid aktif (0.5d)" else "Snap-to-Grid dimatikan")
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

    fun selectAudioRole(role: AudioTrackRole) {
        _uiState.update {
            it.copy(
                selectedAudioRole = role,
                activeToolTab = EditorToolTab.AUDIO
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
        if (undoStack.size >= 30) undoStack.removeFirst()
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

    fun toggleSelectedClipSmoothSlowMo() {
        val idx = _uiState.value.selectedClipIndex
        val clip = _uiState.value.selectedClip ?: return
        mutateTimeline(pausePlayback = false) { timeline ->
            val list = timeline.clips.toMutableList()
            list[idx] = clip.copy(smoothSlowMo = !clip.smoothSlowMo)
            timeline.copy(clips = list)
        }
    }

    fun updateSelectedClipCrop(cropScale: Float) {
        val idx = _uiState.value.selectedClipIndex
        val clip = _uiState.value.selectedClip ?: return
        mutateTimeline(pausePlayback = false) { timeline ->
            val list = timeline.clips.toMutableList()
            list[idx] = clip.copy(cropScale = cropScale.coerceIn(1.0f, 2.5f))
            timeline.copy(clips = list)
        }
    }

    fun setSelectedClipPanZoom(panZoom: PanZoomPreset) {
        val idx = _uiState.value.selectedClipIndex
        val clip = _uiState.value.selectedClip ?: return
        mutateTimeline(pausePlayback = false) { timeline ->
            val list = timeline.clips.toMutableList()
            list[idx] = clip.copy(panZoom = panZoom)
            timeline.copy(clips = list)
        }
    }

    // Touch-first direct finger transform on Canvas for selected Video/Image clip
    fun transformSelectedClip(
        dxFraction: Float,
        dyFraction: Float,
        scaleFactor: Float,
        rotationDelta: Float
    ) {
        val idx = _uiState.value.selectedClipIndex
        val clip = _uiState.value.selectedClip ?: return
        _uiState.update { state ->
            val list = state.timeline.clips.toMutableList()
            list[idx] = clip.copy(
                posX = (clip.posX + dxFraction).coerceIn(-0.6f, 0.6f),
                posY = (clip.posY + dyFraction).coerceIn(-0.6f, 0.6f),
                userScale = (clip.userScale * scaleFactor).coerceIn(0.4f, 3.2f),
                freeRotationDeg = ((clip.freeRotationDeg + rotationDelta) % 360f)
            )
            state.copy(timeline = state.timeline.copy(clips = list))
        }
        scheduleAutoSave()
    }

    fun resetSelectedClipTransform() {
        val idx = _uiState.value.selectedClipIndex
        val clip = _uiState.value.selectedClip ?: return
        mutateTimeline(pausePlayback = false) { timeline ->
            val list = timeline.clips.toMutableList()
            list[idx] = clip.copy(posX = 0f, posY = 0f, userScale = 1.0f, freeRotationDeg = 0f)
            timeline.copy(clips = list)
        }
    }

    fun toggleReverseSelectedClip() {
        val idx = _uiState.value.selectedClipIndex
        val clip = _uiState.value.selectedClip ?: return
        mutateTimeline { timeline ->
            val list = timeline.clips.toMutableList()
            list[idx] = clip.copy(isReversed = !clip.isReversed)
            timeline.copy(clips = list)
        }
        val rev = _uiState.value.selectedClip?.isReversed == true
        _uiState.update {
            it.copy(statusBanner = if (rev) "Mode Reverse klip diaktifkan" else "Mode Reverse dimatikan")
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
            val dur = clip.sourceDurationMs.coerceAtLeast(1000L)
            val audio = AudioTrackItem(
                uriString = Uri.fromFile(outFile).toString(),
                title = "Ekstrak: ${clip.displayName.substringBeforeLast('.')}",
                role = AudioTrackRole.VOCAL,
                volume = clip.volume.coerceAtLeast(0.85f),
                durationMs = dur,
                trimStartMs = 0L,
                trimEndMs = dur
            )
            mutateTimeline { timeline ->
                val list = timeline.clips.toMutableList()
                list[idx] = clip.copy(volume = 0f)
                timeline.copy(clips = list, vocalTrack = audio)
            }
            _uiState.update {
                it.copy(
                    selectedAudioRole = AudioTrackRole.VOCAL,
                    statusBanner = "Audio diekstrak ke Track Vokal"
                )
            }
        }
    }

    /**
     * Pisah Suara & Musik Jadi Dua Track Terpisah:
     * Extracts from selected video clip (or splits active audio) into two independent tracks:
     * 1) Track Suara/Vokal/Narasi
     * 2) Track Musik Latar/Instrumen
     */
    fun splitAudioIntoVocalAndMusicTracks() {
        val state = _uiState.value
        val clip = state.selectedClip
        val sourceUri = when {
            clip != null && !clip.isImage -> Uri.parse(clip.uriString)
            state.timeline.musicTrack != null -> Uri.parse(state.timeline.musicTrack.uriString)
            state.timeline.vocalTrack != null -> Uri.parse(state.timeline.vocalTrack.uriString)
            else -> null
        }
        if (sourceUri == null) {
            _uiState.update { it.copy(statusBanner = "Pilih klip video atau tambah audio terlebih dahulu") }
            return
        }
        val title = clip?.displayName?.substringBeforeLast('.')
            ?: state.timeline.musicTrack?.title
            ?: "Audio"
        val dur = clip?.sourceDurationMs
            ?: state.timeline.musicTrack?.durationMs
            ?: state.timeline.totalDurationMs.coerceAtLeast(3000L)

        viewModelScope.launch {
            _uiState.update { it.copy(aiTaskState = AiTaskState.Loading("Memisahkan Suara & Musik jadi 2 Track...")) }
            val result = AudioExtractionHelper.splitAudioIntoVocalAndMusic(
                context = getApplication(),
                sourceUri = sourceUri,
                baseTitle = title,
                durationMs = dur
            )
            if (result == null) {
                _uiState.update {
                    it.copy(
                        aiTaskState = AiTaskState.Idle,
                        statusBanner = "Video/klip ini tidak memiliki jalur audio untuk dipisahkan"
                    )
                }
                return@launch
            }
            mutateTimeline { timeline ->
                val updatedClips = if (clip != null && !clip.isImage && state.selectedClipIndex in timeline.clips.indices) {
                    timeline.clips.toMutableList().also {
                        it[state.selectedClipIndex] = clip.copy(volume = 0f)
                    }
                } else timeline.clips
                timeline.copy(
                    clips = updatedClips,
                    vocalTrack = result.vocalTrack,
                    musicTrack = result.musicTrack
                )
            }
            _uiState.update {
                it.copy(
                    aiTaskState = AiTaskState.Idle,
                    selectedAudioRole = AudioTrackRole.VOCAL,
                    statusBanner = "Berhasil! Track Vokal & Track Musik kini terpisah"
                )
            }
        }
    }

    fun synthesizeTtsNarration(scriptText: String, style: TtsVoiceStyle) {
        val clean = scriptText.trim()
        if (clean.isEmpty()) {
            _uiState.update { it.copy(statusBanner = "Tulis naskah cerita komik terlebih dahulu") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(aiTaskState = AiTaskState.Loading("Membuat suara narasi Komik Narasi...")) }
            val result = TtsNarrationHelper.synthesizeNarrationToAudio(
                context = getApplication(),
                scriptText = clean,
                style = style
            )
            if (result == null) {
                _uiState.update {
                    it.copy(
                        aiTaskState = AiTaskState.Error(
                            "Mesin Text-to-Speech perangkat tidak tersedia atau gagal menghasilkan suara."
                        )
                    )
                }
                return@launch
            }
            val audio = AudioTrackItem(
                uriString = result.uriString,
                title = result.title,
                role = AudioTrackRole.VOCAL,
                volume = 0.95f,
                durationMs = result.durationMs,
                trimStartMs = 0L,
                trimEndMs = result.durationMs
            )
            mutateTimeline(pausePlayback = false) { it.copy(vocalTrack = audio) }
            _uiState.update {
                it.copy(
                    aiTaskState = AiTaskState.Idle,
                    selectedAudioRole = AudioTrackRole.VOCAL,
                    statusBanner = "${result.title} ditambahkan ke Track Suara/Vokal!"
                )
            }
        }
    }

    fun polishNarrationScriptWithAi(draft: String, onPolished: (String) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(aiTaskState = AiTaskState.Loading("KN AI sedang merangkai naskah narasi...")) }
            when (val res = aiService.polishComicNarrationScript(draft)) {
                is AiTaskResult.Success -> {
                    _uiState.update {
                        it.copy(
                            aiTaskState = AiTaskState.Idle,
                            statusBanner = "Naskah narasi komik siap dibacakan!"
                        )
                    }
                    onPolished(res.data)
                }
                is AiTaskResult.Error -> {
                    _uiState.update { it.copy(aiTaskState = AiTaskState.Error(res.message)) }
                }
            }
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
        val existingIdx = clip.keyframes.indexOfFirst { abs(it.timeOffsetMs - offsetMs) <= 200L }
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
        mutateTimeline(pausePlayback = false) {
            it.copy(canvasBgHex = colorHex, canvasFitMode = CanvasFitMode.AUTO_FIT)
        }
    }

    fun setCanvasBackgroundImage(uri: Uri) {
        MediaMetadataHelper.takePersistablePermissionIfPossible(getApplication(), uri)
        mutateTimeline(pausePlayback = false) {
            it.copy(canvasBgImageUri = uri.toString(), canvasFitMode = CanvasFitMode.CUSTOM_IMAGE)
        }
        _uiState.update { it.copy(statusBanner = "Gambar latar kanvas diperbarui") }
    }

    fun toggleExportWatermark(enabled: Boolean) {
        mutateTimeline(pausePlayback = false) { it.copy(includeWatermark = enabled) }
    }

    fun addTextOverlay(
        text: String,
        colorHex: Long = 0xFFFFFFFF,
        bgColorHex: Long = 0xCC16161A,
        styleId: String = "classic",
        useGradient: Boolean = false,
        isSticker: Boolean = false
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
            useGradient = useGradient,
            isSticker = isSticker,
            styleId = styleId
        )
        mutateTimeline(pausePlayback = false) { timeline ->
            timeline.copy(textOverlays = timeline.textOverlays + item)
        }
        _uiState.update { it.copy(selectedTextId = item.id) }
    }

    fun updateTextOverlayById(id: String, transform: (TextOverlayItem) -> TextOverlayItem) {
        mutateTimeline(pausePlayback = false) { timeline ->
            timeline.copy(
                textOverlays = timeline.textOverlays.map {
                    if (it.id == id) transform(it) else it
                }
            )
        }
    }

    fun updateSelectedTextOverlay(transform: (TextOverlayItem) -> TextOverlayItem) {
        val id = _uiState.value.selectedTextId ?: return
        updateTextOverlayById(id, transform)
    }

    fun transformTextOverlay(
        id: String,
        dxFraction: Float,
        dyFraction: Float,
        scaleFactor: Float,
        rotationDelta: Float
    ) {
        _uiState.update { state ->
            val updated = state.timeline.copy(
                textOverlays = state.timeline.textOverlays.map {
                    if (it.id == id) {
                        it.copy(
                            xFraction = (it.xFraction + dxFraction).coerceIn(0.08f, 0.92f),
                            yFraction = (it.yFraction + dyFraction).coerceIn(0.08f, 0.92f),
                            scale = (it.scale * scaleFactor).coerceIn(0.5f, 3.0f),
                            rotationDeg = ((it.rotationDeg + rotationDelta) % 360f)
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
            _uiState.update { it.copy(aiTaskState = AiTaskState.Loading("KN AI sedang membuat Auto-Caption...")) }
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
                            bgColorHex = 0xCC16161A,
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

    /**
     * Suara Otomatis ke Penerjemah:
     * Detects spoken audio in the video/clip (or vocal track) and directly produces
     * time-synced translated subtitles in targetLanguage, editable in timeline & list.
     */
    fun autoTranscribeAndTranslate(targetLanguage: String) {
        val state = _uiState.value
        val clip = state.selectedClip ?: state.timeline.clips.firstOrNull()
        val mediaUri = when {
            state.timeline.vocalTrack != null -> Uri.parse(state.timeline.vocalTrack.uriString)
            clip != null -> Uri.parse(clip.uriString)
            else -> null
        } ?: return

        val clipIdx = if (state.selectedClip != null) state.selectedClipIndex else 0
        val startOffset = if (state.timeline.vocalTrack != null) state.timeline.vocalTrack.startOffsetMs
        else state.timeline.clipStartPositionMs(clipIdx)
        val dur = clip?.effectiveDurationMs ?: state.timeline.totalDurationMs.coerceAtLeast(3000L)

        viewModelScope.launch {
            _uiState.update {
                it.copy(aiTaskState = AiTaskState.Loading("Mendeteksi suara & menerjemahkan ke $targetLanguage..."))
            }
            val result = aiService.transcribeAndTranslateVideo(
                context = getApplication(),
                mediaUri = mediaUri,
                clipStartOffsetMs = startOffset,
                clipDurationMs = dur,
                targetLanguage = targetLanguage
            )
            when (result) {
                is AiTaskResult.Success -> {
                    val generated = result.data.map { seg ->
                        TextOverlayItem(
                            text = seg.text,
                            originalText = seg.originalText,
                            startMs = seg.startMs,
                            endMs = seg.endMs,
                            xFraction = 0.5f,
                            yFraction = 0.80f,
                            fontSizeSp = 20f,
                            colorHex = 0xFFFFFFFF,
                            bgColorHex = 0xCC16161A,
                            styleId = "classic"
                        )
                    }
                    mutateTimeline(pausePlayback = false) { timeline ->
                        timeline.copy(textOverlays = timeline.textOverlays + generated)
                    }
                    _uiState.update {
                        it.copy(
                            aiTaskState = AiTaskState.Idle,
                            selectedTextId = generated.firstOrNull()?.id,
                            statusBanner = "${generated.size} terjemahan suara ($targetLanguage) ditambahkan!"
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
                                if (translated != null) {
                                    item.copy(
                                        originalText = item.originalText ?: item.text,
                                        text = translated
                                    )
                                } else item
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

    fun transformPipOverlay(
        id: String,
        dxFraction: Float,
        dyFraction: Float,
        scaleFactor: Float,
        rotationDelta: Float
    ) {
        _uiState.update { state ->
            val updated = state.timeline.copy(
                pipOverlays = state.timeline.pipOverlays.map {
                    if (it.id == id) {
                        it.copy(
                            xFraction = (it.xFraction + dxFraction).coerceIn(0.08f, 0.92f),
                            yFraction = (it.yFraction + dyFraction).coerceIn(0.08f, 0.92f),
                            scale = (it.scale * scaleFactor).coerceIn(0.16f, 0.95f),
                            rotationDeg = ((it.rotationDeg + rotationDelta) % 360f)
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

    fun setAudioTrackFromUri(uri: Uri, role: AudioTrackRole = _uiState.value.selectedAudioRole) {
        viewModelScope.launch {
            val probed = MediaMetadataHelper.probeAudio(getApplication(), uri)
            val audio = AudioTrackItem(
                uriString = probed.uriString,
                title = probed.title,
                role = role,
                durationMs = probed.durationMs,
                trimStartMs = 0L,
                trimEndMs = probed.durationMs,
                vocalMode = if (role == AudioTrackRole.VOCAL) VocalSeparationMode.VOCAL_ONLY else VocalSeparationMode.ORIGINAL
            )
            mutateTimeline(pausePlayback = false) {
                when (role) {
                    AudioTrackRole.VOCAL -> it.copy(vocalTrack = audio)
                    AudioTrackRole.MUSIC -> it.copy(musicTrack = audio)
                }
            }
            _uiState.update {
                it.copy(
                    selectedAudioRole = role,
                    statusBanner = "${role.label}: '${probed.title}' ditambahkan"
                )
            }
        }
    }

    private fun updateTrackByRole(
        role: AudioTrackRole,
        pausePlayback: Boolean = false,
        transform: (AudioTrackItem) -> AudioTrackItem
    ) {
        mutateTimeline(pausePlayback = pausePlayback) { tl ->
            when (role) {
                AudioTrackRole.VOCAL -> tl.vocalTrack?.let { tl.copy(vocalTrack = transform(it)) } ?: tl
                AudioTrackRole.MUSIC -> tl.musicTrack?.let { tl.copy(musicTrack = transform(it)) } ?: tl
            }
        }
    }

    fun updateAudioVolume(role: AudioTrackRole, volume: Float) {
        updateTrackByRole(role) { it.copy(volume = volume.coerceIn(0f, 1f)) }
    }

    fun updateAudioTrim(role: AudioTrackRole, trimStartMs: Long, trimEndMs: Long) {
        updateTrackByRole(role) { tr ->
            val maxD = tr.durationMs.coerceAtLeast(1000L)
            val s = trimStartMs.coerceIn(0L, (maxD - 400L).coerceAtLeast(0L))
            val e = trimEndMs.coerceIn((s + 400L).coerceAtMost(maxD), maxD)
            tr.copy(trimStartMs = s, trimEndMs = e)
        }
    }

    fun shiftAudioOffset(role: AudioTrackRole, deltaMs: Long) {
        val total = _uiState.value.timeline.totalDurationMs.coerceAtLeast(5000L)
        updateTrackByRole(role) { tr ->
            tr.copy(startOffsetMs = (tr.startOffsetMs + deltaMs).coerceIn(0L, total))
        }
    }

    fun updateAudioVoiceEffect(role: AudioTrackRole, effect: VoiceEffectPreset) {
        updateTrackByRole(role) { it.copy(voiceEffect = effect) }
    }

    fun updateAudioVocalMode(role: AudioTrackRole, mode: VocalSeparationMode) {
        updateTrackByRole(role) { it.copy(vocalMode = mode) }
    }

    fun toggleAudioNoiseReduction(role: AudioTrackRole) {
        updateTrackByRole(role) { it.copy(noiseReduction = !it.noiseReduction) }
    }

    fun toggleAudioDucking(role: AudioTrackRole) {
        updateTrackByRole(role) { it.copy(autoDucking = !it.autoDucking) }
    }

    fun toggleAudioFade(role: AudioTrackRole, fadeIn: Boolean? = null, fadeOut: Boolean? = null) {
        updateTrackByRole(role) {
            it.copy(
                fadeIn = fadeIn ?: it.fadeIn,
                fadeOut = fadeOut ?: it.fadeOut
            )
        }
    }

    fun removeAudioTrack(role: AudioTrackRole) {
        mutateTimeline(pausePlayback = false) { tl ->
            when (role) {
                AudioTrackRole.VOCAL -> tl.copy(vocalTrack = null)
                AudioTrackRole.MUSIC -> tl.copy(musicTrack = null)
            }
        }
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
                _uiState.update { it.copy(statusBanner = "Video disimpan ke Galeri (Movies/KomikNarasi)") }
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
            Intent.createChooser(intent, "Bagikan Video Komik Narasi").apply {
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
