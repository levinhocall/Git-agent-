package com.levinhocall.gitagent.ai

import java.io.InterruptedIOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlin.coroutines.cancellation.CancellationException

object ProviderErrorMapper {
    fun fromHttp(statusCode: Int, body: String): ProviderError {
        val details = body.take(300).ifBlank { "No response body" }
        return when (statusCode) {
            401, 403 -> ProviderError.Authentication("Authentication failed ($statusCode): $details")
            408 -> ProviderError.Timeout("Request timed out: $details")
            429 -> ProviderError.RateLimited("Rate limited: $details")
            in 500..599 -> ProviderError.Service("Provider unavailable ($statusCode): $details")
            else -> ProviderError.Unknown("Provider request failed ($statusCode): $details")
        }
    }

    fun fromThrowable(throwable: Throwable): ProviderError = when (throwable) {
        is CancellationException -> ProviderError.Cancelled("Request cancelled")
        is SocketTimeoutException, is InterruptedIOException -> ProviderError.Timeout("Network timeout")
        is UnknownHostException -> ProviderError.Network("Unable to reach provider")
        else -> ProviderError.Network(throwable.message ?: "Network request failed")
    }
}
