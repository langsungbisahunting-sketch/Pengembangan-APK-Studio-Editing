package com.example.ui.editor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoAwesomeMotion
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FilterBAndW
import androidx.compose.material.icons.filled.FilterVintage
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.ExportResolution
import com.example.ui.theme.AmberAudio
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.NeonCoral
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCard
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletOverlay
import com.example.viewmodel.EditorToolTab

@Composable
fun EditorTopBar(
    title: String,
    canUndo: Boolean,
    canRedo: Boolean,
    resolution: ExportResolution,
    onBack: () -> Unit,
    onTitleClick: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onResolutionClick: () -> Unit,
    onExportClick: () -> Unit
) {
    Surface(
        color = StudioSurface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("editor_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali",
                        tint = TextPrimary
                    )
                }

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onTitleClick)
                        .padding(horizontal = 6.dp, vertical = 6.dp)
                        .testTag("editor_title_button")
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                IconButton(
                    onClick = onUndo,
                    enabled = canUndo,
                    modifier = Modifier.testTag("editor_undo_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Undo,
                        contentDescription = "Urungkan",
                        tint = if (canUndo) TextPrimary else TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = onRedo,
                    enabled = canRedo,
                    modifier = Modifier.testTag("editor_redo_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Redo,
                        contentDescription = "Ulangi",
                        tint = if (canRedo) TextPrimary else TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Surface(
                    onClick = onResolutionClick,
                    shape = RoundedCornerShape(8.dp),
                    color = StudioCard,
                    border = BorderStroke(1.dp, StudioBorder),
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .testTag("editor_resolution_chip")
                ) {
                    Text(
                        text = resolution.badge,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                }

                Button(
                    onClick = onExportClick,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricCyan,
                        contentColor = Color(0xFF041318)
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    modifier = Modifier
                        .height(36.dp)
                        .testTag("editor_export_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.FileUpload,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Ekspor",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private data class ToolDockItem(
    val tab: EditorToolTab,
    val icon: ImageVector,
    val accent: Color
)

@Composable
fun EditorBottomToolDock(
    activeTab: EditorToolTab,
    onSelectTab: (EditorToolTab) -> Unit,
    onBackToMain: () -> Unit
) {
    val mainTools = listOf(
        ToolDockItem(EditorToolTab.EDIT, Icons.Filled.ContentCut, ElectricCyan),
        ToolDockItem(EditorToolTab.AUDIO, Icons.Filled.MusicNote, AmberAudio),
        ToolDockItem(EditorToolTab.TEXT, Icons.Filled.TextFields, VioletOverlay),
        ToolDockItem(EditorToolTab.PIP, Icons.Filled.Layers, EmeraldSuccess),
        ToolDockItem(EditorToolTab.EFFECTS, Icons.Filled.AutoAwesome, NeonCoral),
        ToolDockItem(EditorToolTab.FILTER, Icons.Filled.FilterVintage, NeonCoral),
        ToolDockItem(EditorToolTab.ADJUST, Icons.Filled.Tune, ElectricCyan),
        ToolDockItem(EditorToolTab.CHROMA, Icons.Filled.FilterBAndW, EmeraldSuccess),
        ToolDockItem(EditorToolTab.KEYFRAME, Icons.Filled.Diamond, ElectricCyan),
        ToolDockItem(EditorToolTab.RATIO, Icons.Filled.AspectRatio, TextPrimary),
        ToolDockItem(EditorToolTab.TRANSITION, Icons.Filled.AutoAwesomeMotion, ElectricCyan),
        ToolDockItem(EditorToolTab.CANVAS, Icons.Filled.ColorLens, TextSecondary)
    )

    Surface(
        color = StudioSurface,
        border = BorderStroke(1.dp, StudioBorder),
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
    ) {
        if (activeTab == EditorToolTab.MAIN) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(mainTools) { item ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .width(66.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelectTab(item.tab) }
                            .padding(vertical = 6.dp)
                            .testTag("tool_${item.tab.name.lowercase()}")
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.tab.label,
                            tint = item.accent,
                            modifier = Modifier.size(21.dp)
                        )
                        Text(
                            text = item.tab.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = TextPrimary,
                            maxLines = 1
                        )
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    onClick = onBackToMain,
                    shape = RoundedCornerShape(10.dp),
                    color = StudioCard,
                    border = BorderStroke(1.dp, StudioBorder),
                    modifier = Modifier.testTag("tool_back_to_main")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ChevronLeft,
                            contentDescription = "Tutup Alat",
                            tint = TextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "Menu",
                            style = MaterialTheme.typography.labelLarge,
                            color = TextPrimary
                        )
                    }
                }

                Text(
                    text = activeTab.label,
                    style = MaterialTheme.typography.titleMedium,
                    color = ElectricCyan
                )

                Surface(
                    onClick = onBackToMain,
                    shape = RoundedCornerShape(10.dp),
                    color = ElectricCyan.copy(alpha = 0.16f)
                ) {
                    Text(
                        text = "Selesai",
                        style = MaterialTheme.typography.labelLarge,
                        color = ElectricCyan,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}
