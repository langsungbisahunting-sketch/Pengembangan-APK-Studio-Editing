package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ProjectEntity::class, ExportEntity::class],
    version = 1,
    exportSchema = false
)
abstract class VeloCutDatabase : RoomDatabase() {
    abstract fun veloCutDao(): VeloCutDao

    companion object {
        @Volatile
        private var INSTANCE: VeloCutDatabase? = null

        fun getInstance(context: Context): VeloCutDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VeloCutDatabase::class.java,
                    "velocut_studio.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
