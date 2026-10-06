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
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiProvider(
    private val client: OkHttpClient = defaultClient(),
    override val defaultEndpoint: String = "https://generativelanguage.googleapis.com/v1beta"
) : AiProvider {
    override val id: AiProviderId = AiProviderId.GEMINI
    override val displayName: String = "Gemini"
    override val models: List<AiModel> = listOf(
        AiModel("gemini-1.5-flash", "Gemini 1.5 Flash", id),
        AiModel("gemini-1.5-pro", "Gemini 1.5 Pro", id)
    )

    override suspend fun complete(request: AiRequest): AiResult = withContext(Dispatchers.IO) {
        if (request.apiKey.isBlank()) {
            return@withContext AiResult(error = ProviderError.Configuration("Missing Gemini API key"))
        }

        val contents = JSONArray().apply {
            request.messages.forEach { message ->
                val role = if (message.role == AiRole.ASSISTANT) "model" else "user"
                put(JSONObject().put("role", role).put("parts", JSONArray().put(JSONObject().put("text", message.content))))
            }
        }
        val payload = JSONObject().put("contents", contents)

        val endpoint = defaultEndpoint.trimEnd('/').toHttpUrl().newBuilder()
            .addPathSegment("models")
            .addPathSegment(request.model.id + ":generateContent")
            .addQueryParameter("key", request.apiKey)
            .build()

        val httpRequest = Request.Builder()
            .url(endpoint)
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
                val candidates = root.optJSONArray("candidates")
                val text = candidates
                    ?.optJSONObject(0)
                    ?.optJSONObject("content")
                    ?.optJSONArray("parts")
                    ?.optJSONObject(0)
                    ?.optString("text")
                    ?.trim()
                    .orEmpty()
                if (text.isBlank()) {
                    return@withContext AiResult(error = ProviderError.Unknown("Gemini returned empty content"))
                }
                val usageMeta = root.optJSONObject("usageMetadata")
                val usage = usageMeta?.let {
                    TokenUsage(
                        promptTokens = it.optInt("promptTokenCount").takeIf { value -> value > 0 },
                        completionTokens = it.optInt("candidatesTokenCount").takeIf { value -> value > 0 },
                        totalTokens = it.optInt("totalTokenCount").takeIf { value -> value > 0 }
                    )
                }
                return@withContext AiResult(
                    message = AiMessage(role = AiRole.ASSISTANT, content = text),
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
