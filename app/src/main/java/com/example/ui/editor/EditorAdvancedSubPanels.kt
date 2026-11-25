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
import androidx.compose.material.icons.automirrored.filled.CallSplit
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
import androidx.compose.material3.RangeSlider
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
import androidx.compose.ui.unit.sp
import com.example.model.AudioTrackRole
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
import com.example.ui.common.formatTimecodeMs
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
    onSelectAudioRole: (AudioTrackRole) -> Unit,
    onPickAudio: () -> Unit,
    onExtractAudio: () -> Unit,
    onSplitVocalAndMusic: () -> Unit,
    onAudioVolumeChange: (AudioTrackRole, Float) -> Unit,
    onAudioTrimChange: (AudioTrackRole, Long, Long) -> Unit,
    onShiftAudioOffset: (AudioTrackRole, Long) -> Unit,
    onAudioVoiceEffectChange: (AudioTrackRole, VoiceEffectPreset) -> Unit,
    onAudioVocalModeChange: (AudioTrackRole, VocalSeparationMode) -> Unit,
    onToggleAudioNoiseReduction: (AudioTrackRole) -> Unit,
    onToggleAudioDucking: (AudioTrackRole) -> Unit,
    onClipVoiceEffectChange: (VoiceEffectPreset) -> Unit,
    onClipVocalModeChange: (VocalSeparationMode) -> Unit,
    onToggleClipNoiseReduction: () -> Unit,
    onToggleFade: (AudioTrackRole, Boolean?, Boolean?) -> Unit,
    onRemoveAudio: (AudioTrackRole) -> Unit,
    onSynthesizeTtsNarration: (String, TtsVoiceStyle) -> Unit,
    onPolishNarrationScript: (String, (String) -> Unit) -> Unit
) {
    val selectedRole = uiState.selectedAudioRole
    val activeTrack = uiState.activeAudioTrack
    val clip = uiState.selectedClip
    val activeVoiceEffect = activeTrack?.voiceEffect ?: clip?.voiceEffect ?: VoiceEffectPreset.NONE
    val activeVocalMode = activeTrack?.vocalMode ?: clip?.vocalMode ?: VocalSeparationMode.ORIGINAL
    val activeNoise = activeTrack?.noiseReduction ?: clip?.noiseReduction ?: false

    var ttsScript by remember { mutableStateOf("") }
    var ttsStyle by remember { mutableStateOf(TtsVoiceStyle.NARATOR_BERAT) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(255.dp)
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Mandatory Feature: Pisah Suara & Musik Jadi 2 Track Terpisah
        Button(
            onClick = onSplitVocalAndMusic,
            colors = ButtonDefaults.buttonColors(containerColor = EnergyOrange, contentColor = PureWhite),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("audio_split_2tracks_button")
        ) {
            Icon(Icons.AutoMirrored.Filled.CallSplit, null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Pisah Suara & Musik Jadi 2 Track", fontWeight = FontWeight.Bold)
        }

        // Switcher between Independent Track 1 (Vokal) and Track 2 (Musik)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AudioTrackRole.entries.forEach { role ->
                val isSelected = selectedRole == role
                val hasTrack = when (role) {
                    AudioTrackRole.VOCAL -> uiState.timeline.vocalTrack != null
                    AudioTrackRole.MUSIC -> (uiState.timeline.musicTrack ?: uiState.timeline.audioTrack) != null
                }
                val accent = if (role == AudioTrackRole.VOCAL) EnergyOrange else GoldAccent
                Surface(
                    onClick = { onSelectAudioRole(role) },
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) accent.copy(alpha = 0.20f) else StudioCard,
                    border = BorderStroke(1.5.dp, if (isSelected) accent else StudioBorder),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("audio_role_${role.name.lowercase()}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = if (role == AudioTrackRole.VOCAL) Icons.Filled.RecordVoiceOver else Icons.Filled.MusicNote,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = if (role == AudioTrackRole.VOCAL) {
                                if (hasTrack) "Vokal (Aktif)" else "Track Vokal"
                            } else {
                                if (hasTrack) "Musik (Aktif)" else "Track Musik"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isSelected) accent else TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

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
                Text(
                    text = if (selectedRole == AudioTrackRole.VOCAL) "Pilih File Suara" else "Pilih File Musik",
                    fontWeight = FontWeight.Bold
                )
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

        if (activeTrack != null) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = StudioCard,
                border = BorderStroke(
                    1.dp,
                    if (selectedRole == AudioTrackRole.VOCAL) EnergyOrange.copy(alpha = 0.6f) else GoldAccent.copy(alpha = 0.6f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${selectedRole.label}: ${activeTrack.title}",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (selectedRole == AudioTrackRole.VOCAL) EnergyOrange else GoldAccent,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { onRemoveAudio(selectedRole) }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Filled.DeleteOutline, contentDescription = "Hapus Track Audio", tint = CrimsonAlert)
                        }
                    }

                    Text(
                        text = "Volume: ${(activeTrack.volume * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    Slider(
                        value = activeTrack.volume,
                        onValueChange = { onAudioVolumeChange(selectedRole, it) },
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(
                            thumbColor = if (selectedRole == AudioTrackRole.VOCAL) EnergyOrange else GoldAccent,
                            activeTrackColor = if (selectedRole == AudioTrackRole.VOCAL) EnergyOrange else GoldAccent
                        )
                    )

                    val maxDur = activeTrack.durationMs.coerceAtLeast(1000L).toFloat()
                    Text(
                        text = "Pangkas Audio: ${formatTimecodeMs(activeTrack.trimStartMs)} - ${formatTimecodeMs(activeTrack.trimEndMs)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    RangeSlider(
                        value = activeTrack.trimStartMs.toFloat()..activeTrack.trimEndMs.toFloat().coerceAtMost(maxDur),
                        onValueChange = { r ->
                            onAudioTrimChange(selectedRole, r.start.toLong(), r.endInclusive.toLong())
                        },
                        valueRange = 0f..maxDur,
                        colors = SliderDefaults.colors(thumbColor = EnergyOrange, activeTrackColor = EnergyOrange)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Posisi: ${formatTimecodeMs(activeTrack.startOffsetMs)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = PureWhite
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(
                                onClick = { onShiftAudioOffset(selectedRole, -500L) },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("-0.5s", style = MaterialTheme.typography.labelSmall, color = PureWhite)
                            }
                            OutlinedButton(
                                onClick = { onShiftAudioOffset(selectedRole, 500L) },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("+0.5s", style = MaterialTheme.typography.labelSmall, color = EnergyOrange)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Fade In", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Switch(
                                checked = activeTrack.fadeIn,
                                onCheckedChange = { onToggleFade(selectedRole, it, null) },
                                colors = SwitchDefaults.colors(checkedThumbColor = PureWhite, checkedTrackColor = EnergyOrange)
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Fade Out", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Switch(
                                checked = activeTrack.fadeOut,
                                onCheckedChange = { onToggleFade(selectedRole, null, it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = PureWhite, checkedTrackColor = EnergyOrange)
                            )
                        }
                    }

                    if (selectedRole == AudioTrackRole.MUSIC) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Auto-Ducking (Kecilkan Musik Saat Narasi)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary
                            )
                            Switch(
                                checked = activeTrack.autoDucking,
                                onCheckedChange = { onToggleAudioDucking(AudioTrackRole.MUSIC) },
                                colors = SwitchDefaults.colors(checkedThumbColor = PureWhite, checkedTrackColor = GoldAccent)
                            )
                        }
                    }
                }
            }
        }

        // Text-to-Speech (Narasi Suara Otomatis ke Track Vokal)
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
                        text = "Narasi Suara Otomatis (TTS -> Track Vokal)",
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

        Text("Pemisah Vokal & Musik (AI DSP)", style = MaterialTheme.typography.labelLarge, color = EnergyOrange)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(VocalSeparationMode.entries) { mode ->
                val selected = mode == activeVocalMode
                FilterChip(
                    selected = selected,
                    onClick = {
                        if (activeTrack != null) onAudioVocalModeChange(selectedRole, mode)
                        else onClipVocalModeChange(mode)
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
                        if (activeTrack != null) onAudioVoiceEffectChange(selectedRole, fx)
                        else onClipVoiceEffectChange(fx)
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
                    if (activeTrack != null) onToggleAudioNoiseReduction(selectedRole)
                    else onToggleClipNoiseReduction()
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
    onUpdateTextById: (String, (TextOverlayItem) -> TextOverlayItem) -> Unit,
    onSelectText: (String?) -> Unit,
    onSeekTo: (Long) -> Unit,
    onDeleteSelected: () -> Unit,
    onGenerateAutoCaptions: (String) -> Unit,
    onAutoTranscribeAndTranslate: (String) -> Unit,
    onTranslateCaptions: (String) -> Unit
) {
    val selected = uiState.selectedTextOverlay
    var inputText by remember(selected?.id) { mutableStateOf(selected?.text ?: "") }
    var targetTranslateLang by remember { mutableStateOf("Indonesia") }
    val styles = remember {
        listOf(
            TextStyleChoice("classic", "Klasik", 0xFFFFFFFF, 0xCC16161A, false),
            TextStyleChoice("kn_orange", "KN Oranye", 0xFFFFFFFF, 0xFFFF6B00, true),
            TextStyleChoice("kn_gold", "Komik Emas", 0xFF1A1200, 0xFFFFC107, false),
            TextStyleChoice("clean", "Putih Polos", 0xFFFFFFFF, 0x00000000, false)
        )
    }
    val comicStickers = listOf("BOOM!", "BERSAMBUNG...", "BABAK BARU", "WHOSH!")
    val translateLanguages = listOf("Indonesia", "English", "Japanese", "Korean", "Arabic", "Spanish", "Mandarin")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(255.dp)
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Mandatory Feature: Suara Otomatis ke Penerjemah
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = StudioCard,
            border = BorderStroke(1.dp, EnergyOrange.copy(alpha = 0.6f)),
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
                    Icon(Icons.Filled.Translate, null, tint = EnergyOrange, modifier = Modifier.size(16.dp))
                    Text(
                        text = "Suara Otomatis ke Penerjemah",
                        style = MaterialTheme.typography.labelLarge,
                        color = PureWhite,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "Pilih bahasa tujuan terjemahan dari ucapan video:",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(translateLanguages) { lang ->
                        val active = lang == targetTranslateLang
                        FilterChip(
                            selected = active,
                            onClick = { targetTranslateLang = lang },
                            label = { Text(lang) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EnergyOrangeSoft,
                                selectedLabelColor = EnergyOrange
                            ),
                            modifier = Modifier.testTag("speech_translate_lang_${lang.lowercase()}")
                        )
                    }
                }
                Button(
                    onClick = { onAutoTranscribeAndTranslate(targetTranslateLang) },
                    colors = ButtonDefaults.buttonColors(containerColor = EnergyOrange, contentColor = PureWhite),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ai_speech_to_translate_button")
                ) {
                    Icon(Icons.Filled.Translate, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Deteksi Suara & Terjemahkan ($targetTranslateLang)", fontWeight = FontWeight.Bold)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { onGenerateAutoCaptions("Indonesian or English") },
                        border = BorderStroke(1.dp, GoldAccent),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("ai_auto_caption_button")
                    ) {
                        Icon(Icons.Filled.ClosedCaption, null, tint = GoldAccent, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Transkrip Asli", color = GoldAccent)
                    }
                    if (uiState.timeline.textOverlays.isNotEmpty()) {
                        OutlinedButton(
                            onClick = { onTranslateCaptions(targetTranslateLang) },
                            border = BorderStroke(1.dp, PureWhite.copy(alpha = 0.45f)),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("ai_translate_existing_button")
                        ) {
                            Text("Terjemah Teks Ada", color = PureWhite)
                        }
                    }
                }
            }
        }

        // Inline Editable Subtitle List (Edit Terjemahan Langsung)
        if (uiState.timeline.textOverlays.isNotEmpty()) {
            Text(
                text = "Daftar Subtitle & Terjemahan (${uiState.timeline.textOverlays.size}) - Edit Langsung:",
                style = MaterialTheme.typography.labelSmall,
                color = GoldAccent,
                fontWeight = FontWeight.Bold
            )
            uiState.timeline.textOverlays.forEach { item ->
                val isSel = item.id == selected?.id
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSel) EnergyOrangeSoft.copy(alpha = 0.45f) else StudioCard,
                    border = BorderStroke(1.dp, if (isSel) EnergyOrange else StudioBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelectText(item.id)
                                    onSeekTo(item.startMs)
                                },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${formatTimecodeMs(item.startMs)} - ${formatTimecodeMs(item.endMs)}",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = EnergyOrange,
                                fontWeight = FontWeight.Bold
                            )
                            if (!item.originalText.isNullOrBlank()) {
                                Text(
                                    text = "Asli: ${item.originalText}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = TextSecondary,
                                    maxLines = 1
                                )
                            }
                        }
                        OutlinedTextField(
                            value = item.text,
                            onValueChange = { updatedText ->
                                onUpdateTextById(item.id) { it.copy(text = updatedText) }
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("subtitle_inline_edit_${item.id}")
                        )
                    }
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
    val chromaColors = listOf(0xFF00D84AL, 0xFF0066FFL, 0xFFFF2A55L)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(225.dp)
            .verticalScroll(rememberScrollState())
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
                Text("Tambah Layer / Overlay PiP", fontWeight = FontWeight.Bold)
            }
            if (selected != null) {
                IconButton(onClick = onDeleteSelectedPip) {
                    Icon(Icons.Filled.DeleteOutline, contentDescription = "Hapus Overlay", tint = CrimsonAlert)
                }
            }
        }

        if (selected != null) {
            Text("Geser, cubit & putar layer PiP langsung di kanvas pratinjau.", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            Text("Mode Blending (Ala KineMaster)", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
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
                    valueRange = 0.16f..0.95f,
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

            // Per-layer Chroma Key on PiP
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Chroma Key Layer (Green Screen)", style = MaterialTheme.typography.labelMedium, color = PureWhite)
                Switch(
                    checked = selected.chromaEnabled,
                    onCheckedChange = { enabled -> onUpdateSelectedPip { it.copy(chromaEnabled = enabled) } },
                    colors = SwitchDefaults.colors(checkedThumbColor = PureWhite, checkedTrackColor = EmeraldSuccess)
                )
            }
            if (selected.chromaEnabled) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Kunci:", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    chromaColors.forEach { hex ->
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color(hex))
                                .border(
                                    width = if (selected.chromaKeyHex == hex) 2.dp else 1.dp,
                                    color = if (selected.chromaKeyHex == hex) EnergyOrange else StudioBorder,
                                    shape = CircleShape
                                )
                                .clickable { onUpdateSelectedPip { it.copy(chromaKeyHex = hex) } }
                        )
                    }
                    Slider(
                        value = selected.chromaTolerance,
                        onValueChange = { tol -> onUpdateSelectedPip { it.copy(chromaTolerance = tol) } },
                        valueRange = 0.05f..0.75f,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(thumbColor = EmeraldSuccess, activeTrackColor = EmeraldSuccess)
                    )
                }
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
