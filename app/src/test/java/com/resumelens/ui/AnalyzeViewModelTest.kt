package com.resumelens.ui

import app.cash.turbine.test
import com.resumelens.domain.model.AppError
import com.resumelens.domain.model.AppResult
import com.resumelens.domain.repository.ResumeTextExtractor
import com.resumelens.domain.usecase.AnalyzeResumeUseCase
import com.resumelens.testutil.Fixtures
import com.resumelens.testutil.MainDispatcherRule
import com.resumelens.ui.analyze.AnalyzeEvent
import com.resumelens.ui.analyze.AnalyzeViewModel
import com.resumelens.ui.analyze.ResumeState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AnalyzeViewModelTest {

    @get:Rule val mainRule = MainDispatcherRule()

    private val extractor: ResumeTextExtractor = mockk()
    private val useCase: AnalyzeResumeUseCase = mockk()
    private val viewModel by lazy { AnalyzeViewModel(extractor, useCase) }

    private fun loadResume() {
        coEvery { extractor.extract("content://resume") } returns AppResult.Success(Fixtures.extractedResume)
        viewModel.onResumePicked("content://resume")
    }

    @Test
    fun `cancelling the picker leaves state untouched`() {
        viewModel.onResumePicked(null)

        assertEquals(ResumeState.None, viewModel.state.value.resume)
        coVerify(exactly = 0) { extractor.extract(any()) }
    }

    @Test
    fun `picking a readable PDF shows it as loaded`() {
        loadResume()

        assertEquals(ResumeState.Loaded(Fixtures.extractedResume), viewModel.state.value.resume)
    }

    @Test
    fun `an unreadable PDF surfaces the error`() {
        coEvery { extractor.extract(any()) } returns AppResult.Failure(AppError.PdfNoText)

        viewModel.onResumePicked("content://scan")

        assertEquals(ResumeState.Error(AppError.PdfNoText), viewModel.state.value.resume)
    }

    @Test
    fun `analyze is only enabled once both inputs are valid`() {
        assertFalse(viewModel.state.value.canAnalyze)

        loadResume()
        assertFalse(viewModel.state.value.canAnalyze)

        viewModel.onJobDescriptionChange(Fixtures.jobDescription)
        assertTrue(viewModel.state.value.canAnalyze)
    }

    @Test
    fun `successful analysis emits a navigation event`() = runTest {
        loadResume()
        viewModel.onJobDescriptionChange(Fixtures.jobDescription)
        coEvery { useCase(any(), any(), "jane_resume.pdf") } returns AppResult.Success(7L)

        viewModel.events.test {
            viewModel.analyze()
            assertEquals(AnalyzeEvent.OpenResult(7L), awaitItem())
        }
        assertFalse(viewModel.state.value.isAnalyzing)
    }

    @Test
    fun `failed analysis shows the error and typing clears it`() {
        loadResume()
        viewModel.onJobDescriptionChange(Fixtures.jobDescription)
        coEvery { useCase(any(), any(), any()) } returns AppResult.Failure(AppError.NoConnection)

        viewModel.analyze()
        assertEquals(AppError.NoConnection, viewModel.state.value.analysisError)
        assertFalse(viewModel.state.value.isAnalyzing)

        viewModel.onJobDescriptionChange(Fixtures.jobDescription + " more")
        assertNull(viewModel.state.value.analysisError)
    }

    @Test
    fun `analyze does nothing without a resume`() {
        viewModel.onJobDescriptionChange(Fixtures.jobDescription)

        viewModel.analyze()

        coVerify(exactly = 0) { useCase(any(), any(), any()) }
    }
}
