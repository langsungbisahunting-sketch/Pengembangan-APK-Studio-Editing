package com.example.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.VeloCutDatabase
import com.example.data.repository.ProjectRepository
import com.example.media.ImageEditorRenderer
import com.example.media.MediaMetadataHelper
import com.example.model.AspectRatioPreset
import com.example.model.ColorAdjustments
import com.example.model.FilterPreset
import com.example.model.ImageBrushStroke
import com.example.model.ImageCropRatio
import com.example.model.ImageEditorDocument
import com.example.model.ImageEditorTool
import com.example.model.ImageStickerLayer
import com.example.model.ImageTextLayer
import com.example.model.NormalizedPoint
import com.example.model.ProjectTimelineData
import com.example.model.TextFontStyle
import com.example.model.VideoClip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ImageEditorUiState(
    val document: ImageEditorDocument = ImageEditorDocument(),
    val baseBitmap: Bitmap? = null,
    val cutoutBitmap: Bitmap? = null,
    val activeTool: ImageEditorTool = ImageEditorTool.TEXT,
    val selectedTextId: String? = null,
    val selectedStickerId: String? = null,
    val brushColorHex: Long = 0xFFFF6B00,
    val brushSizeDp: Float = 8f,
    val isEraserMode: Boolean = false,
    val activeStrokePoints: List<NormalizedPoint> = emptyList(),
    val isBusy: Boolean = false,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false
) {
    val activePreviewBitmap: Bitmap?
        get() = if (document.removeBackground) cutoutBitmap ?: baseBitmap else baseBitmap

    val selectedText: ImageTextLayer?
        get() = document.textLayers.firstOrNull { it.id == selectedTextId }

    val selectedSticker: ImageStickerLayer?
        get() = document.stickers.firstOrNull { it.id == selectedStickerId }
}

class ImageEditorViewModel(application: Application) : AndroidViewModel(application) {

    private val database = VeloCutDatabase.getInstance(application)
    private val repository = ProjectRepository(database.veloCutDao())

    private val _uiState = MutableStateFlow(ImageEditorUiState())
    val uiState: StateFlow<ImageEditorUiState> = _uiState.asStateFlow()

    private val undoStack = ArrayDeque<ImageEditorDocument>()
    private val redoStack = ArrayDeque<ImageEditorDocument>()

