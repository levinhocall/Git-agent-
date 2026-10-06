package com.levinhocall.gitagent.ai.providers

import com.levinhocall.gitagent.ai.AiMessage
import com.levinhocall.gitagent.ai.AiModel
import com.levinhocall.gitagent.ai.AiProvider
import com.levinhocall.gitagent.ai.AiProviderId
import com.levinhocall.gitagent.ai.AiRequest
import com.levinhocall.gitagent.ai.AiResult
import com.levinhocall.gitagent.ai.AiRole
import com.levinhocall.gitagent.ai.ProviderError
import com.levinhocall.gitagent.ai.ProviderErrorMapper
import com.levinhocall.gitagent.ai.TokenUsage
import com.levinhocall.gitagent.ai.awaitResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class OpenRouterProvider(
    private val client: OkHttpClient = defaultClient(),
    override val defaultEndpoint: String = "https://openrouter.ai/api/v1"
) : AiProvider {
    override val id: AiProviderId = AiProviderId.OPENROUTER
    override val displayName: String = "OpenRouter"
    override val models: List<AiModel> = listOf(
        AiModel("openai/gpt-4o-mini", "GPT-4o mini", id),
        AiModel("anthropic/claude-3.5-sonnet", "Claude 3.5 Sonnet", id)
    )

    override suspend fun complete(request: AiRequest): AiResult = withContext(Dispatchers.IO) {
        if (request.apiKey.isBlank()) {
            return@withContext AiResult(error = ProviderError.Configuration("Missing OpenRouter API key"))
        }
        val payloadMessages = JSONArray().apply {
            request.messages.forEach { message ->
                put(JSONObject().put("role", message.role.name.lowercase()).put("content", message.content))
            }
        }
        val payload = JSONObject()
            .put("model", request.model.id)
            .put("messages", payloadMessages)

        val httpRequest = Request.Builder()
            .url(defaultEndpoint.trimEnd('/') + "/chat/completions")
            .addHeader("Authorization", "Bearer ".plus(request.apiKey))
            .addHeader("Content-Type", "application/json")
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        try {
            client.newCall(httpRequest).awaitResponse().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext AiResult(error = ProviderErrorMapper.fromHttp(response.code, body))
                }
                val root = JSONObject(body)
                val content = root
                    .getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .optString("content")
                    .trim()
                if (content.isBlank()) {
                    return@withContext AiResult(error = ProviderError.Unknown("OpenRouter returned empty content"))
                }
                val usage = root.optJSONObject("usage")?.let {
                    TokenUsage(
                        promptTokens = it.optInt("prompt_tokens").takeIf { value -> value > 0 },
                        completionTokens = it.optInt("completion_tokens").takeIf { value -> value > 0 },
                        totalTokens = it.optInt("total_tokens").takeIf { value -> value > 0 }
                    )
                }
                return@withContext AiResult(
                    message = AiMessage(role = AiRole.ASSISTANT, content = content),
                    usage = usage
                )
            }
        } catch (throwable: Throwable) {
            return@withContext AiResult(error = ProviderErrorMapper.fromThrowable(throwable))
        }
    }

    companion object {
        private fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .callTimeout(100, TimeUnit.SECONDS)
            .build()
    }
}
