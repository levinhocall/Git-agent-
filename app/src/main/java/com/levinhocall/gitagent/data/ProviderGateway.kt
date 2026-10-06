package com.levinhocall.gitagent.data

import com.levinhocall.gitagent.ai.AiMessage
import com.levinhocall.gitagent.ai.AiModel
import com.levinhocall.gitagent.ai.AiProvider
import com.levinhocall.gitagent.ai.AiProviderId
import com.levinhocall.gitagent.ai.AiRequest
import com.levinhocall.gitagent.ai.AiResult
import com.levinhocall.gitagent.ai.ProviderError
import com.levinhocall.gitagent.ai.ProviderHealth

class ProviderGateway(
    private val providers: Map<AiProviderId, AiProvider>,
    private val credentialStore: CredentialStore
) {
    fun providerIds(): List<AiProviderId> = providers.keys.toList()

    fun models(providerId: AiProviderId): List<AiModel> = providers[providerId]?.models.orEmpty()

    suspend fun health(providerId: AiProviderId): ProviderHealth {
        val key = credentialStore.getApiKey(providerId)
        return if (key.isNullOrBlank()) ProviderHealth.MISSING_CREDENTIALS else ProviderHealth.READY
    }

    suspend fun complete(providerId: AiProviderId, modelId: String, messages: List<AiMessage>): AiResult {
        val provider = providers[providerId]
            ?: return AiResult(error = ProviderError.Configuration("Provider not configured: $providerId"))
        val model = provider.models.firstOrNull { it.id == modelId }
            ?: provider.models.firstOrNull()
            ?: return AiResult(error = ProviderError.Configuration("No models configured for ${provider.displayName}"))
        val key = credentialStore.getApiKey(providerId).orEmpty()
        if (key.isBlank()) {
            return AiResult(error = ProviderError.Configuration("Set an API key for ${provider.displayName} in Settings"))
        }
        return provider.complete(
            AiRequest(
                model = model,
                messages = messages,
                apiKey = key
            )
        )
    }

    suspend fun saveApiKey(providerId: AiProviderId, apiKey: String) {
        credentialStore.setApiKey(providerId, apiKey)
    }

    suspend fun clearApiKey(providerId: AiProviderId) {
        credentialStore.clearApiKey(providerId)
    }
}
