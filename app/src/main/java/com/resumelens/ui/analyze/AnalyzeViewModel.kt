package com.resumelens.ui.analyze

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.resumelens.domain.model.AppError
import com.resumelens.domain.model.AppResult
import com.resumelens.domain.model.ExtractedResume
import com.resumelens.domain.repository.ResumeTextExtractor
import com.resumelens.domain.usecase.AnalyzeResumeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ResumeState {
    data object None : ResumeState
    data object Loading : ResumeState
    data class Loaded(val resume: ExtractedResume) : ResumeState
    data class Error(val error: AppError) : ResumeState
}

data class AnalyzeUiState(
    val resume: ResumeState = ResumeState.None,
    val jobDescription: String = "",
    val isAnalyzing: Boolean = false,
    val analysisError: AppError? = null,
) {
    val jobDescriptionLength: Int get() = jobDescription.trim().length
    val canAnalyze: Boolean
        get() = resume is ResumeState.Loaded &&
            jobDescriptionLength >= AnalyzeResumeUseCase.MIN_JOB_CHARS &&
            !isAnalyzing
}

sealed interface AnalyzeEvent {
    data class OpenResult(val analysisId: Long) : AnalyzeEvent
}

@HiltViewModel
class AnalyzeViewModel @Inject constructor(
    private val extractor: ResumeTextExtractor,
    private val analyzeResume: AnalyzeResumeUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(AnalyzeUiState())
    val state: StateFlow<AnalyzeUiState> = _state.asStateFlow()

    // A Channel delivers each navigation event exactly once. A StateFlow would replay the event
    // after rotation and navigate twice.
    private val _events = Channel<AnalyzeEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var extractJob: Job? = null

    fun onResumePicked(uri: String?) {
        if (uri == null) return // the user cancelled the picker
        extractJob?.cancel()
        _state.update { it.copy(resume = ResumeState.Loading, analysisError = null) }
        extractJob = viewModelScope.launch {
            val next = when (val result = extractor.extract(uri)) {
                is AppResult.Success -> ResumeState.Loaded(result.value)
                is AppResult.Failure -> ResumeState.Error(result.error)
            }
            _state.update { it.copy(resume = next) }
        }
    }

    fun onRemoveResume() {
        extractJob?.cancel()
        _state.update { it.copy(resume = ResumeState.None) }
    }

    fun onJobDescriptionChange(text: String) {
        _state.update { it.copy(jobDescription = text.take(MAX_INPUT_CHARS), analysisError = null) }
    }

    fun onDismissError() = _state.update { it.copy(analysisError = null) }

    fun analyze() {
        val current = _state.value
        val resume = (current.resume as? ResumeState.Loaded)?.resume ?: return
        if (!current.canAnalyze) return

        _state.update { it.copy(isAnalyzing = true, analysisError = null) }
        viewModelScope.launch {
            when (val result = analyzeResume(resume.text, current.jobDescription, resume.fileName)) {
                is AppResult.Success -> {
                    _state.update { it.copy(isAnalyzing = false) }
                    _events.send(AnalyzeEvent.OpenResult(result.value))
                }
                is AppResult.Failure -> _state.update { it.copy(isAnalyzing = false, analysisError = result.error) }
            }
        }
    }

    private companion object {
        const val MAX_INPUT_CHARS = 20_000
    }
}
