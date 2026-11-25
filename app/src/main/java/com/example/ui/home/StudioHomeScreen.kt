package com.example.ui.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.FileCopy
import androidx.compose.material.icons.filled.FilterVintage
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.data.local.ExportEntity
import com.example.data.local.ProjectEntity
import com.example.model.AspectRatioPreset
import com.example.model.StudioTemplate
import com.example.ui.common.formatDurationShort
import com.example.ui.common.formatFileSize
import com.example.ui.common.formatTimestamp
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.EnergyOrange
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.KomikNarasiHeroGradient
import com.example.ui.theme.PureWhite
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCard
import com.example.ui.theme.StudioObsidian
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.StudioHomeViewModel
import java.io.File

private enum class HomeTab(val label: String) {
    EDIT("Studio"),
    TEMPLATE("Template KN"),
    EXPORTS("Ekspor"),
    DOWNLOAD("Unduh App")
}

private data class QuickToolShortcut(
    val title: String,
    val icon: ImageVector,
    val tint: Color,
    val presetRatio: AspectRatioPreset = AspectRatioPreset.RATIO_9_16
)

@Composable
fun StudioHomeScreen(
    viewModel: StudioHomeViewModel,
    onOpenProject: (Long) -> Unit
) {
    val projects by viewModel.projects.collectAsStateWithLifecycle()
    val exports by viewModel.exports.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedTemplateCategory.collectAsStateWithLifecycle()
    val isCreating by viewModel.isCreatingProject.collectAsStateWithLifecycle()
    val pendingTemplate by viewModel.pendingTemplate.collectAsStateWithLifecycle()
    val uiMessage by viewModel.uiMessage.collectAsStateWithLifecycle()

    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    var pendingRatio by remember { mutableStateOf<AspectRatioPreset?>(null) }
    var renameTarget by remember { mutableStateOf<ProjectEntity?>(null) }
    var previewExport by remember { mutableStateOf<ExportEntity?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiMessage) {
        uiMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg.text)
            viewModel.clearMessage()
        }
    }

    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 12)
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.createProjectFromUris(
                uris = uris,
                template = pendingTemplate,
                initialRatio = pendingRatio,
                onCreated = onOpenProject
            )
        } else {
            viewModel.setPendingTemplate(null)
            pendingRatio = null
        }
    }

    fun launchNewProject(template: StudioTemplate? = null, ratio: AspectRatioPreset? = null) {
        viewModel.setPendingTemplate(template)
        pendingRatio = ratio
        mediaPickerLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
        )
    }

    val currentTab = HomeTab.entries[selectedTabIndex]

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(StudioObsidian)
    ) {
        val isExpanded = maxWidth >= 720.dp

        Scaffold(
            containerColor = StudioObsidian,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                if (!isExpanded) {
                    NavigationBar(
                        containerColor = StudioSurface,
                        contentColor = TextPrimary,
                        windowInsets = WindowInsets.navigationBars
                    ) {
                        HomeTab.entries.forEachIndexed { idx, tab ->
                            val selected = selectedTabIndex == idx
                            NavigationBarItem(
                                selected = selected,
                                onClick = { selectedTabIndex = idx },
                                modifier = Modifier.testTag("home_tab_${tab.name.lowercase()}"),
                                icon = {
                                    Icon(
                                        imageVector = when (tab) {
                                            HomeTab.EDIT -> if (selected) Icons.Filled.Movie else Icons.Outlined.Movie
                                            HomeTab.TEMPLATE -> Icons.Filled.AutoAwesome
                                            HomeTab.EXPORTS -> if (selected) Icons.Filled.VideoLibrary else Icons.Outlined.VideoLibrary
                                            HomeTab.DOWNLOAD -> if (selected) Icons.Filled.Download else Icons.Outlined.Download
                                        },
                                        contentDescription = tab.label
                                    )
                                },
                                label = {
                                    Text(
                                        text = tab.label,
                                        style = MaterialTheme.typography.labelMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = EnergyOrange,
                                    selectedTextColor = EnergyOrange,
                                    indicatorColor = EnergyOrange.copy(alpha = 0.16f),
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                )
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isExpanded) {
                    NavigationRail(
                        containerColor = StudioSurface,
                        contentColor = TextPrimary
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))
                        HomeTab.entries.forEachIndexed { idx, tab ->
                            val selected = selectedTabIndex == idx
                            NavigationRailItem(
                                selected = selected,
                                onClick = { selectedTabIndex = idx },
                                modifier = Modifier.testTag("home_rail_${tab.name.lowercase()}"),
                                icon = {
                                    Icon(
                                        imageVector = when (tab) {
                                            HomeTab.EDIT -> if (selected) Icons.Filled.Movie else Icons.Outlined.Movie
                                            HomeTab.TEMPLATE -> Icons.Filled.AutoAwesome
                                            HomeTab.EXPORTS -> if (selected) Icons.Filled.VideoLibrary else Icons.Outlined.VideoLibrary
                                            HomeTab.DOWNLOAD -> if (selected) Icons.Filled.Download else Icons.Outlined.Download
                                        },
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = EnergyOrange,
                                    selectedTextColor = EnergyOrange,
                                    indicatorColor = EnergyOrange.copy(alpha = 0.16f),
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                )
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Column(
                        modifier = Modifier
                            .widthIn(max = 960.dp)
                            .fillMaxSize()
                    ) {
                        StudioTopHeader(
                            currentTab = currentTab,
                            projectCount = projects.size,
                            exportCount = exports.size,
                            onBadgeClick = {
                                viewModel.showMessage("Komik Narasi Pro Engine Aktif · Semua Fitur Terbuka")
                            }
                        )

                        HorizontalDivider(color = PureWhite.copy(alpha = 0.08f), thickness = 1.dp)

                        when (currentTab) {
                            HomeTab.EDIT -> EditHubContent(
                                projects = projects,
                                isExpanded = isExpanded,
                                onNewProject = { launchNewProject() },
                                onQuickTool = { shortcut ->
                                    launchNewProject(ratio = shortcut.presetRatio)
                                },
                                onOpenProject = onOpenProject,
                                onRename = { renameTarget = it },
                                onDuplicate = { viewModel.duplicateProject(it.id) },
                                onDelete = { viewModel.deleteProject(it.id) }
                            )

                            HomeTab.TEMPLATE -> TemplatesTabContent(
                                templates = viewModel.templates,
                                selectedCategory = selectedCategory,
                                isExpanded = isExpanded,
                                onSelectCategory = viewModel::selectTemplateCategory,
                                onUseTemplate = { template ->
                                    launchNewProject(template = template)
                                }
                            )

                            HomeTab.EXPORTS -> ExportsTabContent(
                                exports = exports,
                                onPlay = { previewExport = it },
                                onSaveGallery = viewModel::saveExportToGallery,
                                onShare = viewModel::shareExport,
                                onDelete = viewModel::deleteExport
                            )

                            HomeTab.DOWNLOAD -> AppDownloadTabContent(
                                onNotify = { msg -> viewModel.showMessage(msg) }
                            )
                        }
                    }

                    if (isCreating) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.72f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = StudioSurface,
                                border = BorderStroke(1.dp, EnergyOrange)
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    CircularProgressIndicator(color = EnergyOrange)
                                    Text(
                                        text = "Menyiapkan Komik Narasi Studio...",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = PureWhite
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    renameTarget?.let { target ->
        RenameProjectDialog(
            initialTitle = target.title,
            onDismiss = { renameTarget = null },
            onConfirm = { newTitle ->
                viewModel.renameProject(target.id, newTitle)
                renameTarget = null
            }
        )
    }

    previewExport?.let { exportItem ->
        ExportVideoPreviewDialog(
            export = exportItem,
            onDismiss = { previewExport = null },
            onSaveGallery = { viewModel.saveExportToGallery(exportItem) },
            onShare = { viewModel.shareExport(exportItem) }
        )
    }
}

@Composable
private fun StudioTopHeader(
    currentTab: HomeTab,
    projectCount: Int,
    exportCount: Int,
    onBadgeClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(KomikNarasiHeroGradient)
                    .border(1.dp, PureWhite.copy(alpha = 0.25f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.ContentCut,
                    contentDescription = "Logo Komik Narasi",
                    tint = PureWhite,
                    modifier = Modifier.size(21.dp)
                )
            }
            Column {
                Text(
                    text = "Komik Narasi",
                    style = MaterialTheme.typography.headlineMedium,
                    color = PureWhite
                )
                Text(
                    text = when (currentTab) {
                        HomeTab.EDIT -> "KN Cloud · $projectCount draf lokal"
                        HomeTab.TEMPLATE -> "Template Eksklusif Komik Narasi"
                        HomeTab.EXPORTS -> "Komik Narasi Pro Engine · $exportCount hasil"
                        HomeTab.DOWNLOAD -> "Unduh APK & panduan instalasi"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }

        Surface(
            onClick = onBadgeClick,
            shape = RoundedCornerShape(999.dp),
            color = Color.Transparent,
            border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.55f)),
            modifier = Modifier.testTag("pro_engine_badge")
        ) {
            Box(
                modifier = Modifier
                    .background(KomikNarasiHeroGradient)
                    .padding(horizontal = 12.dp, vertical = 7.dp)
            ) {
                Text(
                    text = "KN PRO",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = PureWhite
                )
            }
        }
    }
}

@Composable
private fun EditHubContent(
    projects: List<ProjectEntity>,
    isExpanded: Boolean,
    onNewProject: () -> Unit,
    onQuickTool: (QuickToolShortcut) -> Unit,
    onOpenProject: (Long) -> Unit,
    onRename: (ProjectEntity) -> Unit,
    onDuplicate: (ProjectEntity) -> Unit,
    onDelete: (ProjectEntity) -> Unit
) {
    val shortcuts = remember {
        listOf(
            QuickToolShortcut("Potong", Icons.Filled.ContentCut, EnergyOrange, AspectRatioPreset.RATIO_9_16),
            QuickToolShortcut("Narasi TTS", Icons.Filled.RecordVoiceOver, GoldAccent, AspectRatioPreset.RATIO_9_16),
            QuickToolShortcut("Kecepatan", Icons.Filled.Speed, PureWhite, AspectRatioPreset.RATIO_9_16),
            QuickToolShortcut("Filter KN", Icons.Filled.FilterVintage, EnergyOrange, AspectRatioPreset.RATIO_4_5),
            QuickToolShortcut("Teks Komik", Icons.Filled.TextFields, PureWhite, AspectRatioPreset.RATIO_3_4),
            QuickToolShortcut("Rasio", Icons.Filled.AspectRatio, GoldAccent, AspectRatioPreset.RATIO_16_9)
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            NewProjectHeroBanner(onClick = onNewProject)
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Alat Cepat Komik Narasi",
                    style = MaterialTheme.typography.titleMedium,
                    color = PureWhite
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(shortcuts) { shortcut ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onQuickTool(shortcut) }
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                                .testTag("quick_tool_${shortcut.title.lowercase()}")
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(StudioSurface)
                                    .border(1.dp, StudioBorder, RoundedCornerShape(16.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = shortcut.icon,
                                    contentDescription = shortcut.title,
                                    tint = shortcut.tint,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Text(
                                text = shortcut.title,
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Draf Komik Narasi",
                    style = MaterialTheme.typography.titleLarge,
                    color = PureWhite
                )
                if (projects.isNotEmpty()) {
                    Text(
                        text = "${projects.size} proyek",
                        style = MaterialTheme.typography.labelMedium,
                        color = GoldAccent
                    )
                }
            }
        }

        if (projects.isEmpty()) {
            item {
                EmptyDraftsCard(onNewProject = onNewProject)
            }
        } else {
            val chunked = projects.chunked(if (isExpanded) 2 else 1)
            items(chunked) { rowProjects ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowProjects.forEach { project ->
                        ProjectDraftCard(
                            project = project,
                            modifier = Modifier.weight(1f),
                            onClick = { onOpenProject(project.id) },
                            onRename = { onRename(project) },
                            onDuplicate = { onDuplicate(project) },
                            onDelete = { onDelete(project) }
                        )
                    }
                    if (isExpanded && rowProjects.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun NewProjectHeroBanner(onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
            .testTag("new_project_button"),
        shape = RoundedCornerShape(22.dp),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(KomikNarasiHeroGradient)
                .border(
                    width = 1.dp,
                    color = PureWhite.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(22.dp)
                )
                .padding(horizontal = 22.dp, vertical = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = StudioObsidian.copy(alpha = 0.72f)
                    ) {
                        Text(
                            text = "KOMIK NARASI STUDIO",
                            style = MaterialTheme.typography.labelSmall,
                            color = GoldAccent,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    Text(
                        text = "Buat Proyek Baru",
                        style = MaterialTheme.typography.displaySmall,
                        color = PureWhite
                    )
                    Text(
                        text = "Impor panel komik, video, & suara narasi dalam satu timeline studio yang ringan dan elegan.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = PureWhite.copy(alpha = 0.9f)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .clip(CircleShape)
                        .background(PureWhite),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Buat Proyek Baru",
                        tint = StudioObsidian,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ProjectDraftCard(
    project: ProjectEntity,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("project_card_${project.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = StudioSurface),
        border = BorderStroke(1.dp, StudioBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(StudioCard),
                contentAlignment = Alignment.Center
            ) {
                if (!project.coverUri.isNullOrBlank()) {
                    AsyncImage(
                        model = Uri.parse(project.coverUri),
                        contentDescription = project.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Movie,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp),
                    shape = RoundedCornerShape(6.dp),
                    color = StudioObsidian.copy(alpha = 0.85f)
                ) {
                    Text(
                        text = formatDurationShort(project.totalDurationMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = PureWhite,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = project.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = PureWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = EnergyOrange.copy(alpha = 0.16f)
                    ) {
                        Text(
                            text = project.aspectRatioName,
                            style = MaterialTheme.typography.labelSmall,
                            color = EnergyOrange,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = formatTimestamp(project.updatedAt),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.testTag("project_menu_${project.id}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "Opsi Proyek",
                        tint = PureWhite
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    containerColor = StudioCard
                ) {
                    DropdownMenuItem(
                        text = { Text("Ubah Nama", color = PureWhite) },
                        leadingIcon = {
                            Icon(Icons.Filled.DriveFileRenameOutline, null, tint = EnergyOrange)
                        },
                        onClick = {
                            menuExpanded = false
                            onRename()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Duplikasi", color = PureWhite) },
                        leadingIcon = {
                            Icon(Icons.Filled.FileCopy, null, tint = TextSecondary)
                        },
                        onClick = {
                            menuExpanded = false
                            onDuplicate()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Hapus", color = CrimsonAlert) },
                        leadingIcon = {
                            Icon(Icons.Filled.DeleteOutline, null, tint = CrimsonAlert)
                        },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyDraftsCard(onNewProject: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = StudioSurface,
        border = BorderStroke(1.dp, StudioBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Movie,
                contentDescription = null,
                tint = EnergyOrange,
                modifier = Modifier.size(44.dp)
            )
            Text(
                text = "Belum Ada Draf Komik Narasi",
                style = MaterialTheme.typography.titleLarge,
                color = PureWhite
            )
            Text(
                text = "Ketuk Buat Proyek Baru untuk mengimpor panel komik, video, atau foto dari galeri HP Anda.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Button(
                onClick = onNewProject,
                colors = ButtonDefaults.buttonColors(
                    containerColor = EnergyOrange,
                    contentColor = PureWhite
                ),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("empty_new_project_button")
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Pilih Media", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun TemplatesTabContent(
    templates: List<StudioTemplate>,
    selectedCategory: String,
    isExpanded: Boolean,
    onSelectCategory: (String) -> Unit,
    onUseTemplate: (StudioTemplate) -> Unit
) {
    val categories = listOf("Semua", "Komik Pop", "Narasi", "Sinematik", "Beat Sync")
    val filtered = if (selectedCategory == "Semua") {
        templates
    } else {
        templates.filter { it.category == selectedCategory }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories) { category ->
                val selected = category == selectedCategory
                FilterChip(
                    selected = selected,
                    onClick = { onSelectCategory(category) },
                    label = { Text(category) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = EnergyOrange.copy(alpha = 0.2f),
                        selectedLabelColor = EnergyOrange,
                        containerColor = StudioSurface,
                        labelColor = TextSecondary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selected,
                        borderColor = StudioBorder,
                        selectedBorderColor = EnergyOrange
                    )
                )
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(if (isExpanded) 2 else 1),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(filtered, key = { it.id }) { tpl ->
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = StudioSurface),
                    border = BorderStroke(1.dp, StudioBorder)
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                        ) {
                            Image(
                                painter = painterResource(id = tpl.coverDrawableRes),
                                contentDescription = tpl.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(Color.Transparent, StudioObsidian.copy(alpha = 0.85f))
                                        )
                                    )
                            )
                            Row(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = StudioObsidian.copy(alpha = 0.78f)
                                ) {
                                    Text(
                                        text = tpl.aspectRatio.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = EnergyOrange,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = StudioObsidian.copy(alpha = 0.78f)
                                ) {
                                    Text(
                                        text = "${tpl.clipCountLabel} · ${tpl.durationLabel}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = PureWhite,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            Text(
                                text = tpl.title,
                                style = MaterialTheme.typography.titleLarge,
                                color = PureWhite,
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(14.dp)
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = tpl.subtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                modifier = Modifier.weight(1f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Button(
                                onClick = { onUseTemplate(tpl) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = EnergyOrange,
                                    contentColor = PureWhite
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("use_template_${tpl.id}")
                            ) {
                                Text("Gunakan", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExportsTabContent(
    exports: List<ExportEntity>,
    onPlay: (ExportEntity) -> Unit,
    onSaveGallery: (ExportEntity) -> Unit,
    onShare: (ExportEntity) -> Unit,
    onDelete: (ExportEntity) -> Unit
) {
    if (exports.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.VideoLibrary,
                    contentDescription = null,
                    tint = EnergyOrange,
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    text = "Belum Ada Video Ter-Render",
                    style = MaterialTheme.typography.titleLarge,
                    color = PureWhite
                )
                Text(
                    text = "Hasil render MP4 dari Komik Narasi Studio akan tersimpan rapi di sini.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(exports, key = { it.id }) { item ->
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = StudioSurface),
                border = BorderStroke(1.dp, StudioBorder)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(StudioCard)
                                .clickable { onPlay(item) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = "Putar Video",
                                tint = EnergyOrange,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = PureWhite,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${item.resolutionLabel} · ${formatDurationShort(item.durationMs)} · ${formatFileSize(item.fileSizeBytes)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = EnergyOrange
                            )
                            Text(
                                text = formatTimestamp(item.createdAt),
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextMuted
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onSaveGallery(item) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = StudioCard,
                                contentColor = if (item.mediaStoreUri != null) EmeraldSuccess else PureWhite
                            ),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = if (item.mediaStoreUri != null) Icons.Filled.DownloadDone else Icons.Filled.SaveAlt,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (item.mediaStoreUri != null) "Tersimpan" else "Simpan",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                        Button(
                            onClick = { onShare(item) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EnergyOrange.copy(alpha = 0.18f),
                                contentColor = EnergyOrange
                            ),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Bagikan", style = MaterialTheme.typography.labelLarge)
                        }
                        IconButton(onClick = { onDelete(item) }) {
                            Icon(Icons.Filled.DeleteOutline, contentDescription = "Hapus", tint = CrimsonAlert)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RenameProjectDialog(
    initialTitle: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf(initialTitle) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = StudioSurface,
        title = { Text("Ubah Nama Proyek", color = PureWhite) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                label = { Text("Nama proyek") }
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }) {
                Text("Simpan", color = EnergyOrange)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal", color = TextSecondary)
            }
        }
    )
}

@Composable
private fun ExportVideoPreviewDialog(
    export: ExportEntity,
    onDismiss: () -> Unit,
    onSaveGallery: () -> Unit,
    onShare: () -> Unit
) {
    val context = LocalContext.current
    val player = remember(export.filePath) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.fromFile(File(export.filePath))))
            prepare()
            playWhenReady = true
        }
    }
    DisposableEffect(player) {
        onDispose { player.release() }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = StudioSurfaceElevated,
        title = {
            Text(
                text = export.title,
                style = MaterialTheme.typography.titleLarge,
                color = PureWhite
            )
        },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(9f / 16f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.Black)
            ) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            this.player = player
                            useController = true
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onShare) {
                Text("Bagikan", color = EnergyOrange)
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onSaveGallery) {
                    Text("Simpan Galeri", color = EmeraldSuccess)
                }
                TextButton(onClick = onDismiss) {
                    Text("Tutup", color = TextSecondary)
                }
            }
        }
    )
}
