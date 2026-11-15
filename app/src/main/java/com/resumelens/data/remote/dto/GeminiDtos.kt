package com.resumelens.data.remote.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

// ---- Request: POST v1beta/models/{model}:generateContent ----

@Serializable
data class GenerateContentRequest(
    val contents: List<Content>,
    val systemInstruction: Content? = null,
    val generationConfig: GenerationConfig? = null,
)

@Serializable
data class Content(
    val parts: List<Part>,
    val role: String? = null,
)

@Serializable
data class Part(
    val text: String? = null,
    /** Set by "thinking" models on reasoning parts; those parts are skipped when reading the answer. */
    val thought: Boolean? = null,
)

@Serializable
data class GenerationConfig(
    val temperature: Double? = null,
    val responseMimeType: String? = null,
    /** OpenAPI-style schema. Gemini constrains its output to this shape. */
    val responseSchema: JsonObject? = null,
)

// ---- Response ----

@Serializable
data class GenerateContentResponse(
    val candidates: List<Candidate> = emptyList(),
    val promptFeedback: PromptFeedback? = null,
)

@Serializable
data class Candidate(
    val content: Content? = null,
    val finishReason: String? = null,
)

@Serializable
data class PromptFeedback(
    val blockReason: String? = null,
)

// ---- The JSON payload we asked the model to produce ----

@Serializable
data class AnalysisPayload(
    val matchScore: Int = 0,
    val jobTitle: String = "",
    val summary: String = "",
    val missingSkills: List<String> = emptyList(),
    val weakBullets: List<BulletPayload> = emptyList(),
    val atsKeywords: List<KeywordPayload> = emptyList(),
)

@Serializable
data class BulletPayload(
    val original: String = "",
    val rewrite: String = "",
    val reason: String = "",
)

@Serializable
data class KeywordPayload(
    val keyword: String = "",
    val presentInResume: Boolean = false,
)
