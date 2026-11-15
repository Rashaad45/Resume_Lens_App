package com.resumelens.domain

import com.resumelens.domain.model.AppError
import com.resumelens.domain.model.AppResult
import com.resumelens.domain.repository.AnalysisRepository
import com.resumelens.domain.usecase.AnalyzeResumeUseCase
import com.resumelens.testutil.Fixtures
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AnalyzeResumeUseCaseTest {

    private val repository: AnalysisRepository = mockk()
    private val useCase = AnalyzeResumeUseCase(repository)

    @Test
    fun `rejects a resume that is too short without calling the API`() = runTest {
        val result = useCase("too short", Fixtures.jobDescription, "r.pdf")

        assertEquals(AppResult.Failure(AppError.ResumeTooShort), result)
        coVerify(exactly = 0) { repository.analyze(any(), any(), any()) }
    }

    @Test
    fun `rejects a job description that is too short`() = runTest {
        val result = useCase(Fixtures.resumeText, "Android dev", "r.pdf")

        assertEquals(AppResult.Failure(AppError.JobDescriptionTooShort), result)
    }

    @Test
    fun `whitespace-only padding does not count towards the minimum length`() = runTest {
        val padded = "Android dev" + " ".repeat(500)

        assertEquals(AppResult.Failure(AppError.JobDescriptionTooShort), useCase(Fixtures.resumeText, padded, "r.pdf"))
    }

    @Test
    fun `truncates very long input before sending it`() = runTest {
        val resumeSlot = slot<String>()
        coEvery { repository.analyze(capture(resumeSlot), any(), any()) } returns AppResult.Success(1L)

        useCase("a".repeat(50_000), Fixtures.jobDescription, "r.pdf")

        assertEquals(AnalyzeResumeUseCase.MAX_RESUME_CHARS, resumeSlot.captured.length)
    }

    @Test
    fun `valid input is passed through and the new id returned`() = runTest {
        coEvery { repository.analyze(any(), any(), "r.pdf") } returns AppResult.Success(42L)

        val result = useCase(Fixtures.resumeText, Fixtures.jobDescription, "r.pdf")

        assertEquals(AppResult.Success(42L), result)
    }

    @Test
    fun `repository failures are propagated unchanged`() = runTest {
        coEvery { repository.analyze(any(), any(), any()) } returns AppResult.Failure(AppError.RateLimited)

        val result = useCase(Fixtures.resumeText, Fixtures.jobDescription, "r.pdf")

        assertTrue(result is AppResult.Failure && result.error == AppError.RateLimited)
    }
}
