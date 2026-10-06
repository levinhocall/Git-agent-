package com.levinhocall.gitagent.ai

import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.SocketTimeoutException

class ProviderErrorMapperTest {
    @Test
    fun mapsRateLimitStatus() {
        val error = ProviderErrorMapper.fromHttp(429, "too many requests")
        assertTrue(error is ProviderError.RateLimited)
    }

    @Test
    fun mapsAuthStatus() {
        val error = ProviderErrorMapper.fromHttp(401, "bad key")
        assertTrue(error is ProviderError.Authentication)
    }

    @Test
    fun mapsTimeoutThrowable() {
        val error = ProviderErrorMapper.fromThrowable(SocketTimeoutException("timeout"))
        assertTrue(error is ProviderError.Timeout)
    }
}
