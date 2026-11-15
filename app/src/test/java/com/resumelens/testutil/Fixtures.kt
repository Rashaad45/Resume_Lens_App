package com.resumelens.testutil

import com.resumelens.domain.model.Analysis
import com.resumelens.domain.model.AnalysisResult
import com.resumelens.domain.model.AtsKeyword
import com.resumelens.domain.model.BulletRewrite
import com.resumelens.domain.model.ExtractedResume

object Fixtures {
    val resumeText = "Software engineer with 3 years of Kotlin and Android experience. ".repeat(5)
    val jobDescription = "We are hiring an Android developer with Kotlin, Jetpack Compose, and CI/CD experience. ".repeat(2)

    val extractedResume = ExtractedResume(fileName = "jane_resume.pdf", pageCount = 1, text = resumeText)

    fun analysis(id: Long = 1L, score: Int = 72) = Analysis(
        id = id,
        createdAtMillis = 1_700_000_000_000,
        resumeFileName = "jane_resume.pdf",
        jobTitle = "Android Developer",
        result = AnalysisResult(
            matchScore = score,
            summary = "Solid Android background.",
            missingSkills = listOf("CI/CD"),
            weakBullets = listOf(BulletRewrite("Worked on app", "Shipped 3 features to 50k users", "Adds impact")),
            atsKeywords = listOf(AtsKeyword("Kotlin", true), AtsKeyword("Compose", false)),
        ),
    )
}
