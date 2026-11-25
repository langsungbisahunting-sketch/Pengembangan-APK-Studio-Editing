package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface VeloCutDao {
    @Query("SELECT * FROM projects ORDER BY updatedAt DESC")
    fun observeAllProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
    suspend fun getProjectById(id: Long): ProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity): Long

    @Update
    suspend fun updateProject(project: ProjectEntity)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteProjectById(id: Long)

    @Query("SELECT * FROM exports ORDER BY createdAt DESC")
    fun observeAllExports(): Flow<List<ExportEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExport(export: ExportEntity): Long

    @Query("UPDATE exports SET mediaStoreUri = :uri WHERE id = :id")
    suspend fun updateExportMediaStoreUri(id: Long, uri: String)

    @Query("DELETE FROM exports WHERE id = :id")
    suspend fun deleteExportById(id: Long)
}
