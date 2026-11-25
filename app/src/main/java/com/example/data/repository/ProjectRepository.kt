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
            json.decodeFromString<ProjectTimelineData>(timelineJson)
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
            id = "tpl_cinematic",
            title = "Neon Tokyo Cut",
            subtitle = "Tone kontras sinematik untuk reel malam & urban",
            category = "Sinematik",
            coverDrawableRes = R.drawable.img_template_cinematic,
            clipCountLabel = "1-5 Klip",
            durationLabel = "0:15",
            filterPreset = FilterPreset.CINEMATIC,
            aspectRatio = AspectRatioPreset.RATIO_9_16,
            introText = "NIGHT STORIES"
        ),
        StudioTemplate(
            id = "tpl_vlog",
            title = "Golden Hour Vlog",
            subtitle = "Warna hangat matahari terbenam untuk travel & daily vlog",
            category = "Vlog",
            coverDrawableRes = R.drawable.img_template_vlog,
            clipCountLabel = "1-6 Klip",
            durationLabel = "0:30",
            filterPreset = FilterPreset.WARM_SUN,
            aspectRatio = AspectRatioPreset.RATIO_16_9,
            introText = "WEEKEND ESCAPE"
        ),
        StudioTemplate(
            id = "tpl_beat",
            title = "Cyber Beat Sync",
            subtitle = "Grade warna futuristik berenergi tinggi untuk musik & transisi",
            category = "Beat Sync",
            coverDrawableRes = R.drawable.img_template_beat,
            clipCountLabel = "1-8 Klip",
            durationLabel = "0:12",
            filterPreset = FilterPreset.CYBER,
            aspectRatio = AspectRatioPreset.RATIO_9_16,
            introText = "FEEL THE BEAT"
        )
    )
}
