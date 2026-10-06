package com.levinhocall.gitagent

import com.levinhocall.gitagent.ai.AiMessage
import com.levinhocall.gitagent.ai.AiProviderId
import com.levinhocall.gitagent.ai.AiResult
import com.levinhocall.gitagent.ai.ProviderHealth
import com.levinhocall.gitagent.ai.providers.GeminiProvider
import com.levinhocall.gitagent.ai.providers.OpenRouterProvider
import com.levinhocall.gitagent.data.AndroidKeystoreCredentialStore
import com.levinhocall.gitagent.data.ConversationStore
import com.levinhocall.gitagent.data.CredentialStore
import com.levinhocall.gitagent.data.ProviderGateway
import com.levinhocall.gitagent.data.SharedPrefsConversationStore
import com.levinhocall.gitagent.data.ToolRegistry

class AssistantRepository(
    private val providerGateway: ProviderGateway,
    private val conversationStore: ConversationStore,
    val toolRegistry: ToolRegistry = ToolRegistry.phaseOneDefault()
) {
    suspend fun loadConversation(): List<AiMessage> = conversationStore.loadConversation()

    suspend fun send(
        providerId: AiProviderId,
        modelId: String,
        conversation: List<AiMessage>
    ): AiResult {
        val result = providerGateway.complete(providerId, modelId, conversation)
        if (result.isSuccess && result.message != null) {
            conversationStore.saveConversation(conversation + result.message)
        } else {
            conversationStore.saveConversation(conversation)
        }
        return result
    }

    suspend fun clearConversation() {
        conversationStore.clearConversation()
    }

    suspend fun findRelevantMemories(query: String): List<AiMessage> {
        return conversationStore.findRelevantMemories(query)
    }

    fun providers(): List<AiProviderId> = providerGateway.providerIds()

    fun models(providerId: AiProviderId) = providerGateway.models(providerId)

    suspend fun providerHealth(providerId: AiProviderId): ProviderHealth = providerGateway.health(providerId)

    suspend fun saveApiKey(providerId: AiProviderId, apiKey: String) = providerGateway.saveApiKey(providerId, apiKey)

    suspend fun clearApiKey(providerId: AiProviderId) = providerGateway.clearApiKey(providerId)

    companion object {
        fun createDefault(
            credentialStore: CredentialStore,
            conversationStore: ConversationStore
        ): AssistantRepository {
            val gateway = ProviderGateway(
                providers = mapOf(
                    AiProviderId.OPENROUTER to OpenRouterProvider(),
                    AiProviderId.GEMINI to GeminiProvider()
                ),
                credentialStore = credentialStore
            )
            return AssistantRepository(gateway, conversationStore)
        }

        fun createAndroid(appContext: android.content.Context): AssistantRepository {
            return createDefault(
                credentialStore = AndroidKeystoreCredentialStore(appContext),
                conversationStore = SharedPrefsConversationStore(appContext)
            )
        }
    }
}
