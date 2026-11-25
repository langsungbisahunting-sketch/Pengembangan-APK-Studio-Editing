package com.example

import com.example.model.AspectRatioPreset
import com.example.model.AudioTrackItem
import com.example.model.AudioTrackRole
import com.example.model.BlendModePreset
import com.example.model.CanvasFitMode
import com.example.model.ChromaKeyConfig
import com.example.model.ColorAdjustments
import com.example.model.FilterPreset
import com.example.model.ImageCropRatio
import com.example.model.ImageEditorDocument
import com.example.model.ImageTextLayer
import com.example.model.KeyframePoint
import com.example.model.PanZoomPreset
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
        assertTrue(midHero < 1.0f)
        val startBullet = SpeedCurvePreset.BULLET.speedMultiplierAt(0.0f)
        assertTrue(startBullet > 2.0f)
    }

    @Test
    fun `dual audio track effectiveDurationMs respects trim bounds`() {
        val vocal = AudioTrackItem(
            uriString = "file:///vocal.m4a",
            title = "Narasi Vokal",
            role = AudioTrackRole.VOCAL,
            durationMs = 12_000L,
            startOffsetMs = 1_500L,
            trimStartMs = 2_000L,
            trimEndMs = 9_500L
        )
        assertEquals(7_500L, vocal.effectiveDurationMs)
        assertEquals(AudioTrackRole.VOCAL, vocal.role)
    }

    @Test
    fun `image editor document holds touch-first layers and crop ratio`() {
        val doc = ImageEditorDocument(
            sourceUriString = "content://img/panel.png",
            cropRatio = ImageCropRatio.COMIC,
            rotationDegrees = 90,
            flipHorizontal = true,
            removeBackground = true,
            textLayers = listOf(ImageTextLayer(text = "Babak 1", scale = 1.4f, rotationDeg = -12f))
        )
        assertEquals(ImageCropRatio.COMIC, doc.cropRatio)
        assertEquals(90, doc.rotationDegrees)
        assertTrue(doc.flipHorizontal)
        assertTrue(doc.removeBackground)
        assertEquals("Babak 1", doc.textLayers.first().text)
    }

    @Test
    fun `timeline serialization round-trips komik narasi dual audio and touch upgrades`() {
        val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
        val original = ProjectTimelineData(
            clips = listOf(
                VideoClip(
                    uriString = "content://clip/1",
                    sourceDurationMs = 5000L,
                    trimEndMs = 5000L,
                    speedCurve = SpeedCurvePreset.MONTAGE,
                    smoothSlowMo = true,
                    cropScale = 1.35f,
                    userScale = 1.25f,
                    posX = 0.1f,
                    posY = -0.05f,
                    freeRotationDeg = 12f,
                    panZoom = PanZoomPreset.ZOOM_IN,
                    isReversed = true,
                    voiceEffect = VoiceEffectPreset.MEGAPHONE,
                    vocalMode = VocalSeparationMode.VOCAL_ONLY,
                    transitionOut = TransitionType.PAGE_TURN,
                    chromaKey = ChromaKeyConfig(enabled = true, autoRemoveBg = true),
                    keyframes = listOf(
                        KeyframePoint(timeOffsetMs = 0L, scale = 1.0f),
                        KeyframePoint(timeOffsetMs = 2500L, scale = 1.25f)
                    )
                )
            ),
            textOverlays = listOf(
                TextOverlayItem(
                    text = "LEDAAK!",
                    originalText = "BOOM!",
                    startMs = 500L,
                    endMs = 2500L,
                    scale = 1.3f,
                    rotationDeg = -8f,
                    strokeEnabled = true,
                    useGradient = true,
                    curveDegrees = -6f,
                    isSticker = true,
                    fontStyle = TextFontStyle.MONO,
                    animation = TextAnimationType.POP
                )
            ),
            pipOverlays = listOf(
                PipOverlayItem(
                    uriString = "content://pip/1",
                    rotationDeg = 15f,
                    blendMode = BlendModePreset.DARKEN,
                    chromaEnabled = true
                )
            ),
            vocalTrack = AudioTrackItem(
                uriString = "file:///vokal.m4a",
                title = "Vokal Komik",
                role = AudioTrackRole.VOCAL,
                durationMs = 8000L,
                trimEndMs = 8000L
            ),
            musicTrack = AudioTrackItem(
                uriString = "file:///musik.m4a",
                title = "Musik Epik",
                role = AudioTrackRole.MUSIC,
                durationMs = 10000L,
                trimEndMs = 10000L,
                autoDucking = true
            ),
            filterPreset = FilterPreset.KOMIK_POP,
            videoEffect = VideoEffectPreset.SPARKLE,
            adjustments = ColorAdjustments(highlights = 0.15f, shadows = -0.1f),
            aspectRatio = AspectRatioPreset.RATIO_3_4,
            canvasFitMode = CanvasFitMode.CUSTOM_IMAGE,
            canvasBgImageUri = "content://bg/komik.png",
            snapToGrid = true,
            includeWatermark = true
        )
        val encoded = json.encodeToString(ProjectTimelineData.serializer(), original)
        val decoded = json.decodeFromString<ProjectTimelineData>(encoded)
        assertEquals(original, decoded)
    }
}
