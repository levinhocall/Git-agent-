package com.levinhocall.gitagent.ai.providers

import com.levinhocall.gitagent.ai.AiMessage
import com.levinhocall.gitagent.ai.AiModel
import com.levinhocall.gitagent.ai.AiProviderId
import com.levinhocall.gitagent.ai.AiRequest
import com.levinhocall.gitagent.ai.AiRole
import com.levinhocall.gitagent.ai.ProviderError
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenRouterProviderTest {
    @Test
    fun mapsAuthFailure() = runTest {
        val server = MockWebServer()
        server.enqueue(MockResponse().setResponseCode(401).setBody("unauthorized"))
        server.start()

        val provider = OpenRouterProvider(
            client = OkHttpClient(),
            defaultEndpoint = server.url("/").toString()
        )

        val result = provider.complete(
            AiRequest(
                model = AiModel("openai/gpt-4o-mini", "GPT-4o mini", AiProviderId.OPENROUTER),
                messages = listOf(AiMessage(AiRole.USER, "hello")),
                apiKey = "test-key"
            )
        )

        assertTrue(result.error is ProviderError.Authentication)
        server.shutdown()
    }

    @Test
    fun parsesSuccessfulResponse() = runTest {
        val server = MockWebServer()
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {
                  "choices": [{"message": {"content": "Hello back"}}],
                  "usage": {"prompt_tokens": 2, "completion_tokens": 3, "total_tokens": 5}
                }
                """.trimIndent()
            )
        )
        server.start()

        val provider = OpenRouterProvider(
            client = OkHttpClient(),
            defaultEndpoint = server.url("/").toString()
        )

        val result = provider.complete(
            AiRequest(
                model = AiModel("openai/gpt-4o-mini", "GPT-4o mini", AiProviderId.OPENROUTER),
                messages = listOf(AiMessage(AiRole.USER, "hello")),
                apiKey = "test-key"
            )
        )

        assertEquals("Hello back", result.message?.content)
        assertEquals(5, result.usage?.totalTokens)
        server.shutdown()
    }
}
