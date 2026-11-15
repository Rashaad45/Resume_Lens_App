package com.resumelens.ui

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.resumelens.domain.model.Analysis
import com.resumelens.domain.repository.AnalysisRepository
import com.resumelens.testutil.Fixtures
import com.resumelens.testutil.MainDispatcherRule
import com.resumelens.ui.history.HistoryEvent
import com.resumelens.ui.history.HistoryUiState
import com.resumelens.ui.history.HistoryViewModel
import com.resumelens.ui.result.ResultUiState
import com.resumelens.ui.result.ResultViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class HistoryViewModelTest {

    @get:Rule val mainRule = MainDispatcherRule()

    private val repository: AnalysisRepository = mockk()

    @Test
    fun `empty database shows the empty state`() = runTest {
        every { repository.observeHistory() } returns flowOf(emptyList())

        HistoryViewModel(repository).state.test {
            assertEquals(HistoryUiState.Empty, awaitItem())
        }
    }

    @Test
    fun `rows are shown as content and update live`() = runTest {
        val rows = MutableStateFlow(listOf(Fixtures.analysis(1)))
        every { repository.observeHistory() } returns rows

        HistoryViewModel(repository).state.test {
            assertEquals(HistoryUiState.Content(listOf(Fixtures.analysis(1))), awaitItem())
            rows.value = emptyList()
            assertEquals(HistoryUiState.Empty, awaitItem())
        }
    }

    @Test
    fun `delete emits an undo event and undo restores the row`() = runTest {
        every { repository.observeHistory() } returns flowOf(emptyList())
        coEvery { repository.delete(any()) } just runs
        coEvery { repository.restore(any()) } just runs
        val vm = HistoryViewModel(repository)
        val item = Fixtures.analysis(5)

        vm.events.test {
            vm.delete(item)
            assertEquals(HistoryEvent.Deleted(item), awaitItem())
        }
        vm.undoDelete(item)

        coVerify { repository.delete(item) }
        coVerify { repository.restore(item) }
    }
}

class ResultViewModelTest {

    @get:Rule val mainRule = MainDispatcherRule()

    private val repository: AnalysisRepository = mockk()

    private fun vm(id: Long) = ResultViewModel(SavedStateHandle(mapOf("analysisId" to id)), repository)

    @Test
    fun `existing analysis is shown`() = runTest {
        every { repository.observeAnalysis(3) } returns flowOf(Fixtures.analysis(3))

        vm(3).state.test {
            assertEquals(ResultUiState.Success(Fixtures.analysis(3)), awaitItem())
        }
    }

    @Test
    fun `missing analysis shows not-found instead of crashing`() = runTest {
        every { repository.observeAnalysis(9) } returns flowOf<Analysis?>(null)

        vm(9).state.test {
            assertEquals(ResultUiState.NotFound, awaitItem())
        }
    }
}
