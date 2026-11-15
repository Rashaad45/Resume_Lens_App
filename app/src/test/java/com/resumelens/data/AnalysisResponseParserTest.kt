package com.resumelens.data

import com.resumelens.data.remote.AnalysisResponseParser
import com.resumelens.data.remote.dto.Candidate
import com.resumelens.data.remote.dto.Content
import com.resumelens.data.remote.dto.GenerateContentResponse
import com.resumelens.data.remote.dto.Part
import com.resumelens.data.remote.dto.PromptFeedback
import com.resumelens.di.AppModule
import com.resumelens.domain.model.AppError
import com.resumelens.domain.model.AppResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AnalysisResponseParserTest {

    private val parser = AnalysisResponseParser(AppModule.provideJson())

    private val validJson = """
        {
          "matchScore": 78,
          "jobTitle": "Android Engineer",
          "summary": "Good fit.",
          "missingSkills": ["GraphQL", " ", "graphql", "CI/CD"],
          "weakBullets": [
            {"original": "Did stuff", "rewrite": "Built X that cut crashes by [X%]", "reason": "Impact"},
            {"original": "", "rewrite": "orphan", "reason": ""}
          ],
          "atsKeywords": [
            {"keyword": "Kotlin", "presentInResume": true},
            {"keyword": "Compose", "presentInResume": false}
          ]
        }
    """.trimIndent()

    private fun response(text: String, finishReason: String = "STOP") = GenerateContentResponse(
        candidates = listOf(Candidate(Content(parts = listOf(Part(text = text))), finishReason)),
    )

    @Test
    fun `parses a valid response and cleans up the lists`() {
        val result = parser.parse(response(validJson))

        assertTrue(result is AppResult.Success)
        val parsed = (result as AppResult.Success).value
        assertEquals("Android Engineer", parsed.jobTitle)
        assertEquals(78, parsed.result.matchScore)
        // blank and case-insensitive duplicates removed
        assertEquals(listOf("GraphQL", "CI/CD"), parsed.result.missingSkills)
        // bullet with an empty original dropped
        assertEquals(1, parsed.result.weakBullets.size)
        assertEquals(2, parsed.result.atsKeywords.size)
    }

    @Test
    fun `strips markdown code fences`() {
        val result = parser.parse(response("```json\n$validJson\n```"))

        assertTrue(result is AppResult.Success)
    }

    @Test
    fun `clamps out-of-range scores`() {
        val high = parser.parseJson("""{"matchScore": 140}""") as AppResult.Success
        val low = parser.parseJson("""{"matchScore": -5}""") as AppResult.Success

        assertEquals(100, high.value.result.matchScore)
        assertEquals(0, low.value.result.matchScore)
    }

    @Test
    fun `skips thought parts from thinking models`() {
        val resp = GenerateContentResponse(
            candidates = listOf(
                Candidate(Content(parts = listOf(Part(text = "Let me think...", thought = true), Part(text = validJson)))),
            ),
        )

        assertTrue(parser.parse(resp) is AppResult.Success)
    }

    @Test
    fun `malformed JSON becomes BadResponse instead of crashing`() {
        assertEquals(AppResult.Failure(AppError.BadResponse), parser.parse(response("{not json")))
    }

    @Test
    fun `no candidates becomes BadResponse`() {
        assertEquals(AppResult.Failure(AppError.BadResponse), parser.parse(GenerateContentResponse()))
    }

    @Test
    fun `safety blocks are reported as ContentBlocked`() {
        val blockedPrompt = GenerateContentResponse(promptFeedback = PromptFeedback(blockReason = "SAFETY"))

        assertEquals(AppResult.Failure(AppError.ContentBlocked), parser.parse(blockedPrompt))
        assertEquals(AppResult.Failure(AppError.ContentBlocked), parser.parse(response("", finishReason = "SAFETY")))
    }
}
