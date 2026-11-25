package com.example.ui.editor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.BlendModePreset
import com.example.model.ChromaKeyConfig
import com.example.model.PipOverlayItem
import com.example.model.TextAnimationType
import com.example.model.TextFontStyle
import com.example.model.TextOverlayItem
import com.example.model.TtsVoiceStyle
import com.example.model.VideoEffectPreset
import com.example.model.VocalSeparationMode
import com.example.model.VoiceEffectPreset
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.EnergyOrange
import com.example.ui.theme.EnergyOrangeSoft
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldSoft
import com.example.ui.theme.PureWhite
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCard
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.EditorUiState

@Composable
fun AudioAndVoiceInspector(
    uiState: EditorUiState,
    onPickAudio: () -> Unit,
    onExtractAudio: () -> Unit,
    onAudioVolumeChange: (Float) -> Unit,
    onAudioVoiceEffectChange: (VoiceEffectPreset) -> Unit,
    onAudioVocalModeChange: (VocalSeparationMode) -> Unit,
    onToggleAudioNoiseReduction: () -> Unit,
    onClipVoiceEffectChange: (VoiceEffectPreset) -> Unit,
    onClipVocalModeChange: (VocalSeparationMode) -> Unit,
    onToggleClipNoiseReduction: () -> Unit,
    onToggleFade: (Boolean?, Boolean?) -> Unit,
    onRemoveAudio: () -> Unit,
    onSynthesizeTtsNarration: (String, TtsVoiceStyle) -> Unit,
    onPolishNarrationScript: (String, (String) -> Unit) -> Unit
) {
    val audio = uiState.timeline.audioTrack
    val clip = uiState.selectedClip
    val activeVoiceEffect = audio?.voiceEffect ?: clip?.voiceEffect ?: VoiceEffectPreset.NONE
    val activeVocalMode = audio?.vocalMode ?: clip?.vocalMode ?: VocalSeparationMode.ORIGINAL
    val activeNoise = audio?.noiseReduction ?: clip?.noiseReduction ?: false

    var ttsScript by remember { mutableStateOf("") }
    var ttsStyle by remember { mutableStateOf(TtsVoiceStyle.NARATOR_BERAT) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(235.dp)
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onPickAudio,
                colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = Color(0xFF1A1200)),
                modifier = Modifier
                    .weight(1f)
                    .testTag("audio_pick_button")
            ) {
                Icon(Icons.Filled.MusicNote, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (audio == null) "Pilih Musik" else "Ganti Musik", fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
                onClick = onExtractAudio,
                border = BorderStroke(1.dp, EnergyOrange),
                modifier = Modifier
                    .weight(1f)
                    .testTag("audio_extract_button")
            ) {
                Icon(Icons.Filled.Audiotrack, null, tint = EnergyOrange, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Ekstrak Video", color = EnergyOrange)
            }
        }

        // Text-to-Speech (Narasi Suara Otomatis untuk Komik Narasi)
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = StudioCard,
            border = BorderStroke(1.dp, EnergyOrange.copy(alpha = 0.45f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Filled.RecordVoiceOver, null, tint = EnergyOrange, modifier = Modifier.size(16.dp))
                    Text(
                        text = "Narasi Suara Otomatis (TTS Komik)",
                        style = MaterialTheme.typography.labelLarge,
                        color = PureWhite,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedTextField(
                    value = ttsScript,
                    onValueChange = { ttsScript = it },
                    placeholder = { Text("Tulis naskah cerita komik narasi di sini...") },
                    minLines = 2,
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tts_script_input")
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(TtsVoiceStyle.entries) { style ->
                        val selected = style == ttsStyle
                        FilterChip(
                            selected = selected,
                            onClick = { ttsStyle = style },
                            label = { Text(style.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EnergyOrangeSoft,
                                selectedLabelColor = EnergyOrange
                            )
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            onPolishNarrationScript(ttsScript) { polished ->
                                ttsScript = polished
                            }
                        },
                        border = BorderStroke(1.dp, GoldAccent),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("tts_polish_ai_button")
                    ) {
                        Icon(Icons.Filled.AutoAwesome, null, tint = GoldAccent, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Rapikan AI", color = GoldAccent)
                    }
                    Button(
                        onClick = { onSynthesizeTtsNarration(ttsScript, ttsStyle) },
                        colors = ButtonDefaults.buttonColors(containerColor = EnergyOrange, contentColor = PureWhite),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("tts_generate_button")
                    ) {
                        Icon(Icons.Filled.RecordVoiceOver, null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Buat Suara", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (audio != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Trek A1: ${audio.title}", style = MaterialTheme.typography.labelLarge, color = GoldAccent, modifier = Modifier.weight(1f))
                IconButton(onClick = onRemoveAudio, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.DeleteOutline, contentDescription = "Hapus Audio", tint = CrimsonAlert)
                }
            }
            Text("Volume Musik / Narasi: ${(audio.volume * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            Slider(
                value = audio.volume,
                onValueChange = onAudioVolumeChange,
                valueRange = 0f..1f,
                colors = SliderDefaults.colors(thumbColor = GoldAccent, activeTrackColor = GoldAccent)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Fade In", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Switch(
                        checked = audio.fadeIn,
                        onCheckedChange = { onToggleFade(it, null) },
                        colors = SwitchDefaults.colors(checkedThumbColor = PureWhite, checkedTrackColor = EnergyOrange)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Fade Out", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Switch(
                        checked = audio.fadeOut,
                        onCheckedChange = { onToggleFade(null, it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = PureWhite, checkedTrackColor = EnergyOrange)
                    )
                }
            }
        }

        Text("Pemisah Vokal & Musik (AI DSP)", style = MaterialTheme.typography.labelLarge, color = EnergyOrange)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(VocalSeparationMode.entries) { mode ->
                val selected = mode == activeVocalMode
                FilterChip(
                    selected = selected,
                    onClick = {
                        if (audio != null) onAudioVocalModeChange(mode) else onClipVocalModeChange(mode)
                    },
                    label = { Text(mode.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = EnergyOrangeSoft,
                        selectedLabelColor = EnergyOrange
                    )
                )
            }
        }

        Text("Efek Pengubah Suara (Voice FX)", style = MaterialTheme.typography.labelLarge, color = GoldAccent)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(VoiceEffectPreset.entries) { fx ->
                val selected = fx == activeVoiceEffect
                FilterChip(
                    selected = selected,
                    onClick = {
                        if (audio != null) onAudioVoiceEffectChange(fx) else onClipVoiceEffectChange(fx)
                    },
                    label = { Text(fx.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = GoldSoft,
                        selectedLabelColor = GoldAccent
                    )
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Peredam Kebisingan (Noise Reduction)", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
            Switch(
                checked = activeNoise,
                onCheckedChange = {
                    if (audio != null) onToggleAudioNoiseReduction() else onToggleClipNoiseReduction()
                },
                colors = SwitchDefaults.colors(checkedThumbColor = PureWhite, checkedTrackColor = EnergyOrange)
            )
        }
    }
}

private data class TextStyleChoice(
    val id: String,
    val label: String,
    val fgHex: Long,
    val bgHex: Long,
    val useGradient: Boolean = false
)

@Composable
fun TextAndAiCaptionInspector(
    uiState: EditorUiState,
    onAddText: (String, Long, Long, String, Boolean, Boolean) -> Unit,
    onUpdateSelected: ((TextOverlayItem) -> TextOverlayItem) -> Unit,
    onDeleteSelected: () -> Unit,
    onGenerateAutoCaptions: (String) -> Unit,
    onTranslateCaptions: (String) -> Unit
) {
    val selected = uiState.selectedTextOverlay
    var inputText by remember(selected?.id) { mutableStateOf(selected?.text ?: "") }
    val styles = remember {
        listOf(
            TextStyleChoice("classic", "Klasik", 0xFFFFFFFF, 0xCC16161A, false),
            TextStyleChoice("kn_orange", "KN Oranye", 0xFFFFFFFF, 0xFFFF6B00, true),
            TextStyleChoice("kn_gold", "Komik Emas", 0xFF1A1200, 0xFFFFC107, false),
            TextStyleChoice("clean", "Putih Polos", 0xFFFFFFFF, 0x00000000, false)
        )
    }
    val comicStickers = listOf("BOOM!", "BERSAMBUNG...", "BABAK BARU", "WHOSH!")
    val translateLanguages = listOf("English", "Indonesian", "Japanese", "Korean", "Arabic", "Spanish")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(235.dp)
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // AI Auto-Caption Row
        Button(
            onClick = { onGenerateAutoCaptions("Indonesian or English") },
            colors = ButtonDefaults.buttonColors(containerColor = EnergyOrange, contentColor = PureWhite),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("ai_auto_caption_button")
        ) {
            Icon(Icons.Filled.ClosedCaption, null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Auto-Caption AI (Speech-to-Text)", fontWeight = FontWeight.Bold)
        }

        if (uiState.timeline.textOverlays.isNotEmpty()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Filled.Translate, null, tint = GoldAccent, modifier = Modifier.size(16.dp))
                Text("Terjemahkan AI:", style = MaterialTheme.typography.labelMedium, color = GoldAccent)
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(translateLanguages) { lang ->
                    FilterChip(
                        selected = false,
                        onClick = { onTranslateCaptions(lang) },
                        label = { Text(lang) },
                        modifier = Modifier.testTag("ai_translate_${lang.lowercase()}")
                    )
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { value ->
                    inputText = value
                    if (selected != null) {
                        onUpdateSelected { it.copy(text = value) }
                    }
                },
                placeholder = { Text("Tulis teks atau dialog komik...") },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("text_overlay_input")
            )
            if (selected == null) {
                Button(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            onAddText(inputText, 0xFFFFFFFF, 0xCC16161A, "classic", false, false)
                            inputText = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EnergyOrange, contentColor = PureWhite),
                    modifier = Modifier.testTag("text_overlay_add_button")
                ) {
                    Icon(Icons.Filled.Add, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Tambah")
                }
            } else {
                IconButton(onClick = onDeleteSelected) {
                    Icon(Icons.Filled.DeleteOutline, contentDescription = "Hapus Teks", tint = CrimsonAlert)
                }
            }
        }

        Text("Gaya Teks Komik Narasi", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(styles) { style ->
                val active = selected?.styleId == style.id
                Surface(
                    onClick = {
                        if (selected != null) {
                            onUpdateSelected {
                                it.copy(
                                    colorHex = style.fgHex,
                                    bgColorHex = style.bgHex,
                                    styleId = style.id,
                                    useGradient = style.useGradient
                                )
                            }
                        } else if (inputText.isNotBlank()) {
                            onAddText(inputText, style.fgHex, style.bgHex, style.id, style.useGradient, false)
                            inputText = ""
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    color = if (style.bgHex == 0x00000000L) StudioCard else Color(style.bgHex),
                    border = BorderStroke(1.5.dp, if (active) GoldAccent else StudioBorder)
                ) {
                    Text(
                        text = style.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(style.fgHex),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                    )
                }
            }
        }

        Text("Stiker & Balon Komik Cepat", style = MaterialTheme.typography.labelSmall, color = GoldAccent)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(comicStickers) { sticker ->
                Surface(
                    onClick = {
                        onAddText(sticker, 0xFF1A1200, 0xFFFFC107, "kn_gold", false, true)
                    },
                    shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomEnd = 12.dp, bottomStart = 2.dp),
                    color = GoldSoft,
                    border = BorderStroke(1.dp, GoldAccent)
                ) {
                    Text(
                        text = sticker,
                        style = MaterialTheme.typography.labelMedium,
                        color = GoldAccent,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        if (selected != null) {
            Text("Font, Animasi & Efek Teks", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(TextFontStyle.entries) { font ->
                    FilterChip(
                        selected = selected.fontStyle == font,
                        onClick = { onUpdateSelected { it.copy(fontStyle = font) } },
                        label = { Text(font.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EnergyOrangeSoft,
                            selectedLabelColor = EnergyOrange
                        )
                    )
                }
                items(TextAnimationType.entries) { anim ->
                    FilterChip(
                        selected = selected.animation == anim,
                        onClick = { onUpdateSelected { it.copy(animation = anim) } },
                        label = { Text("Anim: ${anim.label}") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EnergyOrangeSoft,
                            selectedLabelColor = EnergyOrange
                        )
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selected.useGradient,
                    onClick = { onUpdateSelected { it.copy(useGradient = !it.useGradient) } },
                    label = { Text("Gradasi") }
                )
                FilterChip(
                    selected = selected.strokeEnabled,
                    onClick = { onUpdateSelected { it.copy(strokeEnabled = !it.strokeEnabled) } },
                    label = { Text("Stroke") }
                )
                FilterChip(
                    selected = selected.hasShadow,
                    onClick = { onUpdateSelected { it.copy(hasShadow = !it.hasShadow) } },
                    label = { Text("Bayangan") }
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Miring / Lengkungan ${selected.curveDegrees.toInt()}°",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    modifier = Modifier.width(120.dp)
                )
                Slider(
                    value = selected.curveDegrees,
                    onValueChange = { deg -> onUpdateSelected { it.copy(curveDegrees = deg) } },
                    valueRange = -20f..20f,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(thumbColor = EnergyOrange, activeTrackColor = EnergyOrange)
                )
            }
        }
    }
}

@Composable
fun PipOverlayInspector(
    uiState: EditorUiState,
    onPickPip: () -> Unit,
    onUpdateSelectedPip: ((PipOverlayItem) -> PipOverlayItem) -> Unit,
    onDeleteSelectedPip: () -> Unit
) {
    val selected = uiState.selectedPipOverlay
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onPickPip,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess, contentColor = Color(0xFF041C11)),
                modifier = Modifier
                    .weight(1f)
                    .testTag("pip_add_button")
            ) {
                Icon(Icons.Filled.Layers, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Tambah Overlay / PiP", fontWeight = FontWeight.Bold)
            }
            if (selected != null) {
                IconButton(onClick = onDeleteSelectedPip) {
                    Icon(Icons.Filled.DeleteOutline, contentDescription = "Hapus Overlay", tint = CrimsonAlert)
                }
            }
        }

        if (selected != null) {
            Text("Mode Blending", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(BlendModePreset.entries) { mode ->
                    FilterChip(
                        selected = selected.blendMode == mode,
                        onClick = { onUpdateSelectedPip { it.copy(blendMode = mode) } },
                        label = { Text(mode.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EnergyOrangeSoft,
                            selectedLabelColor = EnergyOrange
                        )
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Skala", style = MaterialTheme.typography.labelMedium, color = TextSecondary, modifier = Modifier.width(64.dp))
                Slider(
                    value = selected.scale,
                    onValueChange = { v -> onUpdateSelectedPip { it.copy(scale = v) } },
                    valueRange = 0.18f..0.75f,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(thumbColor = EnergyOrange, activeTrackColor = EnergyOrange)
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Opasitas", style = MaterialTheme.typography.labelMedium, color = TextSecondary, modifier = Modifier.width(64.dp))
                Slider(
                    value = selected.opacity,
                    onValueChange = { v -> onUpdateSelectedPip { it.copy(opacity = v) } },
                    valueRange = 0.1f..1f,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(thumbColor = EnergyOrange, activeTrackColor = EnergyOrange)
                )
            }
        }
    }
}

@Composable
fun VideoEffectsInspector(
    selectedEffect: VideoEffectPreset,
    onSelectEffect: (VideoEffectPreset) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(VideoEffectPreset.entries) { fx ->
            val selected = fx == selectedEffect
            Surface(
                onClick = { onSelectEffect(fx) },
                shape = RoundedCornerShape(12.dp),
                color = if (selected) EnergyOrangeSoft else StudioCard,
                border = BorderStroke(1.5.dp, if (selected) EnergyOrange else StudioBorder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Filled.AutoAwesome, null, tint = if (selected) EnergyOrange else TextSecondary, modifier = Modifier.size(16.dp))
                    Text(fx.label, style = MaterialTheme.typography.labelLarge, color = if (selected) EnergyOrange else TextPrimary)
                }
            }
        }
    }
}

@Composable
fun ChromaKeyInspector(
    uiState: EditorUiState,
    onUpdateChromaKey: (ChromaKeyConfig) -> Unit
) {
    val chroma = uiState.selectedClip?.chromaKey ?: ChromaKeyConfig()
    val colors = listOf(0xFF00D84AL, 0xFF0066FFL, 0xFFFF2A55L)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Chroma Key (Green Screen)", style = MaterialTheme.typography.labelLarge, color = PureWhite)
            Switch(
                checked = chroma.enabled,
                onCheckedChange = { onUpdateChromaKey(chroma.copy(enabled = it)) },
                colors = SwitchDefaults.colors(checkedThumbColor = PureWhite, checkedTrackColor = EmeraldSuccess)
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Warna Kunci:", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
            colors.forEach { hex ->
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color(hex))
                        .border(
                            width = if (chroma.keyColorHex == hex) 2.5.dp else 1.dp,
                            color = if (chroma.keyColorHex == hex) EnergyOrange else StudioBorder,
                            shape = CircleShape
                        )
                        .clickable { onUpdateChromaKey(chroma.copy(keyColorHex = hex, enabled = true)) }
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Toleransi", style = MaterialTheme.typography.labelMedium, color = TextSecondary, modifier = Modifier.width(72.dp))
            Slider(
                value = chroma.tolerance,
                onValueChange = { onUpdateChromaKey(chroma.copy(tolerance = it, enabled = true)) },
                valueRange = 0.05f..0.75f,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(thumbColor = EnergyOrange, activeTrackColor = EnergyOrange)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Auto Background Removal (Subjek)", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
            Switch(
                checked = chroma.autoRemoveBg,
                onCheckedChange = { onUpdateChromaKey(chroma.copy(autoRemoveBg = it)) },
                colors = SwitchDefaults.colors(checkedThumbColor = PureWhite, checkedTrackColor = EnergyOrange)
            )
        }
    }
}

@Composable
fun KeyframeInspector(
    uiState: EditorUiState,
    onAddKeyframe: (Float, Float, Float, Float, Float) -> Unit,
    onClearKeyframes: () -> Unit
) {
    var scale by remember { mutableFloatStateOf(1.15f) }
    var rotation by remember { mutableFloatStateOf(0f) }
    var opacity by remember { mutableFloatStateOf(1f) }
    val count = uiState.selectedClip?.keyframes?.size ?: 0

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Keyframe Animasi ($count titik)", style = MaterialTheme.typography.labelLarge, color = GoldAccent)
            if (count > 0) {
                TextButton(onClick = onClearKeyframes) {
                    Text("Hapus Semua", color = CrimsonAlert)
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Skala ${String.format(java.util.Locale.US, "%.2fx", scale)}", style = MaterialTheme.typography.labelSmall, color = TextSecondary, modifier = Modifier.width(80.dp))
            Slider(
                value = scale,
                onValueChange = { scale = it },
                valueRange = 0.5f..2.2f,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(thumbColor = GoldAccent, activeTrackColor = GoldAccent)
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Rotasi ${rotation.toInt()}°", style = MaterialTheme.typography.labelSmall, color = TextSecondary, modifier = Modifier.width(80.dp))
            Slider(
                value = rotation,
                onValueChange = { rotation = it },
                valueRange = -180f..180f,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(thumbColor = GoldAccent, activeTrackColor = GoldAccent)
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Opasitas ${(opacity * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = TextSecondary, modifier = Modifier.width(80.dp))
            Slider(
                value = opacity,
                onValueChange = { opacity = it },
                valueRange = 0.1f..1f,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(thumbColor = GoldAccent, activeTrackColor = GoldAccent)
            )
        }
        Button(
            onClick = { onAddKeyframe(scale, rotation, opacity, 0f, 0f) },
            colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = Color(0xFF1A1200)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("keyframe_add_button")
        ) {
            Icon(Icons.Filled.Diamond, null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Tambah Keyframe di Playhead", fontWeight = FontWeight.Bold)
        }
    }
}
