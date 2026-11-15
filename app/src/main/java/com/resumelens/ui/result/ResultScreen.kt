package com.resumelens.ui.result

import android.content.ClipData
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.resumelens.R
import com.resumelens.domain.model.Analysis
import com.resumelens.domain.model.AtsKeyword
import com.resumelens.domain.model.BulletRewrite
import com.resumelens.ui.components.EmptyState
import com.resumelens.ui.components.LoadingState
import com.resumelens.ui.components.ScoreGauge
import com.resumelens.ui.theme.LocalScoreColors
import com.resumelens.ui.theme.ScoreBand
import com.resumelens.ui.theme.Spacing
import com.resumelens.ui.theme.scoreBand
import com.resumelens.ui.theme.scoreColor
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultRoute(
    onBack: () -> Unit,
    viewModel: ResultViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val copiedMessage = stringResource(R.string.result_copied)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.result_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        when (val s = state) {
            ResultUiState.Loading -> LoadingState(stringResource(R.string.loading), Modifier.padding(padding))
            ResultUiState.NotFound -> EmptyState(
                icon = Icons.Outlined.SearchOff,
                title = stringResource(R.string.result_not_found_title),
                body = stringResource(R.string.result_not_found_body),
                modifier = Modifier.padding(padding),
            )
            is ResultUiState.Success -> ResultContent(
                analysis = s.analysis,
                contentPadding = padding,
                onCopy = { text ->
                    scope.launch {
                        clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("rewrite", text)))
                        snackbar.showSnackbar(copiedMessage)
                    }
                },
            )
        }
    }
}

@Composable
fun ResultContent(
    analysis: Analysis,
    contentPadding: PaddingValues,
    onCopy: (String) -> Unit,
) {
    val result = analysis.result
    LazyColumn(
        contentPadding = PaddingValues(
            start = Spacing.screen,
            end = Spacing.screen,
            top = contentPadding.calculateTopPadding() + Spacing.sm,
            bottom = contentPadding.calculateBottomPadding() + Spacing.xl,
        ),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        item(key = "score") { ScoreCard(analysis) }

        item(key = "skills") {
            SectionCard(title = stringResource(R.string.result_missing_skills)) {
                if (result.missingSkills.isEmpty()) {
                    MutedText(stringResource(R.string.result_missing_skills_empty))
                } else {
                    PillRow {
                        result.missingSkills.forEach { Pill(it, LocalScoreColors.current.weak) }
                    }
                }
            }
        }

        item(key = "keywords") { KeywordsCard(result.atsKeywords) }

        item(key = "bullets-header") {
            Text(
                stringResource(R.string.result_bullets),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = Spacing.sm),
            )
        }
        if (result.weakBullets.isEmpty()) {
            item(key = "bullets-empty") { MutedText(stringResource(R.string.result_bullets_empty)) }
        } else {
            itemsIndexed(result.weakBullets, key = { i, _ -> "bullet-$i" }) { _, bullet ->
                BulletCard(bullet, onCopy)
            }
        }
    }
}

@Composable
private fun ScoreCard(analysis: Analysis) {
    val score = analysis.result.matchScore
    val bandLabel = when (scoreBand(score)) {
        ScoreBand.Strong -> stringResource(R.string.result_band_strong)
        ScoreBand.Fair -> stringResource(R.string.result_band_fair)
        ScoreBand.Weak -> stringResource(R.string.result_band_weak)
    }
    CardSurface {
        Column(Modifier.fillMaxWidth().padding(Spacing.lg), horizontalAlignment = Alignment.CenterHorizontally) {
            ScoreGauge(score)
            Spacer(Modifier.height(Spacing.sm))
            Surface(shape = CircleShape, color = scoreColor(score).copy(alpha = 0.15f)) {
                Text(
                    bandLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = scoreColor(score),
                    modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.xs),
                )
            }
            Spacer(Modifier.height(Spacing.md))
            Text(
                analysis.jobTitle.ifBlank { stringResource(R.string.history_untitled) },
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
            )
            Text(
                analysis.resumeFileName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (analysis.result.summary.isNotBlank()) {
                Spacer(Modifier.height(Spacing.md))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(Spacing.md))
                Text(analysis.result.summary, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun KeywordsCard(keywords: List<AtsKeyword>) {
    val found = keywords.count { it.presentInResume }
    SectionCard(
        title = stringResource(R.string.result_keywords),
        subtitle = if (keywords.isNotEmpty()) stringResource(R.string.result_keywords_count, found, keywords.size) else null,
    ) {
        if (keywords.isEmpty()) {
            MutedText(stringResource(R.string.result_keywords_empty))
        } else {
            val colors = LocalScoreColors.current
            // Found keywords first, so the gaps stand out at the end.
            PillRow {
                keywords.sortedByDescending { it.presentInResume }.forEach { kw ->
                    Pill(
                        text = kw.keyword,
                        color = if (kw.presentInResume) colors.strong else MaterialTheme.colorScheme.onSurfaceVariant,
                        leadingCheck = kw.presentInResume,
                        stateDescription = stringResource(if (kw.presentInResume) R.string.keyword_found else R.string.keyword_missing),
                    )
                }
            }
        }
    }
}

@Composable
private fun BulletCard(bullet: BulletRewrite, onCopy: (String) -> Unit) {
    CardSurface {
        Column(Modifier.padding(start = Spacing.md, top = Spacing.md, bottom = Spacing.md, end = Spacing.xs)) {
            Label(stringResource(R.string.result_before), MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                bullet.original,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textDecoration = TextDecoration.LineThrough,
                modifier = Modifier.padding(end = Spacing.sm),
            )
            Spacer(Modifier.height(Spacing.md))
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Label(stringResource(R.string.result_after), MaterialTheme.colorScheme.primary)
                    Text(bullet.rewrite, style = MaterialTheme.typography.bodyMedium)
                }
                IconButton(onClick = { onCopy(bullet.rewrite) }) {
                    Icon(Icons.Outlined.ContentCopy, contentDescription = stringResource(R.string.result_copy),
                        modifier = Modifier.size(20.dp))
                }
            }
            if (bullet.reason.isNotBlank()) {
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    bullet.reason,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = Spacing.sm),
                )
            }
        }
    }
}

@Composable
private fun CardSurface(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        content = content,
    )
}

@Composable
private fun SectionCard(title: String, subtitle: String? = null, content: @Composable () -> Unit) {
    CardSurface {
        Column(Modifier.padding(Spacing.md)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(Spacing.md))
            content()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PillRow(content: @Composable () -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) { content() }
}

@Composable
private fun Pill(text: String, color: Color, leadingCheck: Boolean = false, stateDescription: String? = null) {
    Surface(
        modifier = if (stateDescription != null) Modifier.semantics { this.stateDescription = stateDescription } else Modifier,
        shape = CircleShape,
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.35f)),
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leadingCheck) {
                Icon(Icons.Outlined.Check, null, tint = color, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(Spacing.xs))
            }
            Text(
                text,
                style = MaterialTheme.typography.labelMedium,
                color = if (leadingCheck) color else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun Label(text: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = Spacing.xs)) {
        Box(Modifier.size(6.dp).background(color, CircleShape))
        Spacer(Modifier.width(6.dp))
        Text(text.uppercase(), style = MaterialTheme.typography.labelSmall, color = color)
    }
}

@Composable
private fun MutedText(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
