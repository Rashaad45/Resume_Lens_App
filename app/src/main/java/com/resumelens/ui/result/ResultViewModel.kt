package com.resumelens.ui.result

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.resumelens.domain.model.Analysis
import com.resumelens.domain.repository.AnalysisRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

sealed interface ResultUiState {
    data object Loading : ResultUiState
    data object NotFound : ResultUiState
    data class Success(val analysis: Analysis) : ResultUiState
}

/**
 * Reads the analysis from Room, not from the network call, so a fresh result and one opened
 * from History use the same code path. The id comes from the type-safe nav route (ResultDestination),
 * which Navigation stores in SavedStateHandle under the property name.
 */
@HiltViewModel
class ResultViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    repository: AnalysisRepository,
) : ViewModel() {

    private val id: Long = checkNotNull(savedStateHandle["analysisId"]) { "analysisId missing from route" }

    val state: StateFlow<ResultUiState> = repository.observeAnalysis(id)
        .map { analysis -> analysis?.let(ResultUiState::Success) ?: ResultUiState.NotFound }
        // WhileSubscribed(5s): keep collecting through a rotation, stop once the screen is really gone.
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ResultUiState.Loading)
}
