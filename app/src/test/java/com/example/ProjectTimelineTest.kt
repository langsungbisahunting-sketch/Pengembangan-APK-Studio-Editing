package com.example

import com.example.model.AspectRatioPreset
import com.example.model.BlendModePreset
import com.example.model.CanvasFitMode
import com.example.model.ChromaKeyConfig
import com.example.model.FilterPreset
import com.example.model.KeyframePoint
import com.example.model.PipOverlayItem
import com.example.model.ProjectTimelineData
import com.example.model.SpeedCurvePreset
import com.example.model.TextAnimationType
import com.example.model.TextFontStyle
import com.example.model.TextOverlayItem
import com.example.model.TransitionType
import com.example.model.VideoClip
import com.example.model.VideoEffectPreset
import com.example.model.VocalSeparationMode
import com.example.model.VoiceEffectPreset
import com.example.model.interpolateKeyframes
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectTimelineTest {

    @Test
    fun `clip effective duration respects trim and speed`() {
        val clip = VideoClip(
            uriString = "content://media/1",
            sourceDurationMs = 10_000L,
            trimStartMs = 2_000L,
            trimEndMs = 8_000L,
            speed = 2.0f
        )
        assertEquals(6_000L, clip.trimmedSourceDurationMs)
        assertEquals(3_000L, clip.effectiveDurationMs)
    }

    @Test
    fun `timeline finds clip index and local offset accurately`() {
        val clip1 = VideoClip(uriString = "content://1", sourceDurationMs = 4000L, trimEndMs = 4000L)
        val clip2 = VideoClip(uriString = "content://2", sourceDurationMs = 6000L, trimEndMs = 6000L, speed = 2.0f)
        val timeline = ProjectTimelineData(clips = listOf(clip1, clip2))

        assertEquals(7000L, timeline.totalDurationMs)
        assertEquals(4000L, timeline.clipStartPositionMs(1))

        val slot1 = timeline.findClipAt(2500L)
        assertNotNull(slot1)
        assertEquals(0, slot1!!.first)
        assertEquals(2500L, slot1.second)

        val slot2 = timeline.findClipAt(5200L)
        assertNotNull(slot2)
        assertEquals(1, slot2!!.first)
        assertEquals(1200L, slot2.second)
    }

    @Test
    fun `interpolateKeyframes smoothly interpolates scale rotation and opacity`() {
        val keyframes = listOf(
            KeyframePoint(timeOffsetMs = 1000L, xOffset = 0f, scale = 1.0f, rotationDeg = 0f, opacity = 1.0f),
            KeyframePoint(timeOffsetMs = 3000L, xOffset = 100f, scale = 2.0f, rotationDeg = 90f, opacity = 0.5f)
        )

        val before = interpolateKeyframes(keyframes, 500L)
        assertEquals(1.0f, before.scale, 0.001f)

        val mid = interpolateKeyframes(keyframes, 2000L)
        assertEquals(50f, mid.xOffset, 0.001f)
        assertEquals(1.5f, mid.scale, 0.001f)
        assertEquals(45f, mid.rotationDeg, 0.001f)
        assertEquals(0.75f, mid.opacity, 0.001f)

        val after = interpolateKeyframes(keyframes, 5000L)
        assertEquals(2.0f, after.scale, 0.001f)
        assertEquals(90f, after.rotationDeg, 0.001f)
    }

    @Test
    fun `speed curve preset returns valid interpolated multipliers`() {
        assertEquals(1.0f, SpeedCurvePreset.NORMAL.speedMultiplierAt(0.5f), 0.001f)
        val midHero = SpeedCurvePreset.HERO.speedMultiplierAt(0.5f)
        assertTrue(midHero < 1.0f) // slow-mo dip in Hero curve
        val startBullet = SpeedCurvePreset.BULLET.speedMultiplierAt(0.0f)
        assertTrue(startBullet > 2.0f)
    }

    @Test
    fun `timeline serialization round-trips filters overlays pip chroma and keyframes`() {
        val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
        val original = ProjectTimelineData(
            clips = listOf(
                VideoClip(
                    uriString = "content://clip/1",
                    sourceDurationMs = 5000L,
                    trimEndMs = 5000L,
                    speedCurve = SpeedCurvePreset.MONTAGE,
                    voiceEffect = VoiceEffectPreset.STUDIO,
                    vocalMode = VocalSeparationMode.VOCAL_ONLY,
                    transitionOut = TransitionType.GLITCH,
                    chromaKey = ChromaKeyConfig(enabled = true, autoRemoveBg = true),
                    keyframes = listOf(
                        KeyframePoint(timeOffsetMs = 0L, scale = 1.0f),
                        KeyframePoint(timeOffsetMs = 2500L, scale = 1.25f)
                    )
                )
            ),
            textOverlays = listOf(
                TextOverlayItem(
                    text = "Halo Studio",
                    startMs = 500L,
                    endMs = 2500L,
                    fontStyle = TextFontStyle.MONO,
                    animation = TextAnimationType.POP
                )
            ),
            pipOverlays = listOf(
                PipOverlayItem(
                    uriString = "content://pip/1",
                    blendMode = BlendModePreset.SCREEN
                )
            ),
            filterPreset = FilterPreset.CINEMATIC,
            videoEffect = VideoEffectPreset.SPARKLE,
            aspectRatio = AspectRatioPreset.RATIO_16_9,
            canvasFitMode = CanvasFitMode.BLUR_BG
        )
        val encoded = json.encodeToString(ProjectTimelineData.serializer(), original)
        val decoded = json.decodeFromString<ProjectTimelineData>(encoded)
        assertEquals(original, decoded)
    }
}
