package com.example.model

import androidx.annotation.DrawableRes
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
enum class AspectRatioPreset(
    val label: String,
    val widthRatio: Float,
    val heightRatio: Float,
    val subtitle: String
) {
    RATIO_9_16("9:16", 9f, 16f, "TikTok / Reels"),
    RATIO_16_9("16:9", 16f, 9f, "YouTube"),
    RATIO_1_1("1:1", 1f, 1f, "Instagram"),
    RATIO_4_5("4:5", 4f, 5f, "Feed IG"),
    RATIO_3_4("3:4", 3f, 4f, "Panel Komik"),
    ORIGINAL("Asli", 9f, 16f, "Original")
}

@Serializable
enum class CanvasFitMode(val label: String) {
    AUTO_FIT("Auto-Fit"),
    BLUR_BG("Blur Latar"),
    CUSTOM_IMAGE("Gambar Latar")
}

@Serializable
enum class FilterPreset(
    val label: String,
    val brightness: Float,
    val contrast: Float,
    val saturation: Float,
    val hueShift: Float,
    val isGrayscale: Boolean = false
) {
    NONE("Asli", 0f, 0f, 0f, 0f),
    KOMIK_POP("Komik Pop", 0.04f, 0.24f, 0.42f, 4f),
    KOMIK_NOIR("Noir Komik", -0.04f, 0.34f, -1f, 0f, isGrayscale = true),
    CINEMATIC("KN Sinematik", -0.05f, 0.22f, -0.14f, -6f),
    WARM_SUN("Oranye Senja", 0.05f, 0.12f, 0.28f, 14f),
    COOL_MINT("Biru Malam", 0.02f, 0.14f, 0.05f, -18f),
    VIVID("Vivid", 0.05f, 0.18f, 0.45f, 0f),
    BW_CLASSIC("B&W", 0.02f, 0.28f, -1f, 0f, isGrayscale = true),
    SEPIA("Retro", -0.02f, 0.14f, -0.35f, 22f)
}

@Serializable
enum class VideoEffectPreset(val label: String) {
    NONE("Tanpa"),
    RETRO("Retro VHS"),
    BLUR("Soft Blur"),
    SPARKLE("Sparkle"),
    SHAKE("Camera Shake")
}

@Serializable
enum class TransitionType(val label: String) {
    NONE("Tanpa"),
    DISSOLVE("Dissolve"),
    ZOOM_IN("Zoom"),
    SLIDE("Slide"),
    PAGE_TURN("Balik Halaman"),
    GLITCH("Glitch"),
    WHIP_PAN("Whip Pan"),
    FADE_BLACK("Fade")
}

@Serializable
enum class SpeedCurvePreset(val label: String, val points: List<Float>) {
    NORMAL("Normal", listOf(1f, 1f, 1f, 1f, 1f)),
    MONTAGE("Montage", listOf(0.6f, 1.4f, 2.2f, 1.2f, 0.7f)),
    HERO("Hero", listOf(1.6f, 2.4f, 0.45f, 0.45f, 2.0f)),
    BULLET("Bullet", listOf(2.8f, 2.5f, 0.35f, 1.5f, 1.0f)),
    JUMP_CUT("Jump Cut", listOf(1.0f, 2.5f, 0.6f, 2.5f, 1.0f));

    fun speedMultiplierAt(progress: Float): Float {
        if (this == NORMAL || points.size < 2) return 1f
        val p = progress.coerceIn(0f, 1f) * (points.size - 1)
        val idx = p.toInt().coerceIn(0, points.size - 2)
        val frac = p - idx
        return points[idx] + (points[idx + 1] - points[idx]) * frac
    }
}

@Serializable
enum class VoiceEffectPreset(val label: String, val pitchMultiplier: Float) {
    NONE("Normal", 1.0f),
    DEEP("Narator Berat", 0.74f),
    CHIPMUNK("Chipmunk", 1.45f),
    ROBOT("Robot", 1.0f),
    STUDIO("Studio Pro", 1.0f),
    ECHO("Echo", 1.0f),
    MEGAPHONE("Megaphone", 1.08f)
}

@Serializable
enum class VocalSeparationMode(val label: String) {
    ORIGINAL("Campuran"),
    VOCAL_ONLY("Vokal / Narasi"),
    INSTRUMENTAL_ONLY("Musik / BGM")
}

enum class TtsVoiceStyle(val label: String, val pitch: Float, val rate: Float) {
    NARATOR_BERAT("Narator Berat", 0.78f, 0.94f),
    EKSPRESIF("Komik Ekspresif", 1.08f, 1.04f),
    TENANG("Story Tenang", 0.94f, 0.88f),
    CEPAT("Rekap Cepat", 1.02f, 1.22f)
}

@Serializable
enum class TextFontStyle(val label: String) {
    MODERN("Outfit Bold"),
    CLEAN("Jakarta Sans"),
    MONO("Komik Mono")
}

