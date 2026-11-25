package com.example.media

import android.graphics.Color
import android.graphics.Matrix
import android.text.SpannableString
import android.text.Spanned
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import androidx.media3.common.Effect
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Brightness
import androidx.media3.effect.Contrast
import androidx.media3.effect.HslAdjustment
import androidx.media3.effect.MatrixTransformation
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.RgbFilter
import androidx.media3.effect.ScaleAndRotateTransformation
import androidx.media3.effect.StaticOverlaySettings
import androidx.media3.effect.TextOverlay
import androidx.media3.effect.TextureOverlay
import com.example.model.ColorAdjustments
import com.example.model.FilterPreset
import com.example.model.TextOverlayItem
import com.example.model.VideoClip
import com.example.model.VideoEffectPreset
import com.example.model.interpolateKeyframes
import kotlin.math.abs
import kotlin.math.sin

@UnstableApi
object Media3EffectsFactory {

    fun buildClipVideoEffects(
        clip: VideoClip,
        filter: FilterPreset,
        videoEffect: VideoEffectPreset,
        adjustments: ColorAdjustments
    ): List<Effect> {
        val effects = mutableListOf<Effect>()

        // Crop zoom, Rotation & Mirror
        if (clip.rotationDegrees != 0 || clip.isMirrored || abs(clip.cropScale - 1f) > 0.02f) {
            val builder = ScaleAndRotateTransformation.Builder()
            if (clip.rotationDegrees != 0) {
                builder.setRotationDegrees(clip.rotationDegrees.toFloat())
            }
            val crop = clip.cropScale.coerceIn(1f, 2.5f)
            val scaleX = crop * (if (clip.isMirrored) -1f else 1f)
            if (scaleX != 1f || crop != 1f) {
                builder.setScale(scaleX, crop)
            }
            effects.add(builder.build())
        }

        // Keyframe animation matrix transformation for export & preview
        if (clip.keyframes.isNotEmpty()) {
            effects.add(
                MatrixTransformation { presentationTimeUs ->
                    val offsetMs = presentationTimeUs / 1000L
                    val kf = interpolateKeyframes(clip.keyframes, offsetMs)
                    Matrix().apply {
                        postScale(kf.scale, kf.scale)
                        postRotate(kf.rotationDeg)
                        postTranslate(kf.xOffset * 2f, -kf.yOffset * 2f)
                    }
                }
            )
        }

        // Camera Shake visual effect
        if (videoEffect == VideoEffectPreset.SHAKE) {
            effects.add(
                MatrixTransformation { presentationTimeUs ->
                    val t = presentationTimeUs / 1_000_000f
                    val dx = sin(t * 28f) * 0.03f
                    val dy = sin(t * 34f + 1.1f) * 0.03f
                    Matrix().apply {
                        postScale(1.06f, 1.06f)
                        postTranslate(dx, dy)
                    }
                }
            )
        }

        // Combined Filter + Exposure + Highlights/Shadows + Contrast + Saturation + Hue + Retro
        val retroContrast = if (videoEffect == VideoEffectPreset.RETRO) 0.14f else 0f
        val retroSat = if (videoEffect == VideoEffectPreset.RETRO) -0.30f else 0f
        val retroHue = if (videoEffect == VideoEffectPreset.RETRO) 18f else 0f

        val toneLift = adjustments.shadows * 0.35f + adjustments.highlights * 0.30f
        val totalBrightness = (filter.brightness + adjustments.brightness + adjustments.exposure * 0.8f + toneLift)
            .coerceIn(-0.9f, 0.9f)
        val totalContrast = (filter.contrast + adjustments.contrast + retroContrast + adjustments.sharpen * 0.12f - adjustments.shadows * 0.15f)
            .coerceIn(-0.9f, 0.9f)
        val totalSaturation = (filter.saturation + adjustments.saturation + retroSat)
            .coerceIn(-1f, 1f)
        val totalHue = (filter.hueShift + adjustments.hue + retroHue)
            .coerceIn(-180f, 180f)

        if (filter.isGrayscale) {
            effects.add(RgbFilter.createGrayscaleFilter())
        } else if (abs(totalSaturation) > 0.02f || abs(totalHue) > 0.5f) {
            effects.add(
                HslAdjustment.Builder()
                    .adjustSaturation(totalSaturation * 100f)
                    .adjustHue(totalHue)
                    .build()
            )
        }

        if (abs(totalBrightness) > 0.01f) {
            effects.add(Brightness(totalBrightness))
        }

        if (abs(totalContrast) > 0.01f) {
            effects.add(Contrast(totalContrast))
        }

        return effects
    }

    fun buildTextOverlayEffect(
        overlays: List<TextOverlayItem>,
        includeWatermark: Boolean = false
    ): OverlayEffect? {
        if (overlays.isEmpty() && !includeWatermark) return null
        val textOverlays: MutableList<TextureOverlay> = overlays.map { item ->
            val span = SpannableString(item.text).apply {
                setSpan(
                    ForegroundColorSpan(item.colorHex.toInt()),
                    0,
                    length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                val bgAlpha = Color.alpha(item.bgColorHex.toInt())
                if (bgAlpha > 0) {
                    setSpan(
                        BackgroundColorSpan(item.bgColorHex.toInt()),
                        0,
                        length,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }
            val ndcX = (item.xFraction * 2f - 1f).coerceIn(-0.85f, 0.85f)
            val ndcY = (1f - item.yFraction * 2f).coerceIn(-0.85f, 0.85f)
            val settings = StaticOverlaySettings.Builder()
                .setOverlayFrameAnchor(0f, 0f)
                .setBackgroundFrameAnchor(ndcX, ndcY)
                .build()
            TextOverlay.createStaticTextOverlay(span, settings) as TextureOverlay
        }.toMutableList()

        if (includeWatermark) {
            val wm = SpannableString("Dibuat dengan Komik Narasi").apply {
                setSpan(ForegroundColorSpan(0xFFFFC107.toInt()), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                setSpan(BackgroundColorSpan(0x9916161A.toInt()), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            val wmSettings = StaticOverlaySettings.Builder()
                .setOverlayFrameAnchor(1f, -1f)
                .setBackgroundFrameAnchor(0.92f, -0.90f)
                .build()
            textOverlays.add(TextOverlay.createStaticTextOverlay(wm, wmSettings))
        }

        return OverlayEffect(textOverlays)
    }
}
