package com.example.data.repository

import com.example.R
import com.example.data.local.ExportEntity
import com.example.data.local.ProjectEntity
import com.example.data.local.VeloCutDao
import com.example.model.AspectRatioPreset
import com.example.model.FilterPreset
import com.example.model.ProjectTimelineData
import com.example.model.StudioTemplate
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json

class ProjectRepository(private val dao: VeloCutDao) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    val allProjects: Flow<List<ProjectEntity>> = dao.observeAllProjects()
    val allExports: Flow<List<ExportEntity>> = dao.observeAllExports()

    suspend fun getProject(id: Long): ProjectEntity? = dao.getProjectById(id)

    fun decodeTimeline(timelineJson: String): ProjectTimelineData {
        return runCatching {
            val decoded = json.decodeFromString<ProjectTimelineData>(timelineJson)
            if (decoded.musicTrack == null && decoded.audioTrack != null) {
                decoded.copy(musicTrack = decoded.audioTrack, audioTrack = null)
            } else {
                decoded
            }
        }.getOrDefault(ProjectTimelineData())
    }

    fun encodeTimeline(timeline: ProjectTimelineData): String {
        return json.encodeToString(ProjectTimelineData.serializer(), timeline)
    }

    suspend fun createProject(
        title: String,
        timeline: ProjectTimelineData
    ): Long {
        val entity = ProjectEntity(
            title = title,
            aspectRatioName = timeline.aspectRatio.label,
            totalDurationMs = timeline.totalDurationMs,
            coverUri = timeline.clips.firstOrNull()?.uriString,
            timelineJson = encodeTimeline(timeline),
            updatedAt = System.currentTimeMillis()
        )
        return dao.insertProject(entity)
    }

    suspend fun saveProject(
        id: Long,
        title: String,
        timeline: ProjectTimelineData
    ) {
        val entity = ProjectEntity(
            id = id,
            title = title,
            aspectRatioName = timeline.aspectRatio.label,
            totalDurationMs = timeline.totalDurationMs,
            coverUri = timeline.clips.firstOrNull()?.uriString,
            timelineJson = encodeTimeline(timeline),
            updatedAt = System.currentTimeMillis()
        )
        dao.updateProject(entity)
    }

    suspend fun renameProject(id: Long, newTitle: String) {
        val existing = dao.getProjectById(id) ?: return
        dao.updateProject(existing.copy(title = newTitle, updatedAt = System.currentTimeMillis()))
    }

    suspend fun duplicateProject(id: Long): Long? {
        val existing = dao.getProjectById(id) ?: return null
        return dao.insertProject(
            existing.copy(
                id = 0L,
                title = "${existing.title} (Salinan)",
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteProject(id: Long) = dao.deleteProjectById(id)

    suspend fun recordExport(export: ExportEntity): Long = dao.insertExport(export)

    suspend fun updateExportMediaStoreUri(id: Long, uri: String) =
        dao.updateExportMediaStoreUri(id, uri)

    suspend fun deleteExport(id: Long) = dao.deleteExportById(id)

    fun getTemplates(): List<StudioTemplate> = listOf(
        StudioTemplate(
            id = "tpl_komik_pop",
            title = "Panel Komik Drama",
            subtitle = "Preset eksklusif Komik Narasi untuk panel cerita & dialog dinamis",
            category = "Komik Pop",
            coverDrawableRes = R.drawable.img_template_cinematic,
            clipCountLabel = "1-6 Panel",
            durationLabel = "0:20",
            filterPreset = FilterPreset.KOMIK_POP,
            aspectRatio = AspectRatioPreset.RATIO_9_16,
            introText = "BABAK BARU"
        ),
        StudioTemplate(
            id = "tpl_recap",
            title = "Recap Alur Cerita",
            subtitle = "Tone sinematik hangat untuk narasi alur komik & YouTube",
            category = "Narasi",
            coverDrawableRes = R.drawable.img_template_vlog,
            clipCountLabel = "1-8 Klip",
            durationLabel = "0:45",
            filterPreset = FilterPreset.WARM_SUN,
            aspectRatio = AspectRatioPreset.RATIO_16_9,
            introText = "KISAH SANG TOKOH"
        ),
        StudioTemplate(
            id = "tpl_noir",
            title = "Misteri Komik Noir",
            subtitle = "Kontras gelap-putih tajam untuk adegan ketegangan & twist",
            category = "Sinematik",
            coverDrawableRes = R.drawable.img_template_cinematic,
            clipCountLabel = "1-5 Klip",
            durationLabel = "0:15",
            filterPreset = FilterPreset.KOMIK_NOIR,
            aspectRatio = AspectRatioPreset.RATIO_4_5,
            introText = "RAHASIA MALAM"
        ),
        StudioTemplate(
            id = "tpl_beat",
            title = "Aksi Beat Komik",
            subtitle = "Transisi cepat berenergi untuk adegan aksi & efek visual",
            category = "Beat Sync",
            coverDrawableRes = R.drawable.img_template_beat,
            clipCountLabel = "1-8 Panel",
            durationLabel = "0:15",
            filterPreset = FilterPreset.CINEMATIC,
            aspectRatio = AspectRatioPreset.RATIO_9_16,
            introText = "AKSI PUNCAK"
        )
    )
}
