package com.levinhocall.gitagent

import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class AssistantRepository(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS).readTimeout(60, TimeUnit.SECONDS).writeTimeout(20, TimeUnit.SECONDS).build()
) {
    suspend fun reply(messages: List<ChatMessage>): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.ASSISTANT_API_KEY
        if (apiKey.isBlank()) return@withContext demoReply(messages.last().text)
        val bodyMessages = JSONArray().apply { messages.forEach { put(JSONObject().put("role", it.role).put("content", it.text)) } }
        val payload = JSONObject().put("model", BuildConfig.ASSISTANT_MODEL).put("messages", bodyMessages)
        val request = Request.Builder().url(BuildConfig.ASSISTANT_BASE_URL.trimEnd('/') + "/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey").addHeader("Content-Type", "application/json")
            .post(payload.toString().toRequestBody("application/json".toMediaType())).build()
        client.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) error("AI service error (${response.code})")
            return@withContext JSONObject(text).getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content")
        }
    }

    private fun demoReply(prompt: String): String = "Demo mode: I received “$prompt”. Add ASSISTANT_API_KEY to local.properties for a live provider."
}
