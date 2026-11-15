package com.resumelens.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Score and title get their own columns because the History list shows and sorts by them.
 * The full result is stored as one JSON column: it is always read as a whole and never
 * queried by its parts, so normalising it into 4 tables would add joins for no benefit.
 */
@Entity(tableName = "analyses")
data class AnalysisEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "resume_file_name") val resumeFileName: String,
    @ColumnInfo(name = "job_title") val jobTitle: String,
    @ColumnInfo(name = "match_score") val matchScore: Int,
    @ColumnInfo(name = "result_json") val resultJson: String,
)
