package com.example.model

import java.util.UUID

enum class ImageCropRatio(val label: String, val ratio: Float?) {
    FREE("Bebas", null),
    SQUARE("1:1", 1f),
    COMIC("3:4", 3f / 4f),
    FEED("4:5", 4f / 5f),
    PORTRAIT("9:16", 9f / 16f),
    LANDSCAPE("16:9", 16f / 9f)
}

enum class ImageExportFitMode(val label: String) {
    FIT_CANVAS("Utuh + Latar"),
    CENTER_CROP("Potong Tengah")
}

enum class ImageEditorTool(val label: String) {
    PAINT("Kuas & Cat"),
    SHAPES("Bentuk"),
    OVERLAY_IMAGE("Timpa Gambar"),
    CROP_TRANSFORM("Pangkas & Putar"),
    TEXT("Tulisan"),
    STICKER("Stiker"),
    FILTER("Filter"),
    ADJUST("Sesuaikan"),
    BG_REMOVE("Hapus Latar")
}

enum class PaintBrushType(val label: String) {
    PENCIL("Pensil"),
    BRUSH("Kuas"),
    MARKER("Spidol"),
    AIRBRUSH("Semprot"),
    BUCKET_FILL("Ember Cat"),
    EYEDROPPER("Pipet"),
    ERASER("Penghapus")
}

enum class PaintShapeType(val label: String) {
    LINE("Garis"),
    ARROW("Panah"),
    RECTANGLE("Kotak"),
    OVAL("Lingkaran"),
    TRIANGLE("Segitiga"),
    STAR("Bintang"),
    BUBBLE("Balon")
}

data class NormalizedPoint(
    val x: Float,
    val y: Float
)

data class ImageCropRect(
    val left: Float = 0f,
    val top: Float = 0f,
    val right: Float = 1f,
    val bottom: Float = 1f
) {
    val width: Float get() = (right - left).coerceAtLeast(0.08f)
    val height: Float get() = (bottom - top).coerceAtLeast(0.08f)
    val isFull: Boolean get() = left <= 0.005f && top <= 0.005f && right >= 0.995f && bottom >= 0.995f
}

data class ImageBrushStroke(
    val id: String = UUID.randomUUID().toString(),
    val points: List<NormalizedPoint>,
    val colorHex: Long = 0xFFFF6B00,
    val strokeWidthDp: Float = 6f,
    val brushType: PaintBrushType = PaintBrushType.BRUSH,
    val alpha: Float = 1f,
    val isEraser: Boolean = false
)

data class ImageFloodFillOp(
    val id: String = UUID.randomUUID().toString(),
    val xFraction: Float,
    val yFraction: Float,
    val colorHex: Long,
    val tolerance: Int = 38
)

data class ImageShapeLayer(
    val id: String = UUID.randomUUID().toString(),
    val shapeType: PaintShapeType,
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float,
    val colorHex: Long = 0xFFFF6B00,
    val strokeWidthDp: Float = 4f,
    val filled: Boolean = false,
    val alpha: Float = 1f
)

data class ImageOverlayLayer(
    val id: String = UUID.randomUUID().toString(),
    val uriString: String,
    val displayName: String = "Gambar Timpa",
    val xFraction: Float = 0.5f,
    val yFraction: Float = 0.5f,
    val scale: Float = 0.42f,
    val rotationDeg: Float = 0f,
    val opacity: Float = 1f,
    val blendMode: BlendModePreset = BlendModePreset.NORMAL,
    val flipHorizontal: Boolean = false
)

data class ImageTextLayer(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val xFraction: Float = 0.5f,
    val yFraction: Float = 0.5f,
    val scale: Float = 1.0f,
    val rotationDeg: Float = 0f,
    val colorHex: Long = 0xFFFFFFFF,
    val bgColorHex: Long = 0xCC16161A,
    val strokeEnabled: Boolean = true,
    val hasShadow: Boolean = true,
    val useBubble: Boolean = true,
    val fontStyle: TextFontStyle = TextFontStyle.MODERN
)

data class ImageStickerLayer(
    val id: String = UUID.randomUUID().toString(),
    val label: String,
    val xFraction: Float = 0.5f,
    val yFraction: Float = 0.35f,
    val scale: Float = 1.0f,
    val rotationDeg: Float = 0f,
    val bgHex: Long = 0xFFFFC107,
    val fgHex: Long = 0xFF1A1200,
    val isBubble: Boolean = true
)

data class ImageEditorDocument(
    val sourceUriString: String? = null,
    val isBlankCanvas: Boolean = true,
    val blankCanvasWidth: Int = 1080,
    val blankCanvasHeight: Int = 1080,
    val canvasBaseColorHex: Long = 0xFFFFFFFF,
    val cropRatio: ImageCropRatio = ImageCropRatio.FREE,
    val appliedCropRect: ImageCropRect = ImageCropRect(),
    val rotationDegrees: Int = 0,
    val flipHorizontal: Boolean = false,
    val flipVertical: Boolean = false,
    val filterPreset: FilterPreset = FilterPreset.NONE,
    val adjustments: ColorAdjustments = ColorAdjustments(),
    val removeBackground: Boolean = false,
    val bgTolerance: Float = 0.24f,
    val customBgColorHex: Long = 0x00000000,
    val floodFills: List<ImageFloodFillOp> = emptyList(),
    val strokes: List<ImageBrushStroke> = emptyList(),
    val shapes: List<ImageShapeLayer> = emptyList(),
    val overlayImages: List<ImageOverlayLayer> = emptyList(),
    val textLayers: List<ImageTextLayer> = emptyList(),
    val stickers: List<ImageStickerLayer> = emptyList()
)
