package com.resumelens.domain.repository

import com.resumelens.domain.model.Analysis
import com.resumelens.domain.model.AppResult
import kotlinx.coroutines.flow.Flow

interface AnalysisRepository {
    /** Runs the LLM analysis, persists it, and returns the new row id. */
    suspend fun analyze(
        resumeText: String,
        jobDescription: String,
        resumeFileName: String,
    ): AppResult<Long>

    fun observeHistory(): Flow<List<Analysis>>

    fun observeAnalysis(id: Long): Flow<Analysis?>

    suspend fun delete(analysis: Analysis)

    /** Re-inserts a deleted analysis with its original id (used for "Undo"). */
    suspend fun restore(analysis: Analysis)
}
