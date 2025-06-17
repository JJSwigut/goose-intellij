package com.block.gooseintellij.config

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.generateServiceName
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.diagnostic.Logger

/**
 * Secure storage for Goose API keys using IntelliJ's credential storage
 */
class SecureApiKeyStorage {
    
    companion object {
        private val LOG = Logger.getInstance(SecureApiKeyStorage::class.java)
        private const val SERVICE_NAME = "Goose IntelliJ Plugin"
        private const val API_KEY_ACCOUNT = "goose-api-key"
    }
    
    private val credentialAttributes = CredentialAttributes(
        generateServiceName(SERVICE_NAME, API_KEY_ACCOUNT)
    )

    /**
     * Store API key securely
     */
    fun storeApiKey(apiKey: String) {
        try {
            PasswordSafe.instance.setPassword(credentialAttributes, apiKey)
            LOG.info("API key stored securely")
        } catch (e: Exception) {
            LOG.error("Failed to store API key securely", e)
            throw SecurityException("Failed to store API key: ${e.message}", e)
        }
    }

    /**
     * Retrieve stored API key
     */
    fun getApiKey(): String? {
        return try {
            val apiKey = PasswordSafe.instance.getPassword(credentialAttributes)
            if (apiKey != null) {
                LOG.debug("API key retrieved from secure storage")
            }
            apiKey
        } catch (e: Exception) {
            LOG.error("Failed to retrieve API key from secure storage", e)
            null
        }
    }

    /**
     * Clear stored API key
     */
    fun clearApiKey() {
        try {
            PasswordSafe.instance.setPassword(credentialAttributes, null)
            LOG.info("API key cleared from secure storage")
        } catch (e: Exception) {
            LOG.error("Failed to clear API key from secure storage", e)
        }
    }

    /**
     * Check if API key is stored
     */
    fun hasApiKey(): Boolean {
        return getApiKey()?.isNotEmpty() == true
    }
    
    /**
     * Get masked API key for display purposes
     */
    fun getMaskedApiKey(): String? {
        val apiKey = getApiKey()
        return if (apiKey != null && apiKey.length > 8) {
            "${apiKey.take(4)}${"*".repeat(apiKey.length - 8)}${apiKey.takeLast(4)}"
        } else if (apiKey != null && apiKey.isNotEmpty()) {
            "*".repeat(apiKey.length)
        } else {
            null
        }
    }
}