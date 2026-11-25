package com.example.ui.editor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.ExportEntity
import com.example.model.AspectRatioPreset
import com.example.model.ExportBitrate
import com.example.model.ExportFrameRate
import com.example.model.ExportResolution
import com.example.ui.common.formatFileSize
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.EnergyOrange
import com.example.ui.theme.EnergyOrangeSoft
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.KomikNarasiProgressGradient
import com.example.ui.theme.PureWhite
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCard
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.ExportState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportBottomSheet(
    resolution: ExportResolution,
    frameRate: ExportFrameRate,
    bitrate: ExportBitrate,
    aspectRatio: AspectRatioPreset,
    includeWatermark: Boolean,
    exportState: ExportState,
    onSelectResolution: (ExportResolution) -> Unit,
    onSelectFrameRate: (ExportFrameRate) -> Unit,
    onSelectBitrate: (ExportBitrate) -> Unit,
    onSelectAspectRatio: (AspectRatioPreset) -> Unit,
    onToggleWatermark: (Boolean) -> Unit,
    onStartExport: () -> Unit,
    onCancelExport: () -> Unit,
    onSaveToGallery: (ExportEntity) -> Unit,
    onShare: (ExportEntity) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = {
            if (exportState !is ExportState.Exporting) onDismiss()
        },
        containerColor = StudioSurfaceElevated
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (exportState) {
                ExportState.Idle -> {
                    Text(
                        text = "Ekspor Komik Narasi Studio",
                        style = MaterialTheme.typography.titleLarge,
                        color = PureWhite,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Rasio Video Simpan (Pilih format output)",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextSecondary
                    )
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(AspectRatioPreset.entries) { ratio ->
                            val selected = ratio == aspectRatio
                            Surface(
                                onClick = { onSelectAspectRatio(ratio) },
                                shape = RoundedCornerShape(12.dp),
                                color = if (selected) EnergyOrangeSoft else StudioCard,
                                border = BorderStroke(1.5.dp, if (selected) EnergyOrange else StudioBorder),
                                modifier = Modifier
                                    .width(94.dp)
                                    .testTag("export_ratio_${ratio.name.lowercase()}")
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = ratio.label,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = if (selected) EnergyOrange else PureWhite,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = ratio.subtitle,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = "Resolusi (Default 720p ringan untuk HP entry-level)",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextSecondary
                    )
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(ExportResolution.entries) { res ->
                            val selected = res == resolution
                            Surface(
                                onClick = { onSelectResolution(res) },
                                shape = RoundedCornerShape(12.dp),
                                color = if (selected) EnergyOrangeSoft else StudioCard,
                                border = BorderStroke(1.5.dp, if (selected) EnergyOrange else StudioBorder),
                                modifier = Modifier.width(104.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = res.badge,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = if (selected) EnergyOrange else PureWhite,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = res.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    Text("Frame Rate", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ExportFrameRate.entries.forEach { fps ->
                            val selected = fps == frameRate
                            Surface(
                                onClick = { onSelectFrameRate(fps) },
                                shape = RoundedCornerShape(12.dp),
                                color = if (selected) EnergyOrangeSoft else StudioCard,
                                border = BorderStroke(1.5.dp, if (selected) EnergyOrange else StudioBorder),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = fps.label,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (selected) EnergyOrange else TextPrimary,
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp)
                                )
                            }
                        }
                    }

                    Text("Bitrate Video", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ExportBitrate.entries.forEach { br ->
                            val selected = br == bitrate
                            Surface(
                                onClick = { onSelectBitrate(br) },
                                shape = RoundedCornerShape(12.dp),
                                color = if (selected) EnergyOrangeSoft else StudioCard,
                                border = BorderStroke(1.5.dp, if (selected) EnergyOrange else StudioBorder),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = br.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (selected) EnergyOrange else TextPrimary,
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp)
                                )
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = StudioCard,
                        border = BorderStroke(1.dp, StudioBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Watermark \"Dibuat dengan Komik Narasi\"",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = PureWhite
                                )
                                Text(
                                    text = "Opsional — matikan untuk ekspor bersih tanpa watermark",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }
                            Switch(
                                checked = includeWatermark,
                                onCheckedChange = onToggleWatermark,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = PureWhite,
                                    checkedTrackColor = EnergyOrange
                                ),
                                modifier = Modifier.testTag("export_watermark_switch")
                            )
                        }
                    }

                    Button(
                        onClick = onStartExport,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EnergyOrange,
                            contentColor = PureWhite
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("confirm_export_button")
                    ) {
                        Text("Mulai Ekspor Sekarang", fontWeight = FontWeight.Bold)
                    }
                }

                is ExportState.Exporting -> {
                    val clampedProgress = exportState.progress.coerceIn(0.02f, 1f)
                    val pct = (clampedProgress * 100).toInt().coerceIn(0, 100)
                    Text(
                        text = "Merender Komik Narasi... $pct%",
                        style = MaterialTheme.typography.titleLarge,
                        color = PureWhite,
                        fontWeight = FontWeight.Bold
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(StudioCard)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(clampedProgress)
                                .clip(RoundedCornerShape(999.dp))
                                .background(KomikNarasiProgressGradient)
                        )
                    }
                    Text(
                        text = "Komik Narasi Pro Engine menggabungkan klip, narasi TTS/DSP, efek warna, dan overlay ke MP4.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    OutlinedButton(
                        onClick = onCancelExport,
                        border = BorderStroke(1.dp, CrimsonAlert),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Batalkan Ekspor", color = CrimsonAlert)
                    }
                }

                is ExportState.Success -> {
                    val entity = exportState.exportEntity
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = EmeraldSuccess,
                            modifier = Modifier.size(30.dp)
                        )
                        Column {
                            Text(
                                text = "Ekspor Selesai!",
                                style = MaterialTheme.typography.titleLarge,
                                color = PureWhite,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${entity.resolutionLabel} · ${formatFileSize(entity.fileSizeBytes)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = GoldAccent
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { onSaveToGallery(entity) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldSuccess,
                                contentColor = Color(0xFF041C11)
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.SaveAlt, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Simpan Galeri", fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { onShare(entity) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EnergyOrange,
                                contentColor = PureWhite
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Share, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Bagikan", fontWeight = FontWeight.Bold)
                        }
                    }

                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Selesai", color = TextSecondary)
                    }
                }

                is ExportState.Error -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Filled.ErrorOutline, null, tint = CrimsonAlert, modifier = Modifier.size(28.dp))
                        Text("Ekspor Gagal", style = MaterialTheme.typography.titleLarge, color = CrimsonAlert)
                    }
                    Text(
                        text = exportState.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Button(
                        onClick = onStartExport,
                        colors = ButtonDefaults.buttonColors(containerColor = EnergyOrange, contentColor = PureWhite),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Coba Lagi")
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
