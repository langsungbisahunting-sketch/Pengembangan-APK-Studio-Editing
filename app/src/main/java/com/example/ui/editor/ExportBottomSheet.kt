package com.example.ui.editor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.ExportEntity
import com.example.model.ExportBitrate
import com.example.model.ExportFrameRate
import com.example.model.ExportResolution
import com.example.ui.common.formatFileSize
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.NeonCoral
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
    exportState: ExportState,
    onSelectResolution: (ExportResolution) -> Unit,
    onSelectFrameRate: (ExportFrameRate) -> Unit,
    onSelectBitrate: (ExportBitrate) -> Unit,
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
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (exportState) {
                ExportState.Idle -> {
                    Text(
                        text = "Pengaturan Ekspor MP4",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )

                    Text("Resolusi", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ExportResolution.entries.forEach { res ->
                            val selected = res == resolution
                            Surface(
                                onClick = { onSelectResolution(res) },
                                shape = RoundedCornerShape(12.dp),
                                color = if (selected) ElectricCyan.copy(alpha = 0.16f) else StudioCard,
                                border = BorderStroke(1.5.dp, if (selected) ElectricCyan else StudioBorder),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = res.badge,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = if (selected) ElectricCyan else TextPrimary
                                    )
                                    Text(
                                        text = res.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary
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
                                color = if (selected) ElectricCyan.copy(alpha = 0.16f) else StudioCard,
                                border = BorderStroke(1.5.dp, if (selected) ElectricCyan else StudioBorder),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = fps.label,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (selected) ElectricCyan else TextPrimary,
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
                                color = if (selected) ElectricCyan.copy(alpha = 0.16f) else StudioCard,
                                border = BorderStroke(1.5.dp, if (selected) ElectricCyan else StudioBorder),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = br.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (selected) ElectricCyan else TextPrimary,
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp)
                                )
                            }
                        }
                    }

                    Button(
                        onClick = onStartExport,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricCyan,
                            contentColor = Color(0xFF041318)
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
                    val pct = (exportState.progress * 100).toInt().coerceIn(0, 100)
                    Text(
                        text = "Sedang Merender Video... $pct%",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                    LinearProgressIndicator(
                        progress = { exportState.progress },
                        color = ElectricCyan,
                        trackColor = StudioCard,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                    )
                    Text(
                        text = "Menggabungkan klip, efek warna, audio DSP, dan teks overlay ke MP4.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    OutlinedButton(
                        onClick = onCancelExport,
                        border = BorderStroke(1.dp, NeonCoral),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Batalkan Ekspor", color = NeonCoral)
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
                                color = TextPrimary
                            )
                            Text(
                                text = "${entity.resolutionLabel} · ${formatFileSize(entity.fileSizeBytes)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = ElectricCyan
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
                                containerColor = ElectricCyan,
                                contentColor = Color(0xFF041318)
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
                        Icon(Icons.Filled.ErrorOutline, null, tint = NeonCoral, modifier = Modifier.size(28.dp))
                        Text("Ekspor Gagal", style = MaterialTheme.typography.titleLarge, color = NeonCoral)
                    }
                    Text(
                        text = exportState.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Button(
                        onClick = onStartExport,
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = Color(0xFF041318)),
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
