package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val aspectRatioName: String,
    val totalDurationMs: Long,
    val coverUri: String?,
    val timelineJson: String,
    val updatedAt: Long = System.currentTimeMillis()
)
