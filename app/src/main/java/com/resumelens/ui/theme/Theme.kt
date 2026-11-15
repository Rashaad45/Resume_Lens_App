package com.resumelens.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Brand palette. Dynamic colour (Material You) is off on purpose so the app keeps its identity.
private val Indigo = Color(0xFF3346D3)
private val IndigoLight = Color(0xFFAEB8FF)

private val LightColors = lightColorScheme(
    primary = Indigo,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0E4FF),
    onPrimaryContainer = Color(0xFF0E1A6B),
    secondary = Color(0xFF0E7C66),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD3F2E9),
    onSecondaryContainer = Color(0xFF00382D),
    tertiary = Color(0xFFB45309),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE8D1),
    onTertiaryContainer = Color(0xFF4A2000),
    background = Color(0xFFF6F6F3),
    onBackground = Color(0xFF16171D),
    surface = Color(0xFFF6F6F3),
    onSurface = Color(0xFF16171D),
    surfaceVariant = Color(0xFFE9E9EF),
    onSurfaceVariant = Color(0xFF5B5D6B),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFF0F0F3),
    surfaceContainerHigh = Color(0xFFEAEAEF),
    surfaceContainerHighest = Color(0xFFE4E4EA),
    outline = Color(0xFFC4C5D0),
    outlineVariant = Color(0xFFE1E1E8),
    error = Color(0xFFB3261E),
    errorContainer = Color(0xFFFDE7E6),
    onErrorContainer = Color(0xFF5F120D),
)

private val DarkColors = darkColorScheme(
    primary = IndigoLight,
    onPrimary = Color(0xFF0E1A6B),
    primaryContainer = Color(0xFF26338F),
    onPrimaryContainer = Color(0xFFE0E4FF),
    secondary = Color(0xFF6FD6BC),
    onSecondary = Color(0xFF00382D),
    secondaryContainer = Color(0xFF0B4A3D),
    onSecondaryContainer = Color(0xFFD3F2E9),
    tertiary = Color(0xFFFFB68A),
    onTertiary = Color(0xFF4A2000),
    tertiaryContainer = Color(0xFF5C3008),
    onTertiaryContainer = Color(0xFFFFE8D1),
    background = Color(0xFF0F1015),
    onBackground = Color(0xFFE6E6EE),
    surface = Color(0xFF0F1015),
    onSurface = Color(0xFFE6E6EE),
    surfaceVariant = Color(0xFF262833),
    onSurfaceVariant = Color(0xFFA4A6B5),
    surfaceContainerLowest = Color(0xFF0B0C10),
    surfaceContainerLow = Color(0xFF181920),
    surfaceContainer = Color(0xFF1C1D25),
    surfaceContainerHigh = Color(0xFF23242D),
    surfaceContainerHighest = Color(0xFF2B2C36),
    outline = Color(0xFF454857),
    outlineVariant = Color(0xFF2E303B),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF5F120D),
    onErrorContainer = Color(0xFFFDE7E6),
)

private val Sans = FontFamily.SansSerif

private val AppTypography = Typography(
    displaySmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 40.sp, lineHeight = 44.sp, letterSpacing = (-1).sp),
    headlineMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 34.sp, letterSpacing = (-0.5).sp),
    headlineSmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = (-0.25).sp),
    titleLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 17.sp),
    labelLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    labelMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp),
    labelSmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.8.sp),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

/** Colours for score bands; not part of the Material scheme, so they're provided separately. */
@Immutable
data class ScoreColors(val strong: Color, val fair: Color, val weak: Color)

private val LightScoreColors = ScoreColors(strong = Color(0xFF0E9F7E), fair = Color(0xFFE59A12), weak = Color(0xFFD9483B))
private val DarkScoreColors = ScoreColors(strong = Color(0xFF5BD6B4), fair = Color(0xFFFFC266), weak = Color(0xFFFF8A7E))

val LocalScoreColors = staticCompositionLocalOf { LightScoreColors }

@Composable
fun ResumeLensTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    androidx.compose.runtime.CompositionLocalProvider(
        LocalScoreColors provides if (darkTheme) DarkScoreColors else LightScoreColors,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}

/** One spacing scale for the whole app (4dp grid). */
object Spacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
    val screen = 20.dp
}

enum class ScoreBand { Strong, Fair, Weak }

fun scoreBand(score: Int): ScoreBand = when {
    score >= 75 -> ScoreBand.Strong
    score >= 50 -> ScoreBand.Fair
    else -> ScoreBand.Weak
}

@Composable
fun scoreColor(score: Int): Color = with(LocalScoreColors.current) {
    when (scoreBand(score)) {
        ScoreBand.Strong -> strong
        ScoreBand.Fair -> fair
        ScoreBand.Weak -> weak
    }
}
