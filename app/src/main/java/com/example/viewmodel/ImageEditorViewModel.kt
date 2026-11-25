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
import com.example.model.BlendModePreset
import com.example.model.ColorAdjustments
import com.example.model.FilterPreset
import com.example.model.ImageBrushStroke
import com.example.model.ImageCropRatio
import com.example.model.ImageCropRect
import com.example.model.ImageEditorDocument
import com.example.model.ImageEditorTool
import com.example.model.ImageFloodFillOp
import com.example.model.ImageOverlayLayer
import com.example.model.ImageShapeLayer
import com.example.model.ImageStickerLayer
import com.example.model.ImageTextLayer
import com.example.model.NormalizedPoint
import com.example.model.PaintBrushType
import com.example.model.PaintShapeType
import com.example.model.ProjectTimelineData
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
    val workingBitmap: Bitmap? = null,
    val activeTool: ImageEditorTool = ImageEditorTool.PAINT,
    val selectedTextId: String? = null,
    val selectedStickerId: String? = null,
    val selectedOverlayId: String? = null,
    val paintBrushType: PaintBrushType = PaintBrushType.BRUSH,
    val brushColorHex: Long = 0xFFFF6B00,
    val brushSizeDp: Float = 8f,
    val brushOpacity: Float = 1f,
    val activeStrokePoints: List<NormalizedPoint> = emptyList(),
    val activeShapeType: PaintShapeType = PaintShapeType.RECTANGLE,
    val shapeFilled: Boolean = false,
    val draftShape: ImageShapeLayer? = null,
    val draftCropRect: ImageCropRect = ImageCropRect(),
    val canvasZoom: Float = 1f,
    val isBusy: Boolean = false,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false
) {
    val activePreviewBitmap: Bitmap?
        get() = workingBitmap ?: baseBitmap

    val selectedText: ImageTextLayer?
        get() = document.textLayers.firstOrNull { it.id == selectedTextId }

    val selectedSticker: ImageStickerLayer?
        get() = document.stickers.firstOrNull { it.id == selectedStickerId }

    val selectedOverlay: ImageOverlayLayer?
        get() = document.overlayImages.firstOrNull { it.id == selectedOverlayId }
}

class ImageEditorViewModel(application: Application) : AndroidViewModel(application) {

    private val database = VeloCutDatabase.getInstance(application)
    private val repository = ProjectRepository(database.veloCutDao())

    private val _uiState = MutableStateFlow(ImageEditorUiState())
    val uiState: StateFlow<ImageEditorUiState> = _uiState.asStateFlow()

    private val undoStack = ArrayDeque<ImageEditorDocument>()
    private val redoStack = ArrayDeque<ImageEditorDocument>()

    init {
        createBlankCanvas(width = 960, height = 960, bgColorHex = 0xFFFFFFFFL, resetHistory = true)
    }

    fun createBlankCanvas(
        width: Int = 960,
        height: Int = 960,
        bgColorHex: Long = 0xFFFFFFFFL,
        resetHistory: Boolean = false
    ) {
        viewModelScope.launch {
            val bmp = withContext(Dispatchers.Default) {
                ImageEditorRenderer.createBlankCanvasBitmap(width, height, bgColorHex)
            }
            if (resetHistory) {
                undoStack.clear()
                redoStack.clear()
            } else {
                if (undoStack.size >= 30) undoStack.removeFirst()
                undoStack.addLast(_uiState.value.document)
                redoStack.clear()
            }
            val doc = ImageEditorDocument(
                sourceUriString = null,
                isBlankCanvas = true,
                blankCanvasWidth = width,
                blankCanvasHeight = height,
                canvasBaseColorHex = bgColorHex
            )
            _uiState.update {
                ImageEditorUiState(
                    document = doc,
                    baseBitmap = bmp,
                    workingBitmap = bmp,
                    activeTool = it.activeTool,
                    brushColorHex = if (bgColorHex == 0xFF0B0B0DL) 0xFFFFFFFFL else 0xFFFF6B00L,
                    canUndo = undoStack.isNotEmpty(),
                    canRedo = redoStack.isNotEmpty()
                )
            }
        }
    }

