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

class GeminiProviderTest {
    @Test
    fun mapsAuthFailure() = runTest {
        val server = MockWebServer()
        server.enqueue(MockResponse().setResponseCode(401).setBody("unauthorized"))
        server.start()

        val provider = GeminiProvider(
            client = OkHttpClient(),
            defaultEndpoint = server.url("/").toString()
        )

        val result = provider.complete(
            AiRequest(
                model = AiModel("gemini-1.5-flash", "Gemini 1.5 Flash", AiProviderId.GEMINI),
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
                  "candidates": [
                    {
                      "content": {
                        "parts": [{ "text": "Gemini response" }]
                      }
                    }
                  ],
                  "usageMetadata": {
                    "promptTokenCount": 4,
                    "candidatesTokenCount": 6,
                    "totalTokenCount": 10
                  }
                }
                """.trimIndent()
            )
        )
        server.start()

        val provider = GeminiProvider(
            client = OkHttpClient(),
            defaultEndpoint = server.url("/").toString()
        )

        val result = provider.complete(
            AiRequest(
                model = AiModel("gemini-1.5-flash", "Gemini 1.5 Flash", AiProviderId.GEMINI),
                messages = listOf(AiMessage(AiRole.USER, "hello")),
                apiKey = "test-key"
            )
        )

        assertEquals("Gemini response", result.message?.content)
        assertEquals(10, result.usage?.totalTokens)
        server.shutdown()
    }
}
