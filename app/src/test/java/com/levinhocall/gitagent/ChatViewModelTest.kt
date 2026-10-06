package com.levinhocall.gitagent

import com.levinhocall.gitagent.ai.AiMessage
import com.levinhocall.gitagent.ai.AiModel
import com.levinhocall.gitagent.ai.AiProvider
import com.levinhocall.gitagent.ai.AiProviderId
import com.levinhocall.gitagent.ai.AiRequest
import com.levinhocall.gitagent.ai.AiResult
import com.levinhocall.gitagent.ai.AiRole
import com.levinhocall.gitagent.data.InMemoryConversationStore
import com.levinhocall.gitagent.data.InMemoryCredentialStore
import com.levinhocall.gitagent.data.ProviderGateway
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {
    @Test
    fun sendUpdatesStateWithAssistantReply() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        val credentialStore = InMemoryCredentialStore().apply {
            setApiKey(AiProviderId.OPENROUTER, "test-key")
        }
        val provider = object : AiProvider {
            override val id: AiProviderId = AiProviderId.OPENROUTER
            override val displayName: String = "OpenRouter"
            override val defaultEndpoint: String = "https://example.com"
            override val models: List<AiModel> = listOf(AiModel("model-a", "model-a", id))

            override suspend fun complete(request: AiRequest): AiResult {
                return AiResult(message = AiMessage(role = AiRole.ASSISTANT, content = "response"))
            }
        }
        val repository = AssistantRepository(
            providerGateway = ProviderGateway(mapOf(AiProviderId.OPENROUTER to provider), credentialStore),
            conversationStore = InMemoryConversationStore()
        )

        val viewModel = ChatViewModel(repository)
        advanceUntilIdle()

        viewModel.updateDraft("hi")
        viewModel.send()
        advanceUntilIdle()

        assertTrue(viewModel.state.value.messages.any { it.text == "response" })
        assertEquals(false, viewModel.state.value.isLoading)

        Dispatchers.resetMain()
    }

    @Test
    fun missingKeyShowsConfigurationError() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        val provider = object : AiProvider {
            override val id: AiProviderId = AiProviderId.OPENROUTER
            override val displayName: String = "OpenRouter"
            override val defaultEndpoint: String = "https://example.com"
            override val models: List<AiModel> = listOf(AiModel("model-a", "model-a", id))

            override suspend fun complete(request: AiRequest): AiResult {
                return AiResult(message = AiMessage(role = AiRole.ASSISTANT, content = "response"))
            }
        }

        val repository = AssistantRepository(
            providerGateway = ProviderGateway(mapOf(AiProviderId.OPENROUTER to provider), InMemoryCredentialStore()),
            conversationStore = InMemoryConversationStore()
        )

        val viewModel = ChatViewModel(repository)
        advanceUntilIdle()

        viewModel.updateDraft("hi")
        viewModel.send()
        advanceUntilIdle()

        assertTrue(viewModel.state.value.error?.contains("Set an API key") == true)
        Dispatchers.resetMain()
    }
}