    fun loadImageUri(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            val context = getApplication<Application>()
            MediaMetadataHelper.takePersistablePermissionIfPossible(context, uri)
            val bmp = ImageEditorRenderer.loadPreviewBitmap(context, uri, maxDim = 960)
            undoStack.clear()
            redoStack.clear()
            val doc = ImageEditorDocument(
                sourceUriString = uri.toString(),
                isBlankCanvas = false
            )
            _uiState.update {
                ImageEditorUiState(
                    document = doc,
                    baseBitmap = bmp,
                    workingBitmap = bmp,
                    isBusy = false
                )
            }
        }
    }

    fun selectTool(tool: ImageEditorTool) {
        _uiState.update {
            it.copy(
                activeTool = tool,
                draftCropRect = if (tool == ImageEditorTool.CROP_TRANSFORM && it.draftCropRect.isFull) {
                    ImageCropRect(0.08f, 0.08f, 0.92f, 0.92f)
                } else it.draftCropRect
            )
        }
    }

    fun setCanvasZoom(zoom: Float) {
        _uiState.update { it.copy(canvasZoom = zoom.coerceIn(1f, 3.5f)) }
    }

    fun selectText(id: String?) {
        _uiState.update {
            it.copy(
                selectedTextId = id,
                selectedStickerId = if (id != null) null else it.selectedStickerId,
                selectedOverlayId = if (id != null) null else it.selectedOverlayId,
                activeTool = if (id != null) ImageEditorTool.TEXT else it.activeTool
            )
        }
    }

    fun selectSticker(id: String?) {
        _uiState.update {
            it.copy(
                selectedStickerId = id,
                selectedTextId = if (id != null) null else it.selectedTextId,
                selectedOverlayId = if (id != null) null else it.selectedOverlayId,
                activeTool = if (id != null) ImageEditorTool.STICKER else it.activeTool
            )
        }
    }

    fun selectOverlayImage(id: String?) {
        _uiState.update {
            it.copy(
                selectedOverlayId = id,
                selectedTextId = if (id != null) null else it.selectedTextId,
                selectedStickerId = if (id != null) null else it.selectedStickerId,
                activeTool = if (id != null) ImageEditorTool.OVERLAY_IMAGE else it.activeTool
            )
        }
    }

    private fun mutateDoc(
        rebuildBase: Boolean = false,
        transform: (ImageEditorDocument) -> ImageEditorDocument
    ) {
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
        if (rebuildBase) rebuildWorkingBitmap()
    }

    fun undo() {
        val prev = undoStack.removeLastOrNull() ?: return
        redoStack.addLast(_uiState.value.document)
        val shouldRefreshBase = prev.sourceUriString != _uiState.value.document.sourceUriString ||
            prev.canvasBaseColorHex != _uiState.value.document.canvasBaseColorHex ||
            prev.blankCanvasWidth != _uiState.value.document.blankCanvasWidth ||
            prev.blankCanvasHeight != _uiState.value.document.blankCanvasHeight
        _uiState.update {
            it.copy(
                document = prev,
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty()
            )
        }
        if (shouldRefreshBase) {
            reloadBaseForDoc(prev)
        } else {
            rebuildWorkingBitmap()
        }
    }

    fun redo() {
        val next = redoStack.removeLastOrNull() ?: return
        undoStack.addLast(_uiState.value.document)
        val shouldRefreshBase = next.sourceUriString != _uiState.value.document.sourceUriString ||
            next.canvasBaseColorHex != _uiState.value.document.canvasBaseColorHex ||
            next.blankCanvasWidth != _uiState.value.document.blankCanvasWidth ||
            next.blankCanvasHeight != _uiState.value.document.blankCanvasHeight
        _uiState.update {
            it.copy(
                document = next,
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty()
            )
        }
        if (shouldRefreshBase) {
            reloadBaseForDoc(next)
        } else {
            rebuildWorkingBitmap()
        }
    }

    private fun reloadBaseForDoc(doc: ImageEditorDocument) {
        viewModelScope.launch {
            val base = if (doc.sourceUriString != null) {
                ImageEditorRenderer.loadPreviewBitmap(getApplication(), Uri.parse(doc.sourceUriString), 960)
            } else {
                withContext(Dispatchers.Default) {
                    ImageEditorRenderer.createBlankCanvasBitmap(doc.blankCanvasWidth, doc.blankCanvasHeight, doc.canvasBaseColorHex)
                }
            }
            _uiState.update { it.copy(baseBitmap = base) }
            rebuildWorkingBitmap()
        }
    }

    private fun rebuildWorkingBitmap() {
        val base = _uiState.value.baseBitmap ?: return
        val doc = _uiState.value.document
        viewModelScope.launch {
            val rebuilt = withContext(Dispatchers.Default) {
                ImageEditorRenderer.buildPreviewWorkingBitmap(base, doc)
            }
            _uiState.update { it.copy(workingBitmap = rebuilt) }
        }
    }

    // Paint Tools (Pencil, Brush, Marker, Airbrush, Bucket Fill, Eyedropper, Eraser)
    fun selectPaintBrushType(type: PaintBrushType) {
        _uiState.update { it.copy(paintBrushType = type) }
    }

    fun setBrushColor(hex: Long) {
        _uiState.update {
            val nextType = if (it.paintBrushType == PaintBrushType.ERASER || it.paintBrushType == PaintBrushType.EYEDROPPER) {
                PaintBrushType.BRUSH
            } else it.paintBrushType
            it.copy(brushColorHex = hex, paintBrushType = nextType)
        }
    }

    fun setBrushSize(dp: Float) = _uiState.update { it.copy(brushSizeDp = dp.coerceIn(2f, 34f)) }
    fun setBrushOpacity(alpha: Float) = _uiState.update { it.copy(brushOpacity = alpha.coerceIn(0.15f, 1f)) }
    fun clearBrushStrokesAndFills() = mutateDoc(rebuildBase = true) { it.copy(strokes = emptyList(), floodFills = emptyList()) }

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
            brushType = st.paintBrushType,
            alpha = st.brushOpacity,
            isEraser = st.paintBrushType == PaintBrushType.ERASER
        )
        mutateDoc { it.copy(strokes = it.strokes + stroke) }
    }

    fun performFloodFillAt(normX: Float, normY: Float) {
        val colorHex = _uiState.value.brushColorHex
        val op = ImageFloodFillOp(
            xFraction = normX.coerceIn(0f, 1f),
            yFraction = normY.coerceIn(0f, 1f),
            colorHex = colorHex,
            tolerance = 38
        )
        mutateDoc(rebuildBase = true) { it.copy(floodFills = it.floodFills + op) }
    }

    fun pickColorAt(normX: Float, normY: Float, onPicked: (String) -> Unit) {
        val bmp = _uiState.value.activePreviewBitmap ?: return
        val sampled = ImageEditorRenderer.samplePixelColor(bmp, normX, normY)
        _uiState.update {
            it.copy(
                brushColorHex = sampled,
                paintBrushType = PaintBrushType.BRUSH
            )
        }
        onPicked("Warna dipilih dari kanvas (#${java.lang.Long.toHexString(sampled).uppercase()})")
    }

    // MS Paint Geometric Shapes
    fun selectShapeType(type: PaintShapeType) = _uiState.update { it.copy(activeShapeType = type) }
    fun toggleShapeFilled(filled: Boolean) = _uiState.update { it.copy(shapeFilled = filled) }
    fun deleteLastShape() = mutateDoc { it.copy(shapes = it.shapes.dropLast(1)) }

    fun startShapeDrag(pt: NormalizedPoint) {
        val st = _uiState.value
        _uiState.update {
            it.copy(
                draftShape = ImageShapeLayer(
                    shapeType = st.activeShapeType,
                    startX = pt.x,
                    startY = pt.y,
                    endX = pt.x,
                    endY = pt.y,
                    colorHex = st.brushColorHex,
                    strokeWidthDp = st.brushSizeDp,
                    filled = st.shapeFilled,
                    alpha = st.brushOpacity
                )
            )
        }
    }

    fun updateShapeDrag(pt: NormalizedPoint) {
        _uiState.update { st ->
            st.copy(draftShape = st.draftShape?.copy(endX = pt.x, endY = pt.y))
        }
    }

    fun finishShapeDrag() {
        val shape = _uiState.value.draftShape ?: return
        _uiState.update { it.copy(draftShape = null) }
        val dx = kotlin.math.abs(shape.endX - shape.startX)
        val dy = kotlin.math.abs(shape.endY - shape.startY)
        if (dx < 0.015f && dy < 0.015f) return
        mutateDoc { it.copy(shapes = it.shapes + shape) }
    }

    // Timpa Gambar (Overlay Images)
    fun addOverlayImage(uri: Uri) {
        val context = getApplication<Application>()
        MediaMetadataHelper.takePersistablePermissionIfPossible(context, uri)
        val name = uri.lastPathSegment?.substringAfterLast('/')?.take(24) ?: "Gambar Timpa"
        val item = ImageOverlayLayer(
            uriString = uri.toString(),
            displayName = name
        )
        mutateDoc { it.copy(overlayImages = it.overlayImages + item) }
        _uiState.update {
            it.copy(
                selectedOverlayId = item.id,
                activeTool = ImageEditorTool.OVERLAY_IMAGE
            )
        }
    }

    fun transformOverlayImage(id: String, dx: Float, dy: Float, scaleFactor: Float, rotDelta: Float) {
        _uiState.update { state ->
            val updated = state.document.copy(
                overlayImages = state.document.overlayImages.map {
                    if (it.id == id) {
                        it.copy(
                            xFraction = (it.xFraction + dx).coerceIn(0.05f, 0.95f),
                            yFraction = (it.yFraction + dy).coerceIn(0.05f, 0.95f),
                            scale = (it.scale * scaleFactor).coerceIn(0.14f, 1.8f),
                            rotationDeg = (it.rotationDeg + rotDelta) % 360f
                        )
                    } else it
                }
            )
            state.copy(document = updated, selectedOverlayId = id)
        }
    }

    fun updateSelectedOverlayImage(transform: (ImageOverlayLayer) -> ImageOverlayLayer) {
        val id = _uiState.value.selectedOverlayId ?: return
        mutateDoc { doc ->
            doc.copy(overlayImages = doc.overlayImages.map { if (it.id == id) transform(it) else it })
        }
    }

    fun bringSelectedOverlayToFront() {
        val id = _uiState.value.selectedOverlayId ?: return
        mutateDoc { doc ->
            val item = doc.overlayImages.firstOrNull { it.id == id } ?: return@mutateDoc doc
            doc.copy(overlayImages = doc.overlayImages.filterNot { it.id == id } + item)
        }
    }

    fun deleteSelectedOverlayImage() {
        val id = _uiState.value.selectedOverlayId ?: return
        mutateDoc { doc ->
            doc.copy(overlayImages = doc.overlayImages.filterNot { it.id == id })
        }
        _uiState.update { it.copy(selectedOverlayId = null) }
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

    // Interactive MS Paint Crop, Rotate, Flip
    fun updateDraftCropRect(rect: ImageCropRect) {
        val minSpan = 0.14f
        val l = rect.left.coerceIn(0f, 1f - minSpan)
        val t = rect.top.coerceIn(0f, 1f - minSpan)
        val r = rect.right.coerceIn(l + minSpan, 1f)
        val b = rect.bottom.coerceIn(t + minSpan, 1f)
        _uiState.update { it.copy(draftCropRect = ImageCropRect(l, t, r, b)) }
    }

    fun applyCropNow() {
        val draft = _uiState.value.draftCropRect
        if (draft.isFull) return
        mutateDoc(rebuildBase = true) { doc ->
            val cur = doc.appliedCropRect
            val newRect = ImageCropRect(
                left = (cur.left + cur.width * draft.left).coerceIn(0f, 0.95f),
                top = (cur.top + cur.height * draft.top).coerceIn(0f, 0.95f),
                right = (cur.left + cur.width * draft.right).coerceIn(0.05f, 1f),
                bottom = (cur.top + cur.height * draft.bottom).coerceIn(0.05f, 1f)
            )
            doc.copy(appliedCropRect = newRect, cropRatio = ImageCropRatio.FREE)
        }
        _uiState.update { it.copy(draftCropRect = ImageCropRect(0.08f, 0.08f, 0.92f, 0.92f)) }
    }

    fun resetCrop() {
        mutateDoc(rebuildBase = true) {
            it.copy(appliedCropRect = ImageCropRect(), cropRatio = ImageCropRatio.FREE)
        }
        _uiState.update { it.copy(draftCropRect = ImageCropRect(0.08f, 0.08f, 0.92f, 0.92f)) }
    }

    fun setCropRatio(ratio: ImageCropRatio) = mutateDoc(rebuildBase = true) { it.copy(cropRatio = ratio) }
    fun rotate90() = mutateDoc(rebuildBase = true) { it.copy(rotationDegrees = (it.rotationDegrees + 90) % 360) }
    fun toggleFlipH() = mutateDoc(rebuildBase = true) { it.copy(flipHorizontal = !it.flipHorizontal) }
    fun toggleFlipV() = mutateDoc(rebuildBase = true) { it.copy(flipVertical = !it.flipVertical) }

    // Filter & Adjustments
    fun setFilter(preset: FilterPreset) = mutateDoc { it.copy(filterPreset = preset) }
    fun updateAdjustments(adj: ColorAdjustments) = mutateDoc { it.copy(adjustments = adj) }

    // Background Removal
    fun toggleBackgroundRemoval(enabled: Boolean) {
        mutateDoc(rebuildBase = true) { it.copy(removeBackground = enabled) }
    }

    fun setBgTolerance(tol: Float) {
        mutateDoc(rebuildBase = true) { it.copy(bgTolerance = tol.coerceIn(0.05f, 0.65f), removeBackground = true) }
    }

    fun setCustomBgColor(hex: Long) {
        mutateDoc(rebuildBase = true) { it.copy(customBgColorHex = hex, removeBackground = true) }
    }

    fun saveEditedImageToGallery(onResult: (String) -> Unit) {
        val doc = _uiState.value.document
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
                displayName = "Panel Komik Paint",
                isImage = true,
                sourceDurationMs = 5000L,
                trimStartMs = 0L,
                trimEndMs = 5000L
            )
            val ratio = when (doc.cropRatio) {
                ImageCropRatio.SQUARE -> AspectRatioPreset.RATIO_1_1
                ImageCropRatio.COMIC -> AspectRatioPreset.RATIO_3_4
                ImageCropRatio.FEED -> AspectRatioPreset.RATIO_4_5
                ImageCropRatio.LANDSCAPE -> AspectRatioPreset.RATIO_16_9
                else -> AspectRatioPreset.RATIO_9_16
            }
            val timeline = ProjectTimelineData(clips = listOf(clip), aspectRatio = ratio)
            val id = repository.createProject("KN Paint Gambar", timeline)
            _uiState.update { it.copy(isBusy = false) }
            onProjectCreated(id)
        }
    }
}
