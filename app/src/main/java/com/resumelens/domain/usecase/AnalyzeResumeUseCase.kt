package com.resumelens.domain.usecase

import com.resumelens.domain.model.AppError
import com.resumelens.domain.model.AppResult
import com.resumelens.domain.repository.AnalysisRepository
import javax.inject.Inject

/**
 * Validates and normalises input before spending an API call on it.
 * Truncation keeps requests well inside the model's context window and the free-tier
 * token budget; a resume rarely exceeds ~12k characters.
 */
class AnalyzeResumeUseCase @Inject constructor(
    private val repository: AnalysisRepository,
) {
    suspend operator fun invoke(
        resumeText: String,
        jobDescription: String,
        resumeFileName: String,
    ): AppResult<Long> {
        val resume = resumeText.normalizeWhitespace()
        val job = jobDescription.normalizeWhitespace()

        if (resume.length < MIN_RESUME_CHARS) return AppResult.Failure(AppError.ResumeTooShort)
        if (job.length < MIN_JOB_CHARS) return AppResult.Failure(AppError.JobDescriptionTooShort)

        return repository.analyze(
            resumeText = resume.take(MAX_RESUME_CHARS),
            jobDescription = job.take(MAX_JOB_CHARS),
            resumeFileName = resumeFileName,
        )
    }

    private fun String.normalizeWhitespace(): String =
        replace(Regex("[ \\t\\u00A0]+"), " ")
            .replace(Regex("\\n{3,}"), "\n\n")
            .trim()

    companion object {
        const val MIN_RESUME_CHARS = 100
        const val MIN_JOB_CHARS = 80
        const val MAX_RESUME_CHARS = 15_000
        const val MAX_JOB_CHARS = 10_000
    }
}
