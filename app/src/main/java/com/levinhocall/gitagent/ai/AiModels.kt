package com.levinhocall.gitagent.ai

enum class AiProviderId {
    OPENROUTER,
    GEMINI
}

data class AiModel(
    val id: String,
    val displayName: String,
    val providerId: AiProviderId
)

enum class AiRole {
    SYSTEM,
    USER,
    ASSISTANT,
    TOOL
}

data class AiMessage(
    val role: AiRole,
    val content: String,
    val timestampMillis: Long = System.currentTimeMillis()
)

data class TokenUsage(
    val promptTokens: Int? = null,
    val completionTokens: Int? = null,
    val totalTokens: Int? = null
)

sealed interface ProviderError {
    val message: String

    data class Configuration(override val message: String) : ProviderError
    data class Authentication(override val message: String) : ProviderError
    data class RateLimited(override val message: String) : ProviderError
    data class Network(override val message: String) : ProviderError
    data class Timeout(override val message: String) : ProviderError
    data class Cancelled(override val message: String) : ProviderError
    data class Service(override val message: String) : ProviderError
    data class Unknown(override val message: String) : ProviderError
}

data class AiResult(
    val message: AiMessage? = null,
    val usage: TokenUsage? = null,
    val error: ProviderError? = null
) {
    val isSuccess: Boolean get() = error == null && message != null
}

data class AiRequest(
    val model: AiModel,
    val messages: List<AiMessage>,
    val apiKey: String
)

enum class ProviderHealth {
    READY,
    MISSING_CREDENTIALS,
    DEGRADED
}

interface AiProvider {
    val id: AiProviderId
    val displayName: String
    val defaultEndpoint: String
    val models: List<AiModel>

    suspend fun complete(request: AiRequest): AiResult
}
