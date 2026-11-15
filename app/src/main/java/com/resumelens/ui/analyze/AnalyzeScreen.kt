package com.resumelens.ui.analyze

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.resumelens.R
import com.resumelens.domain.usecase.AnalyzeResumeUseCase
import com.resumelens.ui.components.ErrorBanner
import com.resumelens.ui.components.isRetryable
import com.resumelens.ui.components.message
import com.resumelens.ui.theme.Spacing
import kotlinx.coroutines.launch
import java.text.NumberFormat

@Composable
fun AnalyzeRoute(
    onOpenResult: (Long) -> Unit,
    viewModel: AnalyzeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()

    // SAF picker: no storage permission needed, and the user picks from any provider (Drive, Downloads...).
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        viewModel.onResumePicked(uri?.toString())
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is AnalyzeEvent.OpenResult -> onOpenResult(event.analysisId)
            }
        }
    }

    AnalyzeContent(
        state = state,
        onPickResume = { picker.launch(arrayOf("application/pdf")) },
        onRemoveResume = viewModel::onRemoveResume,
        onJobDescriptionChange = viewModel::onJobDescriptionChange,
        onPaste = {
            scope.launch {
                val text = clipboard.getClipEntry()?.clipData?.takeIf { it.itemCount > 0 }
                    ?.getItemAt(0)?.text?.toString()
                if (!text.isNullOrBlank()) viewModel.onJobDescriptionChange(text)
            }
        },
        onAnalyze = viewModel::analyze,
        onDismissError = viewModel::onDismissError,
    )
}

/** Stateless: everything it shows comes from [state], which keeps it easy to preview and UI-test. */
@Composable
fun AnalyzeContent(
    state: AnalyzeUiState,
    onPickResume: () -> Unit,
    onRemoveResume: () -> Unit,
    onJobDescriptionChange: (String) -> Unit,
    onPaste: () -> Unit,
    onAnalyze: () -> Unit,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.screen, vertical = Spacing.lg),
    ) {
        Text(
            stringResource(R.string.analyze_title),
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(Modifier.height(Spacing.sm))
        Text(
            stringResource(R.string.analyze_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(Spacing.xl))
        SectionLabel(stringResource(R.string.analyze_step_resume))
        ResumeCard(state.resume, onPickResume, onRemoveResume)

        Spacer(Modifier.height(Spacing.lg))
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionLabel(stringResource(R.string.analyze_step_job), Modifier.weight(1f))
            if (state.jobDescription.isEmpty()) {
                TextButton(onClick = onPaste) {
                    Icon(Icons.Outlined.ContentPaste, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(Spacing.xs))
                    Text(stringResource(R.string.analyze_paste))
                }
            } else {
                TextButton(onClick = { onJobDescriptionChange("") }) { Text(stringResource(R.string.analyze_clear)) }
            }
        }
        OutlinedTextField(
            value = state.jobDescription,
            onValueChange = onJobDescriptionChange,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 180.dp, max = 320.dp)
                .testTag("jobDescriptionField"),
            placeholder = { Text(stringResource(R.string.analyze_job_placeholder)) },
            shape = MaterialTheme.shapes.medium,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            ),
            enabled = !state.isAnalyzing,
        )
        Spacer(Modifier.height(Spacing.xs))
        val min = AnalyzeResumeUseCase.MIN_JOB_CHARS
        Text(
            text = if (state.jobDescriptionLength >= min) {
                stringResource(R.string.analyze_job_counter_ok, NumberFormat.getIntegerInstance().format(state.jobDescriptionLength))
            } else {
                stringResource(R.string.analyze_job_counter, state.jobDescriptionLength, min)
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.End),
        )

        AnimatedVisibility(visible = state.analysisError != null) {
            state.analysisError?.let { error ->
                ErrorBanner(
                    message = error.message(),
                    modifier = Modifier.padding(top = Spacing.md),
                    onRetry = if (error.isRetryable) onAnalyze else null,
                    onDismiss = onDismissError,
                )
            }
        }

        Spacer(Modifier.height(Spacing.lg))
        Button(
            onClick = onAnalyze,
            enabled = state.canAnalyze,
            modifier = Modifier.fillMaxWidth().height(56.dp).testTag("analyzeButton"),
            shape = MaterialTheme.shapes.medium,
        ) {
            if (state.isAnalyzing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Text(stringResource(R.string.analyze_button), style = MaterialTheme.typography.titleMedium)
            }
        }
        if (state.isAnalyzing) {
            Spacer(Modifier.height(Spacing.sm))
            Text(
                stringResource(R.string.analyze_running),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
        Spacer(Modifier.height(Spacing.md))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Lock, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(Spacing.xs))
            Text(
                stringResource(R.string.analyze_privacy),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(bottom = Spacing.sm),
    )
}

@Composable
private fun ResumeCard(state: ResumeState, onPick: () -> Unit, onRemove: () -> Unit) {
    when (state) {
        ResumeState.None, is ResumeState.Error -> Column {
            Surface(
                onClick = onPick,
                modifier = Modifier.fillMaxWidth().testTag("pickResume"),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
            ) {
                Column(
                    Modifier.padding(vertical = Spacing.lg, horizontal = Spacing.md),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(Icons.Outlined.UploadFile, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                    Spacer(Modifier.height(Spacing.sm))
                    Text(stringResource(R.string.analyze_pick_pdf), style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary)
                    Text(stringResource(R.string.analyze_pick_pdf_hint), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (state is ResumeState.Error) {
                ErrorBanner(message = state.error.message(), modifier = Modifier.padding(top = Spacing.sm))
            }
        }

        ResumeState.Loading -> Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
        ) {
            Row(Modifier.padding(Spacing.md), verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.5.dp)
                Spacer(Modifier.width(Spacing.md))
                Text(stringResource(R.string.analyze_reading_pdf), style = MaterialTheme.typography.bodyMedium)
            }
        }

        is ResumeState.Loaded -> Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Row(Modifier.padding(start = Spacing.md, top = Spacing.sm, bottom = Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(44.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.shapes.small),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.Description, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                Spacer(Modifier.width(Spacing.md))
                Column(Modifier.weight(1f)) {
                    Text(state.resume.fileName, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        stringResource(
                            R.string.analyze_resume_meta,
                            state.resume.pageCount,
                            NumberFormat.getIntegerInstance().format(state.resume.text.length),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = onPick) { Text(stringResource(R.string.analyze_replace)) }
                IconButton(onClick = onRemove) {
                    Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.analyze_remove))
                }
            }
        }
    }
}
