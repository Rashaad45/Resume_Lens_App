package com.resumelens.data.remote

import com.resumelens.domain.model.AppError
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException
import java.io.InterruptedIOException
import java.net.UnknownHostException

/** Maps transport-level exceptions to user-meaningful errors. Pure function, so it is unit-tested directly. */
object ErrorMapper {
    fun map(t: Throwable): AppError = when (t) {
        is HttpException -> fromHttp(t.code(), t.response()?.errorBody()?.string().orEmpty())
        // OkHttp's call and read timeouts surface as InterruptedIOException subclasses.
        is InterruptedIOException -> AppError.Timeout
        is UnknownHostException -> AppError.NoConnection
        is IOException -> AppError.NoConnection
        is SerializationException -> AppError.BadResponse
        else -> AppError.Unknown
    }

    fun fromHttp(code: Int, body: String): AppError = when {
        // Gemini returns 400 INVALID_ARGUMENT with "API key not valid" for a bad key.
        code == 400 && body.contains("API key", ignoreCase = true) -> AppError.InvalidApiKey
        code == 401 || code == 403 -> AppError.InvalidApiKey
        code == 404 -> AppError.ModelUnavailable
        code == 429 -> AppError.RateLimited
        else -> AppError.Server(code)
    }
}
