package com.resumelens.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.resumelens.domain.model.Analysis
import com.resumelens.domain.repository.AnalysisRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface HistoryUiState {
    data object Loading : HistoryUiState
    data object Empty : HistoryUiState
    data class Content(val items: List<Analysis>) : HistoryUiState
}

sealed interface HistoryEvent {
    data class Deleted(val analysis: Analysis) : HistoryEvent
}

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: AnalysisRepository,
) : ViewModel() {

    val state: StateFlow<HistoryUiState> = repository.observeHistory()
        .map { if (it.isEmpty()) HistoryUiState.Empty else HistoryUiState.Content(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState.Loading)

    private val _events = Channel<HistoryEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun delete(analysis: Analysis) {
        viewModelScope.launch {
            repository.delete(analysis)
            _events.send(HistoryEvent.Deleted(analysis))
        }
    }

    /** Undo re-inserts the same row with its original id, so the list keeps its order. */
    fun undoDelete(analysis: Analysis) {
        viewModelScope.launch { repository.restore(analysis) }
    }
}
