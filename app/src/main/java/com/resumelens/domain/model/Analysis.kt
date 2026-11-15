package com.resumelens.domain.model

/** A saved analysis: what was compared, when, and what the model concluded. */
data class Analysis(
    val id: Long,
    val createdAtMillis: Long,
    val resumeFileName: String,
    val jobTitle: String,
    val result: AnalysisResult,
)

data class AnalysisResult(
    /** 0..100, always clamped before it reaches the UI. */
    val matchScore: Int,
    val summary: String,
    val missingSkills: List<String>,
    val weakBullets: List<BulletRewrite>,
    val atsKeywords: List<AtsKeyword>,
)

data class BulletRewrite(
    val original: String,
    val rewrite: String,
    val reason: String,
)

data class AtsKeyword(
    val keyword: String,
    val presentInResume: Boolean,
)

/** Text pulled out of the user's PDF, plus metadata shown on the picker card. */
data class ExtractedResume(
    val fileName: String,
    val pageCount: Int,
    val text: String,
)
