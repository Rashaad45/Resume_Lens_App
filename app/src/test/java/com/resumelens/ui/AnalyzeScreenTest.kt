package com.resumelens.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.resumelens.domain.model.AppError
import com.resumelens.testutil.Fixtures
import com.resumelens.ui.analyze.AnalyzeContent
import com.resumelens.ui.analyze.AnalyzeUiState
import com.resumelens.ui.analyze.ResumeState
import com.resumelens.ui.theme.ResumeLensTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.ConscryptMode
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.SQLiteMode

/**
 * Compose UI test that runs on the JVM through Robolectric, so `gradlew test` covers it without a
 * phone or emulator. It tests the stateless AnalyzeContent, which needs no Hilt or ViewModel.
 */
@RunWith(RobolectricTestRunner::class)
// Pure-Java modes: no native DLLs are extracted to %TEMP%, which Windows Smart App Control can block.
@GraphicsMode(GraphicsMode.Mode.LEGACY)
@ConscryptMode(ConscryptMode.Mode.OFF)
@SQLiteMode(SQLiteMode.Mode.LEGACY)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi") // a typical phone, not Robolectric's tiny default
class AnalyzeScreenTest {

    @get:Rule val compose = createComposeRule()

    private var analyzeClicks = 0
    private var retryClicks = 0

    private fun setContent(state: AnalyzeUiState) {
        compose.setContent {
            ResumeLensTheme {
                AnalyzeContent(
                    state = state,
                    onPickResume = {},
                    onRemoveResume = {},
                    onJobDescriptionChange = {},
                    onPaste = {},
                    onAnalyze = { if (state.analysisError != null) retryClicks++ else analyzeClicks++ },
                    onDismissError = {},
                )
            }
        }
    }

    @Test
    fun analyzeButton_disabledUntilInputsAreValid() {
        setContent(AnalyzeUiState())

        compose.onNodeWithTag("analyzeButton").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("Choose a PDF").assertIsDisplayed()
    }

    @Test
    fun analyzeButton_enabledAndClickableWithValidInputs() {
        setContent(
            AnalyzeUiState(
                resume = ResumeState.Loaded(Fixtures.extractedResume),
                jobDescription = Fixtures.jobDescription,
            ),
        )

        compose.onNodeWithText("jane_resume.pdf").assertIsDisplayed()
        compose.onNodeWithTag("analyzeButton").performScrollTo().assertIsEnabled().performClick()
        assertEquals(1, analyzeClicks)
    }

    @Test
    fun retryableError_showsBannerWithRetry() {
        setContent(
            AnalyzeUiState(
                resume = ResumeState.Loaded(Fixtures.extractedResume),
                jobDescription = Fixtures.jobDescription,
                analysisError = AppError.NoConnection,
            ),
        )

        compose.onNodeWithText("No internet connection", substring = true).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Try again").performScrollTo().performClick()
        assertEquals(1, retryClicks)
    }
}
