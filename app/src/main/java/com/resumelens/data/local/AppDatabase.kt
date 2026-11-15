package com.resumelens.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [AnalysisEntity::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun analysisDao(): AnalysisDao

    companion object {
        const val NAME = "resumelens.db"
    }
}
