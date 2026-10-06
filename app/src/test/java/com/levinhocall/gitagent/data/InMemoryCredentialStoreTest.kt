package com.levinhocall.gitagent.data

import com.levinhocall.gitagent.ai.AiProviderId
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InMemoryCredentialStoreTest {
    @Test
    fun savesAndClearsApiKey() = runTest {
        val store = InMemoryCredentialStore()
        store.setApiKey(AiProviderId.OPENROUTER, "  key-value  ")

        assertEquals("key-value", store.getApiKey(AiProviderId.OPENROUTER))

        store.clearApiKey(AiProviderId.OPENROUTER)
        assertNull(store.getApiKey(AiProviderId.OPENROUTER))
    }
}
