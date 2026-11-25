package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exports")
data class ExportEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val filePath: String,
    val mediaStoreUri: String? = null,
    val resolutionLabel: String,
    val durationMs: Long,
    val fileSizeBytes: Long,
    val createdAt: Long = System.currentTimeMillis()
)
