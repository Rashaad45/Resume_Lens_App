package com.resumelens.data.remote

import com.resumelens.data.remote.dto.AnalysisPayload
import com.resumelens.data.remote.dto.GenerateContentResponse
import com.resumelens.domain.model.AnalysisResult
import com.resumelens.domain.model.AppError
import com.resumelens.domain.model.AppResult
import com.resumelens.domain.model.AtsKeyword
import com.resumelens.domain.model.BulletRewrite
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import javax.inject.Inject

/**
 * Turns a raw Gemini response into a domain model. Even with a response schema, LLM output is
 * untrusted input, so the parser strips stray markdown fences, clamps the score, drops blank
 * or duplicate entries, and caps list sizes. Anything unparseable becomes AppError.BadResponse.
 */
class AnalysisResponseParser @Inject constructor(
    private val json: Json,
) {
    data class Parsed(val jobTitle: String, val result: AnalysisResult)

    fun parse(response: GenerateContentResponse): AppResult<Parsed> {
        if (response.promptFeedback?.blockReason != null) {
            return AppResult.Failure(AppError.ContentBlocked)
        }
        val candidate = response.candidates.firstOrNull()
            ?: return AppResult.Failure(AppError.BadResponse)
        if (candidate.finishReason in BLOCKED_REASONS) {
            return AppResult.Failure(AppError.ContentBlocked)
        }

        val text = candidate.content?.parts.orEmpty()
            .filter { it.thought != true }
            .mapNotNull { it.text }
            .joinToString("")
        if (text.isBlank()) return AppResult.Failure(AppError.BadResponse)

        return parseJson(text)
    }

    internal fun parseJson(raw: String): AppResult<Parsed> {
        val payload = try {
            json.decodeFromString<AnalysisPayload>(raw.stripCodeFences())
        } catch (_: SerializationException) {
            return AppResult.Failure(AppError.BadResponse)
        } catch (_: IllegalArgumentException) {
            return AppResult.Failure(AppError.BadResponse)
        }
        return AppResult.Success(payload.toParsed())
    }

    private fun AnalysisPayload.toParsed() = Parsed(
        jobTitle = jobTitle.trim().take(80),
        result = AnalysisResult(
            matchScore = matchScore.coerceIn(0, 100),
            summary = summary.trim(),
            missingSkills = missingSkills.map { it.trim() }
                .filter { it.isNotEmpty() }
                .distinctBy { it.lowercase() }
                .take(MAX_SKILLS),
            weakBullets = weakBullets
                .filter { it.original.isNotBlank() && it.rewrite.isNotBlank() }
                .map { BulletRewrite(it.original.trim(), it.rewrite.trim(), it.reason.trim()) }
                .take(MAX_BULLETS),
            atsKeywords = atsKeywords
                .filter { it.keyword.isNotBlank() }
                .distinctBy { it.keyword.trim().lowercase() }
                .map { AtsKeyword(it.keyword.trim(), it.presentInResume) }
                .take(MAX_KEYWORDS),
        ),
    )

    private fun String.stripCodeFences(): String =
        trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()

    private companion object {
        val BLOCKED_REASONS = setOf("SAFETY", "RECITATION", "PROHIBITED_CONTENT", "BLOCKLIST", "SPII")
        const val MAX_SKILLS = 12
        const val MAX_BULLETS = 6
        const val MAX_KEYWORDS = 25
    }
}
