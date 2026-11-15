package com.resumelens.data

import com.resumelens.data.remote.ErrorMapper
import com.resumelens.domain.model.AppError
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class ErrorMapperTest {

    private fun http(code: Int, body: String = "{}") =
        HttpException(Response.error<Any>(code, body.toResponseBody()))

    @Test
    fun `http status codes map to specific errors`() {
        assertEquals(AppError.InvalidApiKey, ErrorMapper.map(http(400, """{"error":{"message":"API key not valid."}}""")))
        assertEquals(AppError.InvalidApiKey, ErrorMapper.map(http(403)))
        assertEquals(AppError.ModelUnavailable, ErrorMapper.map(http(404)))
        assertEquals(AppError.RateLimited, ErrorMapper.map(http(429)))
        assertEquals(AppError.Server(503), ErrorMapper.map(http(503)))
        assertEquals(AppError.Server(400), ErrorMapper.map(http(400, "bad request")))
    }

    @Test
    fun `io failures map to connectivity errors`() {
        assertEquals(AppError.Timeout, ErrorMapper.map(SocketTimeoutException()))
        assertEquals(AppError.NoConnection, ErrorMapper.map(UnknownHostException()))
        assertEquals(AppError.NoConnection, ErrorMapper.map(IOException()))
    }

    @Test
    fun `unexpected exceptions map to Unknown`() {
        assertEquals(AppError.Unknown, ErrorMapper.map(IllegalStateException()))
    }
}
