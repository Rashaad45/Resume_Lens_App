package com.resumelens.data.local

import com.resumelens.domain.model.Analysis
import com.resumelens.domain.model.AnalysisResult
import com.resumelens.domain.model.AtsKeyword
import com.resumelens.domain.model.BulletRewrite
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject

/**
 * Converts between the Room entity and the domain model. The stored JSON has its own
 * @Serializable shape, separate from the domain classes, so the domain layer stays free of
 * serialization annotations and the stored format can evolve on its own.
 */
class AnalysisMapper @Inject constructor(private val json: Json) {

    fun toDomain(entity: AnalysisEntity): Analysis? {
        val stored = runCatching { json.decodeFromString<StoredResult>(entity.resultJson) }
            .getOrNull() ?: return null // a corrupted row is skipped rather than crashing the list
        return Analysis(
            id = entity.id,
            createdAtMillis = entity.createdAt,
            resumeFileName = entity.resumeFileName,
            jobTitle = entity.jobTitle,
            result = AnalysisResult(
                matchScore = entity.matchScore,
                summary = stored.summary,
                missingSkills = stored.missingSkills,
                weakBullets = stored.weakBullets.map { BulletRewrite(it.original, it.rewrite, it.reason) },
                atsKeywords = stored.atsKeywords.map { AtsKeyword(it.keyword, it.present) },
            ),
        )
    }

    fun toEntity(analysis: Analysis): AnalysisEntity = AnalysisEntity(
        id = analysis.id,
        createdAt = analysis.createdAtMillis,
        resumeFileName = analysis.resumeFileName,
        jobTitle = analysis.jobTitle,
        matchScore = analysis.result.matchScore,
        resultJson = json.encodeToString(
            StoredResult.serializer(),
            StoredResult(
                summary = analysis.result.summary,
                missingSkills = analysis.result.missingSkills,
                weakBullets = analysis.result.weakBullets.map { StoredBullet(it.original, it.rewrite, it.reason) },
                atsKeywords = analysis.result.atsKeywords.map { StoredKeyword(it.keyword, it.presentInResume) },
            ),
        ),
    )

    @Serializable
    private data class StoredResult(
        val summary: String = "",
        val missingSkills: List<String> = emptyList(),
        val weakBullets: List<StoredBullet> = emptyList(),
        val atsKeywords: List<StoredKeyword> = emptyList(),
    )

    @Serializable
    private data class StoredBullet(val original: String, val rewrite: String, val reason: String = "")

    @Serializable
    private data class StoredKeyword(val keyword: String, val present: Boolean)
}