@Serializable
enum class TextAnimationType(val label: String) {
    NONE("Tanpa"),
    FADE("Fade In"),
    POP("Pop In"),
    SLIDE_UP("Slide Up"),
    FADE_OUT("Fade Out"),
    PULSE_LOOP("Pulse Loop")
}

@Serializable
enum class BlendModePreset(val label: String) {
    NORMAL("Normal"),
    SCREEN("Screen"),
    MULTIPLY("Multiply"),
    OVERLAY("Overlay"),
    LIGHTEN("Lighten")
}

enum class ExportResolution(val label: String, val heightPx: Int, val badge: String) {
    SD_480("480p Ringan", 480, "480P"),
    HD_720("720p HD (Disarankan)", 720, "720P"),
    FHD_1080("1080p Full HD", 1080, "1080P"),
    QHD_2K("2K QHD", 1440, "2K"),
    UHD_4K("4K Ultra HD", 2160, "4K")
}

enum class ExportFrameRate(val label: String, val fps: Int) {
    FPS_24("24 fps", 24),
    FPS_30("30 fps", 30),
    FPS_60("60 fps", 60)
}

enum class ExportBitrate(val label: String, val bps: Int) {
    SAVER("Hemat (4 Mbps)", 4_000_000),
    STANDARD("Standar (10 Mbps)", 10_000_000),
    HIGH("Tinggi (20 Mbps)", 20_000_000)
}

@Serializable
data class KeyframePoint(
    val id: String = UUID.randomUUID().toString(),
    val timeOffsetMs: Long,
    val xOffset: Float = 0f,
    val yOffset: Float = 0f,
    val scale: Float = 1f,
    val rotationDeg: Float = 0f,
    val opacity: Float = 1f
)

data class KeyframeTransform(
    val xOffset: Float = 0f,
    val yOffset: Float = 0f,
    val scale: Float = 1f,
    val rotationDeg: Float = 0f,
    val opacity: Float = 1f
)

fun interpolateKeyframes(keyframes: List<KeyframePoint>, offsetMs: Long): KeyframeTransform {
    if (keyframes.isEmpty()) return KeyframeTransform()
    val sorted = keyframes.sortedBy { it.timeOffsetMs }
    if (offsetMs <= sorted.first().timeOffsetMs) {
        val k = sorted.first()
        return KeyframeTransform(k.xOffset, k.yOffset, k.scale, k.rotationDeg, k.opacity)
    }
    if (offsetMs >= sorted.last().timeOffsetMs) {
        val k = sorted.last()
        return KeyframeTransform(k.xOffset, k.yOffset, k.scale, k.rotationDeg, k.opacity)
    }
    for (i in 0 until sorted.lastIndex) {
        val a = sorted[i]
        val b = sorted[i + 1]
        if (offsetMs in a.timeOffsetMs..b.timeOffsetMs) {
            val span = (b.timeOffsetMs - a.timeOffsetMs).coerceAtLeast(1L)
            val t = (offsetMs - a.timeOffsetMs).toFloat() / span.toFloat()
            return KeyframeTransform(
                xOffset = a.xOffset + (b.xOffset - a.xOffset) * t,
                yOffset = a.yOffset + (b.yOffset - a.yOffset) * t,
                scale = a.scale + (b.scale - a.scale) * t,
                rotationDeg = a.rotationDeg + (b.rotationDeg - a.rotationDeg) * t,
                opacity = (a.opacity + (b.opacity - a.opacity) * t).coerceIn(0.05f, 1f)
            )
        }
    }
    return KeyframeTransform()
}

@Serializable
data class ChromaKeyConfig(
    val enabled: Boolean = false,
    val keyColorHex: Long = 0xFF00D84A,
    val tolerance: Float = 0.25f,
    val autoRemoveBg: Boolean = false
)

@Serializable
data class VideoClip(
    val id: String = UUID.randomUUID().toString(),
    val uriString: String,
    val displayName: String = "Klip",
    val isImage: Boolean = false,
    val sourceDurationMs: Long = 5000L,
    val trimStartMs: Long = 0L,
    val trimEndMs: Long = 5000L,
    val speed: Float = 1.0f,
    val speedCurve: SpeedCurvePreset = SpeedCurvePreset.NORMAL,
    val smoothSlowMo: Boolean = false,
    val volume: Float = 1.0f,
    val voiceEffect: VoiceEffectPreset = VoiceEffectPreset.NONE,
    val vocalMode: VocalSeparationMode = VocalSeparationMode.ORIGINAL,
    val noiseReduction: Boolean = false,
    val cropScale: Float = 1.0f,
    val rotationDegrees: Int = 0,
    val isMirrored: Boolean = false,
    val isReversed: Boolean = false,
    val transitionOut: TransitionType = TransitionType.NONE,
    val chromaKey: ChromaKeyConfig = ChromaKeyConfig(),
    val keyframes: List<KeyframePoint> = emptyList()
) {
    val trimmedSourceDurationMs: Long
        get() = (trimEndMs - trimStartMs).coerceAtLeast(300L)

    val effectiveSpeedAverage: Float
        get() = if (speedCurve == SpeedCurvePreset.NORMAL) {
            speed.coerceIn(0.1f, 10.0f)
        } else {
            speedCurve.points.average().toFloat().coerceIn(0.25f, 4.0f)
        }

    val effectiveDurationMs: Long
        get() = (trimmedSourceDurationMs / effectiveSpeedAverage).toLong().coerceAtLeast(300L)
}

