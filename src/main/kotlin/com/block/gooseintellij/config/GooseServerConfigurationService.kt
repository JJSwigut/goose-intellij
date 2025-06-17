package com.block.gooseintellij.config

import com.intellij.openapi.components.*

/**
 * Service for managing Goose server configuration
 */
@Service
@State(
    name = "GooseServerConfiguration",
    storages = [Storage("goose-server-config.xml")]
)
class GooseServerConfigurationService : PersistentStateComponent<GooseServerConfigurationService.State> {

    /**
     * Configuration state data class
     */
    data class State(
        var serverUrl: String = "http://localhost:8000",
        var connectionTimeout: Int = 30,
        var enableStreaming: Boolean = true,
        var autoStartSession: Boolean = true,
        var lastConnectionTest: Long = 0L,
        var serverVersion: String = ""
    )

    private var state = State()

    override fun getState(): State = state

    override fun loadState(state: State) {
        this.state = state
    }

    // Convenience methods for accessing configuration
    
    fun getServerUrl(): String = state.serverUrl
    
    fun setServerUrl(url: String) {
        state.serverUrl = url
    }
    
    fun getConnectionTimeout(): Int = state.connectionTimeout
    
    fun setConnectionTimeout(timeout: Int) {
        state.connectionTimeout = timeout
    }
    
    fun isStreamingEnabled(): Boolean = state.enableStreaming
    
    fun setStreamingEnabled(enabled: Boolean) {
        state.enableStreaming = enabled
    }
    
    fun isAutoStartSessionEnabled(): Boolean = state.autoStartSession
    
    fun setAutoStartSessionEnabled(enabled: Boolean) {
        state.autoStartSession = enabled
    }
    
    fun getLastConnectionTest(): Long = state.lastConnectionTest
    
    fun setLastConnectionTest(timestamp: Long) {
        state.lastConnectionTest = timestamp
    }
    
    fun getServerVersion(): String = state.serverVersion
    
    fun setServerVersion(version: String) {
        state.serverVersion = version
    }
    
    /**
     * Reset configuration to defaults
     */
    fun resetToDefaults() {
        state = State()
    }
    
    /**
     * Check if configuration is valid
     */
    fun isConfigurationValid(): Boolean {
        return state.serverUrl.isNotBlank() && 
               (state.serverUrl.startsWith("http://") || state.serverUrl.startsWith("https://")) &&
               state.connectionTimeout > 0
    }
}

/**
 * Configuration change listener interface
 */
interface GooseConfigurationListener {
    companion object {
        val TOPIC = com.intellij.util.messages.Topic.create(
            "GooseConfigurationChanged",
            GooseConfigurationListener::class.java
        )
    }
    
    fun configurationChanged()
}