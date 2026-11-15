package com.resumelens.data.remote

import com.resumelens.data.remote.dto.GenerateContentRequest
import com.resumelens.data.remote.dto.GenerateContentResponse
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

/** The API key is added by an OkHttp interceptor (x-goog-api-key header), never as a URL query parameter. */
interface GeminiApi {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Body request: GenerateContentRequest,
    ): GenerateContentResponse
}

data class GeminiConfig(
    val apiKey: String,
    val model: String,
)