    fun loadImageUri(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            val context = getApplication<Application>()
            MediaMetadataHelper.takePersistablePermissionIfPossible(context, uri)
            val bmp = ImageEditorRenderer.loadPreviewBitmap(context, uri, maxDim = 960)
            undoStack.clear()
            redoStack.clear()
            _uiState.update {
                ImageEditorUiState(
                    document = ImageEditorDocument(sourceUriString = uri.toString()),
                    baseBitmap = bmp,
                    isBusy = false
                )
            }
        }
    }

    fun selectTool(tool: ImageEditorTool) {
        _uiState.update { it.copy(activeTool = tool) }
    }

    fun selectText(id: String?) {
        _uiState.update {
            it.copy(
                selectedTextId = id,
                selectedStickerId = if (id != null) null else it.selectedStickerId,
                activeTool = if (id != null) ImageEditorTool.TEXT else it.activeTool
            )
        }
    }

    fun selectSticker(id: String?) {
        _uiState.update {
            it.copy(
                selectedStickerId = id,
                selectedTextId = if (id != null) null else it.selectedTextId,
                activeTool = if (id != null) ImageEditorTool.STICKER else it.activeTool
            )
        }
    }

    private fun mutateDoc(transform: (ImageEditorDocument) -> ImageEditorDocument) {
        val current = _uiState.value.document
        val updated = transform(current)
        if (updated == current) return
        if (undoStack.size >= 30) undoStack.removeFirst()
        undoStack.addLast(current)
        redoStack.clear()
        _uiState.update {
            it.copy(
                document = updated,
                canUndo = undoStack.isNotEmpty(),
                canRedo = false
            )
        }
    }

    fun undo() {
        val prev = undoStack.removeLastOrNull() ?: return
        redoStack.addLast(_uiState.value.document)
        _uiState.update {
            it.copy(
                document = prev,
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty()
            )
        }
    }

    fun redo() {
        val next = redoStack.removeLastOrNull() ?: return
        undoStack.addLast(_uiState.value.document)
        _uiState.update {
            it.copy(
                document = next,
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty()
            )
        }
    }

    // Text Tools
    fun addText(text: String) {
        val clean = text.trim()
        if (clean.isEmpty()) return
        val layer = ImageTextLayer(text = clean)
        mutateDoc { it.copy(textLayers = it.textLayers + layer) }
        _uiState.update { it.copy(selectedTextId = layer.id) }
    }

    fun updateSelectedText(transform: (ImageTextLayer) -> ImageTextLayer) {
        val id = _uiState.value.selectedTextId ?: return
        mutateDoc { doc ->
            doc.copy(textLayers = doc.textLayers.map { if (it.id == id) transform(it) else it })
        }
    }

    fun transformText(id: String, dx: Float, dy: Float, scaleFactor: Float, rotDelta: Float) {
        _uiState.update { state ->
            val updated = state.document.copy(
                textLayers = state.document.textLayers.map {
                    if (it.id == id) {
                        it.copy(
                            xFraction = (it.xFraction + dx).coerceIn(0.08f, 0.92f),
                            yFraction = (it.yFraction + dy).coerceIn(0.08f, 0.92f),
                            scale = (it.scale * scaleFactor).coerceIn(0.45f, 3.2f),
                            rotationDeg = (it.rotationDeg + rotDelta) % 360f
                        )
                    } else it
                }
            )
            state.copy(document = updated, selectedTextId = id)
        }
    }

    fun deleteSelectedText() {
        val id = _uiState.value.selectedTextId ?: return
        mutateDoc { it.copy(textLayers = it.textLayers.filterNot { t -> t.id == id }) }
        _uiState.update { it.copy(selectedTextId = null) }
    }

    // Sticker Tools
    fun addSticker(label: String, bgHex: Long = 0xFFFFC107, fgHex: Long = 0xFF1A1200) {
        val item = ImageStickerLayer(label = label, bgHex = bgHex, fgHex = fgHex)
        mutateDoc { it.copy(stickers = it.stickers + item) }
        _uiState.update { it.copy(selectedStickerId = item.id) }
    }

    fun transformSticker(id: String, dx: Float, dy: Float, scaleFactor: Float, rotDelta: Float) {
        _uiState.update { state ->
            val updated = state.document.copy(
                stickers = state.document.stickers.map {
                    if (it.id == id) {
                        it.copy(
                            xFraction = (it.xFraction + dx).coerceIn(0.08f, 0.92f),
                            yFraction = (it.yFraction + dy).coerceIn(0.08f, 0.92f),
                            scale = (it.scale * scaleFactor).coerceIn(0.45f, 3.2f),
                            rotationDeg = (it.rotationDeg + rotDelta) % 360f
                        )
                    } else it
                }
            )
            state.copy(document = updated, selectedStickerId = id)
        }
    }

    fun deleteSelectedSticker() {
        val id = _uiState.value.selectedStickerId ?: return
        mutateDoc { it.copy(stickers = it.stickers.filterNot { s -> s.id == id }) }
        _uiState.update { it.copy(selectedStickerId = null) }
    }

    // Crop, Rotate, Flip
    fun setCropRatio(ratio: ImageCropRatio) = mutateDoc { it.copy(cropRatio = ratio) }
    fun rotate90() = mutateDoc { it.copy(rotationDegrees = (it.rotationDegrees + 90) % 360) }
    fun toggleFlipH() = mutateDoc { it.copy(flipHorizontal = !it.flipHorizontal) }
    fun toggleFlipV() = mutateDoc { it.copy(flipVertical = !it.flipVertical) }

    // Filter & Adjustments
    fun setFilter(preset: FilterPreset) = mutateDoc { it.copy(filterPreset = preset) }
    fun updateAdjustments(adj: ColorAdjustments) = mutateDoc { it.copy(adjustments = adj) }

    // Brush
    fun setBrushColor(hex: Long) = _uiState.update { it.copy(brushColorHex = hex, isEraserMode = false) }
    fun setBrushSize(dp: Float) = _uiState.update { it.copy(brushSizeDp = dp.coerceIn(2f, 28f)) }
    fun toggleEraser(enabled: Boolean) = _uiState.update { it.copy(isEraserMode = enabled) }
    fun clearBrushStrokes() = mutateDoc { it.copy(strokes = emptyList()) }

    fun startStroke(pt: NormalizedPoint) {
        _uiState.update { it.copy(activeStrokePoints = listOf(pt)) }
    }

    fun appendStrokePoint(pt: NormalizedPoint) {
        _uiState.update { it.copy(activeStrokePoints = it.activeStrokePoints + pt) }
    }

    fun finishStroke() {
        val st = _uiState.value
        val pts = st.activeStrokePoints
        _uiState.update { it.copy(activeStrokePoints = emptyList()) }
        if (pts.size < 2) return
        val stroke = ImageBrushStroke(
            points = pts,
            colorHex = st.brushColorHex,
            strokeWidthDp = st.brushSizeDp,
            isEraser = st.isEraserMode
        )
        mutateDoc { it.copy(strokes = it.strokes + stroke) }
    }

    // Background Removal
    fun toggleBackgroundRemoval(enabled: Boolean) {
        mutateDoc { it.copy(removeBackground = enabled) }
        if (enabled) rebuildCutoutPreview()
    }

    fun setBgTolerance(tol: Float) {
        mutateDoc { it.copy(bgTolerance = tol.coerceIn(0.05f, 0.65f), removeBackground = true) }
        rebuildCutoutPreview()
    }

    fun setCustomBgColor(hex: Long) {
        mutateDoc { it.copy(customBgColorHex = hex, removeBackground = true) }
        rebuildCutoutPreview()
    }

    private fun rebuildCutoutPreview() {
        val base = _uiState.value.baseBitmap ?: return
        val tol = _uiState.value.document.bgTolerance
        viewModelScope.launch {
            val cut = withContext(Dispatchers.Default) {
                ImageEditorRenderer.applyBackgroundCutout(base, tol)
            }
            _uiState.update { it.copy(cutoutBitmap = cut) }
        }
    }

    fun saveEditedImageToGallery(onResult: (String) -> Unit) {
        val doc = _uiState.value.document
        if (doc.sourceUriString == null) {
            onResult("Pilih gambar terlebih dahulu")
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            val file = ImageEditorRenderer.renderDocumentToFile(getApplication(), doc)
            if (file == null) {
                _uiState.update { it.copy(isBusy = false) }
                onResult("Gagal memproses gambar")
                return@launch
            }
            val uri = ImageEditorRenderer.saveImageToGallery(
                getApplication(),
                file,
                "KomikNarasi_${System.currentTimeMillis()}"
            )
            _uiState.update { it.copy(isBusy = false) }
            onResult(
                if (uri != null) "Gambar tersimpan ke Galeri (Pictures/KomikNarasi)"
                else "Gagal menyimpan ke Galeri"
            )
        }
    }

    fun sendToVideoTimeline(onProjectCreated: (Long) -> Unit, onError: (String) -> Unit) {
        val doc = _uiState.value.document
        if (doc.sourceUriString == null) {
            onError("Pilih gambar terlebih dahulu")
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            val file = ImageEditorRenderer.renderDocumentToFile(getApplication(), doc)
            if (file == null) {
                _uiState.update { it.copy(isBusy = false) }
                onError("Gagal merender gambar untuk timeline")
                return@launch
            }
            val clip = VideoClip(
                uriString = Uri.fromFile(file).toString(),
                displayName = "Panel Komik",
                isImage = true,
                sourceDurationMs = 5000L,
                trimStartMs = 0L,
                trimEndMs = 5000L
            )
            val ratio = when (doc.cropRatio) {
                com.example.model.ImageCropRatio.SQUARE -> AspectRatioPreset.RATIO_1_1
                com.example.model.ImageCropRatio.COMIC -> AspectRatioPreset.RATIO_3_4
                com.example.model.ImageCropRatio.FEED -> AspectRatioPreset.RATIO_4_5
                com.example.model.ImageCropRatio.LANDSCAPE -> AspectRatioPreset.RATIO_16_9
                else -> AspectRatioPreset.RATIO_9_16
            }
            val timeline = ProjectTimelineData(clips = listOf(clip), aspectRatio = ratio)
            val id = repository.createProject("KN Panel Gambar", timeline)
            _uiState.update { it.copy(isBusy = false) }
            onProjectCreated(id)
        }
    }
}
