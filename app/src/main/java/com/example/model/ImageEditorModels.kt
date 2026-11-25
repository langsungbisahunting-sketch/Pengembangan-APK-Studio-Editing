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

enum class ImageEditorTool(val label: String) {
    TEXT("Tulisan"),
    CROP_TRANSFORM("Pangkas & Putar"),
    FILTER("Filter"),
    ADJUST("Sesuaikan"),
    BRUSH("Kuas"),
    STICKER("Stiker"),
    BG_REMOVE("Hapus Latar")
}

data class NormalizedPoint(
    val x: Float,
    val y: Float
)

data class ImageBrushStroke(
    val id: String = UUID.randomUUID().toString(),
    val points: List<NormalizedPoint>,
    val colorHex: Long = 0xFFFF6B00,
    val strokeWidthDp: Float = 6f,
    val isEraser: Boolean = false
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
    val cropRatio: ImageCropRatio = ImageCropRatio.FREE,
    val rotationDegrees: Int = 0,
    val flipHorizontal: Boolean = false,
    val flipVertical: Boolean = false,
    val filterPreset: FilterPreset = FilterPreset.NONE,
    val adjustments: ColorAdjustments = ColorAdjustments(),
    val removeBackground: Boolean = false,
    val bgTolerance: Float = 0.24f,
    val customBgColorHex: Long = 0x00000000,
    val strokes: List<ImageBrushStroke> = emptyList(),
    val textLayers: List<ImageTextLayer> = emptyList(),
    val stickers: List<ImageStickerLayer> = emptyList()
)
