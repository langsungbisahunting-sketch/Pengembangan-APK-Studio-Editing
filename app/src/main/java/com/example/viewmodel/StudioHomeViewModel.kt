package com.example.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.ExportEntity
import com.example.data.local.ProjectEntity
import com.example.data.local.VeloCutDatabase
import com.example.data.repository.ProjectRepository
import com.example.media.MediaMetadataHelper
import com.example.media.VideoExportManager
import com.example.model.AspectRatioPreset
import com.example.model.FilterPreset
import com.example.model.ProjectTimelineData
import com.example.model.StudioTemplate
import com.example.model.TextOverlayItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

data class HomeUiMessage(val text: String, val isError: Boolean = false)

class StudioHomeViewModel(application: Application) : AndroidViewModel(application) {

    private val database = VeloCutDatabase.getInstance(application)
    private val repository = ProjectRepository(database.veloCutDao())

    val projects: StateFlow<List<ProjectEntity>> = repository.allProjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val exports: StateFlow<List<ExportEntity>> = repository.allExports
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val templates: List<StudioTemplate> = repository.getTemplates()

    private val _selectedTemplateCategory = MutableStateFlow("Semua")
    val selectedTemplateCategory: StateFlow<String> = _selectedTemplateCategory.asStateFlow()

    private val _isCreatingProject = MutableStateFlow(false)
    val isCreatingProject: StateFlow<Boolean> = _isCreatingProject.asStateFlow()

    private val _pendingTemplate = MutableStateFlow<StudioTemplate?>(null)
    val pendingTemplate: StateFlow<StudioTemplate?> = _pendingTemplate.asStateFlow()

    private val _uiMessage = MutableStateFlow<HomeUiMessage?>(null)
    val uiMessage: StateFlow<HomeUiMessage?> = _uiMessage.asStateFlow()

    fun selectTemplateCategory(category: String) {
        _selectedTemplateCategory.value = category
    }

    fun setPendingTemplate(template: StudioTemplate?) {
        _pendingTemplate.value = template
    }

    fun clearMessage() {
        _uiMessage.value = null
    }

    fun showMessage(text: String) {
        _uiMessage.value = HomeUiMessage(text)
    }

    fun createProjectFromUris(
        uris: List<Uri>,
        template: StudioTemplate? = null,
        initialRatio: AspectRatioPreset? = null,
        onCreated: (Long) -> Unit
    ) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _isCreatingProject.value = true
            try {
                val context = getApplication<Application>()
                val clips = uris.map { uri ->
                    MediaMetadataHelper.probeMediaToClip(context, uri)
                }
                val ratio = template?.aspectRatio ?: initialRatio ?: AspectRatioPreset.RATIO_9_16
                val filter = template?.filterPreset ?: FilterPreset.NONE
                val overlays = if (template != null) {
                    listOf(
                        TextOverlayItem(
                            text = template.introText,
                            startMs = 0L,
                            endMs = clips.sumOf { it.effectiveDurationMs }.coerceAtMost(3500L),
                            xFraction = 0.5f,
                            yFraction = 0.22f,
                            fontSizeSp = 28f,
                            colorHex = 0xFFFFFFFF,
                            bgColorHex = 0xFFFF6B00,
                            styleId = "kn_orange"
                        )
                    )
                } else emptyList()

                val timeline = ProjectTimelineData(
                    clips = clips,
                    textOverlays = overlays,
                    filterPreset = filter,
                    aspectRatio = ratio
                )
                val title = template?.title ?: "KN ${clips.firstOrNull()?.displayName?.substringBeforeLast('.') ?: "Proyek Baru"}"
                val projectId = repository.createProject(title.take(32), timeline)
                _pendingTemplate.value = null
                onCreated(projectId)
            } catch (e: Throwable) {
                _uiMessage.value = HomeUiMessage(
                    "Gagal memuat media: ${e.localizedMessage ?: "Kesalahan tidak diketahui"}",
                    isError = true
                )
            } finally {
                _isCreatingProject.value = false
            }
        }
    }

    fun renameProject(id: Long, newTitle: String) {
        val clean = newTitle.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch {
            repository.renameProject(id, clean)
        }
    }

    fun duplicateProject(id: Long) {
        viewModelScope.launch {
            repository.duplicateProject(id)
            _uiMessage.value = HomeUiMessage("Proyek berhasil diduplikasi")
        }
    }

    fun deleteProject(id: Long) {
        viewModelScope.launch {
            repository.deleteProject(id)
            _uiMessage.value = HomeUiMessage("Draf dihapus")
        }
    }

    fun saveExportToGallery(export: ExportEntity) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val file = File(export.filePath)
            if (!file.exists()) {
                _uiMessage.value = HomeUiMessage("File video tidak ditemukan di perangkat", isError = true)
                return@launch
            }
            val uri = VideoExportManager.saveToGalleryMediaStore(context, file, export.title)
            if (uri != null) {
                repository.updateExportMediaStoreUri(export.id, uri.toString())
                _uiMessage.value = HomeUiMessage("Disimpan ke Galeri (Movies/KomikNarasi)")
            } else {
                _uiMessage.value = HomeUiMessage("Gagal menyimpan ke Galeri", isError = true)
            }
        }
    }

    fun shareExport(export: ExportEntity) {
        val context = getApplication<Application>()
        val file = File(export.filePath)
        if (!file.exists()) {
            _uiMessage.value = HomeUiMessage("File video tidak ditemukan", isError = true)
            return
        }
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "video/mp4"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(shareIntent, "Bagikan Video Komik Narasi").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    fun deleteExport(export: ExportEntity) {
        viewModelScope.launch {
            runCatching { File(export.filePath).delete() }
            repository.deleteExport(export.id)
            _uiMessage.value = HomeUiMessage("Hasil ekspor dihapus")
        }
    }
}
