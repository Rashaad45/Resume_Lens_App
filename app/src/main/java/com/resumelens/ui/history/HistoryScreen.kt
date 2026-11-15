package com.resumelens.ui.history

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.resumelens.R
import com.resumelens.domain.model.Analysis
import com.resumelens.ui.components.EmptyState
import com.resumelens.ui.components.LoadingState
import com.resumelens.ui.theme.Spacing
import com.resumelens.ui.theme.scoreColor
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryRoute(
    onOpen: (Long) -> Unit,
    onStartAnalysis: () -> Unit,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val deletedMsg = stringResource(R.string.history_deleted)
    val undoLabel = stringResource(R.string.history_undo)

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is HistoryEvent.Deleted -> {
                    snackbar.currentSnackbarData?.dismiss()
                    val result = snackbar.showSnackbar(deletedMsg, undoLabel, duration = SnackbarDuration.Short)
                    if (result == SnackbarResult.ActionPerformed) viewModel.undoDelete(event.analysis)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.history_title), style = MaterialTheme.typography.headlineSmall) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        when (val s = state) {
            HistoryUiState.Loading -> LoadingState(stringResource(R.string.loading), Modifier.padding(padding))
            HistoryUiState.Empty -> EmptyState(
                icon = Icons.Outlined.History,
                title = stringResource(R.string.history_empty_title),
                body = stringResource(R.string.history_empty_body),
                actionLabel = stringResource(R.string.history_empty_action),
                onAction = onStartAnalysis,
                modifier = Modifier.padding(padding),
            )
            is HistoryUiState.Content -> LazyColumn(
                contentPadding = PaddingValues(
                    start = Spacing.screen,
                    end = Spacing.screen,
                    top = padding.calculateTopPadding(),
                    bottom = padding.calculateBottomPadding() + Spacing.md,
                ),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                // Stable keys let Compose animate and keep each row's swipe state when the list changes.
                items(s.items, key = { it.id }) { analysis ->
                    SwipeToDeleteRow(
                        analysis = analysis,
                        onClick = { onOpen(analysis.id) },
                        onDelete = { viewModel.delete(analysis) },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
    }
}

@Composable
private fun SwipeToDeleteRow(
    analysis: Analysis,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dismissState = rememberSwipeToDismissBoxState()
    val deleteLabel = stringResource(R.string.history_delete)

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier.semantics {
            // Swiping isn't accessible to everyone; TalkBack users get a "Delete" action instead.
            customActions = listOf(CustomAccessibilityAction(deleteLabel) { onDelete(); true })
        },
        enableDismissFromStartToEnd = false,
        onDismiss = { value -> if (value == SwipeToDismissBoxValue.EndToStart) onDelete() },
        backgroundContent = {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.errorContainer, MaterialTheme.shapes.medium)
                    .padding(horizontal = Spacing.lg),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(Icons.Outlined.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
            }
        },
    ) {
        HistoryItem(analysis, onClick)
    }
}

@Composable
private fun HistoryItem(analysis: Analysis, onClick: () -> Unit) {
    val score = analysis.result.matchScore
    val color = scoreColor(score)
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(Modifier.padding(Spacing.md), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(52.dp)
                    .background(color.copy(alpha = 0.14f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(score.toString(), style = MaterialTheme.typography.titleLarge, color = color)
            }
            Spacer(Modifier.width(Spacing.md))
            Column(Modifier.weight(1f)) {
                Text(
                    analysis.jobTitle.ifBlank { stringResource(R.string.history_untitled) },
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    analysis.resumeFileName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    formatDate(analysis.createdAtMillis),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private val dateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)

private fun formatDate(millis: Long): String =
    dateFormatter.format(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()))
