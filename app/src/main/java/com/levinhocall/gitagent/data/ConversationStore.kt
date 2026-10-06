package com.levinhocall.gitagent.data

import android.content.Context
import com.levinhocall.gitagent.ai.AiMessage
import com.levinhocall.gitagent.ai.AiRole
import org.json.JSONArray
import org.json.JSONObject

interface ConversationStore {
    suspend fun loadConversation(): List<AiMessage>
    suspend fun saveConversation(messages: List<AiMessage>)
    suspend fun clearConversation()
    suspend fun findRelevantMemories(query: String, limit: Int = 5): List<AiMessage>
}

class SharedPrefsConversationStore(context: Context) : ConversationStore {
    private val prefs = context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)

    override suspend fun loadConversation(): List<AiMessage> {
        val raw = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    val role = runCatching { AiRole.valueOf(item.getString("role")) }.getOrDefault(AiRole.USER)
                    add(
                        AiMessage(
                            role = role,
                            content = item.optString("content"),
                            timestampMillis = item.optLong("timestamp", System.currentTimeMillis())
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    override suspend fun saveConversation(messages: List<AiMessage>) {
        val array = JSONArray().apply {
            messages.takeLast(MAX_STORED_MESSAGES).forEach { message ->
                put(
                    JSONObject()
                        .put("role", message.role.name)
                        .put("content", message.content)
                        .put("timestamp", message.timestampMillis)
                )
            }
        }
        prefs.edit().putString(KEY_HISTORY, array.toString()).apply()
    }

    override suspend fun clearConversation() {
        prefs.edit().remove(KEY_HISTORY).apply()
    }

    override suspend fun findRelevantMemories(query: String, limit: Int): List<AiMessage> {
        if (query.isBlank()) return emptyList()
        return loadConversation()
            .filter { it.content.contains(query, ignoreCase = true) }
            .takeLast(limit)
    }

    private companion object {
        const val PREF_FILE = "aurix_conversation"
        const val KEY_HISTORY = "history"
        const val MAX_STORED_MESSAGES = 200
    }
}

class InMemoryConversationStore : ConversationStore {
    private var messages: List<AiMessage> = emptyList()

    override suspend fun loadConversation(): List<AiMessage> = messages

    override suspend fun saveConversation(messages: List<AiMessage>) {
        this.messages = messages
    }

    override suspend fun clearConversation() {
        messages = emptyList()
    }

    override suspend fun findRelevantMemories(query: String, limit: Int): List<AiMessage> {
        return messages.filter { it.content.contains(query, ignoreCase = true) }.takeLast(limit)
    }
}