@Serializable
data class TextOverlayItem(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val startMs: Long = 0L,
    val endMs: Long = 3000L,
    val xFraction: Float = 0.5f,
    val yFraction: Float = 0.5f,
    val fontSizeSp: Float = 24f,
    val colorHex: Long = 0xFFFFFFFF,
    val bgColorHex: Long = 0x9916161A,
    val strokeColorHex: Long = 0xFF000000,
    val strokeEnabled: Boolean = true,
    val hasShadow: Boolean = true,
    val useGradient: Boolean = false,
    val curveDegrees: Float = 0f,
    val isSticker: Boolean = false,
    val fontStyle: TextFontStyle = TextFontStyle.MODERN,
    val animation: TextAnimationType = TextAnimationType.NONE,
    val styleId: String = "classic",
    val keyframes: List<KeyframePoint> = emptyList()
)

@Serializable
data class PipOverlayItem(
    val id: String = UUID.randomUUID().toString(),
    val uriString: String,
    val displayName: String = "Overlay",
    val isImage: Boolean = true,
    val startMs: Long = 0L,
    val endMs: Long = 4000L,
    val xFraction: Float = 0.72f,
    val yFraction: Float = 0.28f,
    val scale: Float = 0.34f,
    val opacity: Float = 1f,
    val blendMode: BlendModePreset = BlendModePreset.NORMAL
)

@Serializable
data class AudioTrackItem(
    val id: String = UUID.randomUUID().toString(),
    val uriString: String,
    val title: String,
    val volume: Float = 0.85f,
    val durationMs: Long = 30000L,
    val fadeIn: Boolean = false,
    val fadeOut: Boolean = false,
    val voiceEffect: VoiceEffectPreset = VoiceEffectPreset.NONE,
    val vocalMode: VocalSeparationMode = VocalSeparationMode.ORIGINAL,
    val noiseReduction: Boolean = false
)

@Serializable
data class ColorAdjustments(
    val brightness: Float = 0f, // -0.5f .. 0.5f
    val contrast: Float = 0f,   // -0.5f .. 0.5f
    val saturation: Float = 0f, // -1.0f .. 1.0f
    val exposure: Float = 0f,   // -0.5f .. 0.5f
    val sharpen: Float = 0f,    // 0f .. 1.0f
    val highlights: Float = 0f, // -0.5f .. 0.5f
    val shadows: Float = 0f,    // -0.5f .. 0.5f
    val vignette: Float = 0f,   // 0f .. 1.0f
    val hue: Float = 0f         // -45f .. 45f
)

@Serializable
data class ProjectTimelineData(
    val clips: List<VideoClip> = emptyList(),
    val textOverlays: List<TextOverlayItem> = emptyList(),
    val pipOverlays: List<PipOverlayItem> = emptyList(),
    val audioTrack: AudioTrackItem? = null,
    val filterPreset: FilterPreset = FilterPreset.NONE,
    val videoEffect: VideoEffectPreset = VideoEffectPreset.NONE,
    val adjustments: ColorAdjustments = ColorAdjustments(),
    val aspectRatio: AspectRatioPreset = AspectRatioPreset.RATIO_9_16,
    val canvasFitMode: CanvasFitMode = CanvasFitMode.AUTO_FIT,
    val canvasBgHex: Long = 0xFF0B0B0D,
    val canvasBgImageUri: String? = null,
    val snapToGrid: Boolean = true,
    val includeWatermark: Boolean = false
) {
    val totalDurationMs: Long
        get() = clips.sumOf { it.effectiveDurationMs }

    fun clipStartPositionMs(clipIndex: Int): Long {
        if (clipIndex <= 0) return 0L
        return clips.take(clipIndex).sumOf { it.effectiveDurationMs }
    }

    fun findClipAt(positionMs: Long): Pair<Int, Long>? {
        if (clips.isEmpty()) return null
        var acc = 0L
        clips.forEachIndexed { index, clip ->
            val end = acc + clip.effectiveDurationMs
            if (positionMs < end || index == clips.lastIndex) {
                val offsetInClip = (positionMs - acc).coerceIn(0L, clip.effectiveDurationMs)
                return index to offsetInClip
            }
            acc = end
        }
        return null
    }
}

data class StudioTemplate(
    val id: String,
    val title: String,
    val subtitle: String,
    val category: String,
    @DrawableRes val coverDrawableRes: Int,
    val clipCountLabel: String,
    val durationLabel: String,
    val filterPreset: FilterPreset,
    val aspectRatio: AspectRatioPreset,
    val introText: String
)
