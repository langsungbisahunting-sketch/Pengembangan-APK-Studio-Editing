package com.example.ui.editor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.EnergyOrange
import com.example.ui.theme.EnergyOrangeSoft
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.KomikNarasiHeroGradient
import com.example.ui.theme.PureWhite
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCard
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
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
                        tint = PureWhite
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
                        tint = if (canUndo) PureWhite else TextMuted,
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
                        tint = if (canRedo) PureWhite else TextMuted,
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
                        color = PureWhite,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .height(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(KomikNarasiHeroGradient)
                        .clickable(onClick = onExportClick)
                        .padding(horizontal = 12.dp)
                        .testTag("editor_export_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.FileUpload,
                            contentDescription = null,
                            tint = PureWhite,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Ekspor",
                            style = MaterialTheme.typography.labelLarge,
                            color = PureWhite,
                            fontWeight = FontWeight.Bold
                        )
                    }
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
        ToolDockItem(EditorToolTab.EDIT, Icons.Filled.ContentCut, EnergyOrange),
        ToolDockItem(EditorToolTab.AUDIO, Icons.Filled.MusicNote, GoldAccent),
        ToolDockItem(EditorToolTab.TEXT, Icons.Filled.TextFields, PureWhite),
        ToolDockItem(EditorToolTab.PIP, Icons.Filled.Layers, EmeraldSuccess),
        ToolDockItem(EditorToolTab.EFFECTS, Icons.Filled.AutoAwesome, EnergyOrange),
        ToolDockItem(EditorToolTab.FILTER, Icons.Filled.FilterVintage, EnergyOrange),
        ToolDockItem(EditorToolTab.ADJUST, Icons.Filled.Tune, PureWhite),
        ToolDockItem(EditorToolTab.CHROMA, Icons.Filled.FilterBAndW, EmeraldSuccess),
        ToolDockItem(EditorToolTab.KEYFRAME, Icons.Filled.Diamond, GoldAccent),
        ToolDockItem(EditorToolTab.RATIO, Icons.Filled.AspectRatio, PureWhite),
        ToolDockItem(EditorToolTab.TRANSITION, Icons.Filled.AutoAwesomeMotion, EnergyOrange),
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
                            .width(68.dp)
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
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
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
                            tint = PureWhite,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "Menu",
                            style = MaterialTheme.typography.labelLarge,
                            color = PureWhite
                        )
                    }
                }

                Text(
                    text = activeTab.label,
                    style = MaterialTheme.typography.titleMedium,
                    color = EnergyOrange,
                    fontWeight = FontWeight.Bold
                )

                Surface(
                    onClick = onBackToMain,
                    shape = RoundedCornerShape(10.dp),
                    color = EnergyOrangeSoft,
                    border = BorderStroke(1.dp, EnergyOrange.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "Selesai",
                        style = MaterialTheme.typography.labelLarge,
                        color = EnergyOrange,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}
