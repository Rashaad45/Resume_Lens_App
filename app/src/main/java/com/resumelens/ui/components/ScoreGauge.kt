package com.resumelens.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.resumelens.R
import com.resumelens.ui.theme.scoreColor

/**
 * A 240° arc gauge drawn on a Canvas. The arc starts at 150° (lower left) and sweeps clockwise.
 * The progress animates once from 0 when the gauge first appears.
 */
@Composable
fun ScoreGauge(
    score: Int,
    modifier: Modifier = Modifier,
    size: Dp = 168.dp,
    strokeWidth: Dp = 14.dp,
) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(score) {
        progress.animateTo(score / 100f, tween(durationMillis = 1100, easing = FastOutSlowInEasing))
    }
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val arcColor = scoreColor(score)
    val matchLabel = stringResource(R.string.result_match)

    Box(
        modifier = modifier
            .size(size)
            .semantics { contentDescription = "$score% $matchLabel" },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(size)) {
            val stroke = strokeWidth.toPx()
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
            val topLeft = Offset(stroke / 2, stroke / 2)
            drawArc(trackColor, START_ANGLE, SWEEP, useCenter = false, topLeft = topLeft, size = arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round))
            if (progress.value > 0f) {
                drawArc(arcColor, START_ANGLE, SWEEP * progress.value, useCenter = false, topLeft = topLeft,
                    size = arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = (progress.value * 100).toInt().toString(),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = matchLabel.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private const val START_ANGLE = 150f
private const val SWEEP = 240f
