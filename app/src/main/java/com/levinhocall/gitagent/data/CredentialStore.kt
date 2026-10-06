package com.levinhocall.gitagent.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.levinhocall.gitagent.ai.AiProviderId

interface CredentialStore {
    suspend fun getApiKey(providerId: AiProviderId): String?
    suspend fun setApiKey(providerId: AiProviderId, apiKey: String)
    suspend fun clearApiKey(providerId: AiProviderId)
}

class AndroidKeystoreCredentialStore(context: Context) : CredentialStore {
    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        FILE_NAME,
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    override suspend fun getApiKey(providerId: AiProviderId): String? {
        return sharedPreferences.getString(providerId.prefKey(), null)
    }

    override suspend fun setApiKey(providerId: AiProviderId, apiKey: String) {
        sharedPreferences.edit().putString(providerId.prefKey(), apiKey.trim()).apply()
    }

    override suspend fun clearApiKey(providerId: AiProviderId) {
        sharedPreferences.edit().remove(providerId.prefKey()).apply()
    }

    private fun AiProviderId.prefKey(): String = "api_key_${name.lowercase()}"

    private companion object {
        const val FILE_NAME = "aurix_credentials"
    }
}

class InMemoryCredentialStore : CredentialStore {
    private val values = mutableMapOf<AiProviderId, String>()

    override suspend fun getApiKey(providerId: AiProviderId): String? = values[providerId]

    override suspend fun setApiKey(providerId: AiProviderId, apiKey: String) {
        values[providerId] = apiKey.trim()
    }

    override suspend fun clearApiKey(providerId: AiProviderId) {
        values.remove(providerId)
    }
}
