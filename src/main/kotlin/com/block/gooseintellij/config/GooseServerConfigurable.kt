package com.block.gooseintellij.config

import com.block.gooseintellij.client.GooseHttpClient
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.ui.Messages
import com.intellij.ui.dsl.builder.*
import kotlinx.coroutines.runBlocking
import javax.swing.JComponent

/**
 * Settings configurable for Goose server configuration
 */
class GooseServerConfigurable : Configurable {

    private val configService = service<GooseServerConfigurationService>()
    private val secureStorage = SecureApiKeyStorage()
    
    // UI state variables
    private var serverUrl = ""
    private var apiKey = ""
    private var connectionTimeout = 30
    private var enableStreaming = true
    private var autoStartSession = true

    override fun getDisplayName(): String = "Goose Server"

    override fun createComponent(): JComponent {
        // Load current settings
        loadSettings()
        
        return panel {
            group("Server Configuration") {
                row("Server URL:") {
                    textField()
                        .bindText(::serverUrl)
                        .comment("URL of the Goose server (e.g., http://localhost:8000, https://your-databricks-workspace.cloud.databricks.com)")
                        .columns(50)
                }
                
                row("API Key:") {
                    passwordField()
                        .bindText(::apiKey)
                        .comment("API key for authentication (e.g., Databricks PAT, custom API key)")
                        .columns(50)
                }
                
                row("Connection Timeout (seconds):") {
                    intTextField(range = 5..300)
                        .bindIntText(::connectionTimeout)
                        .comment("Timeout for HTTP requests (5-300 seconds)")
                }
                
                row {
                    checkBox("Enable real-time streaming responses")
                        .bindSelected(::enableStreaming)
                        .comment("Enable Server-Sent Events for streaming responses")
                }
                
                row {
                    checkBox("Auto-start session on project open")
                        .bindSelected(::autoStartSession)
                        .comment("Automatically initialize Goose session when opening a project")
                }
            }
            
            group("Connection Test") {
                row {
                    button("Test Connection") {
                        testConnection()
                    }.comment("Verify connection to Goose server with current settings")
                }
            }
            
            group("Migration") {
                row {
                    button("Import from Terminal Config") {
                        importTerminalConfig()
                    }.comment("Import settings from existing terminal-based configuration")
                }
            }
            
            group("Advanced") {
                row {
                    button("Clear Stored Credentials") {
                        clearCredentials()
                    }.comment("Remove all stored API keys and reset to defaults")
                }
            }
        }
    }

    override fun isModified(): Boolean {
        val currentState = configService.getState()
        return serverUrl != currentState.serverUrl ||
                apiKey != (secureStorage.getApiKey() ?: "") ||
                connectionTimeout != currentState.connectionTimeout ||
                enableStreaming != currentState.enableStreaming ||
                autoStartSession != currentState.autoStartSession
    }

    override fun apply() {
        // Save settings to configuration service
        val state = configService.getState()
        state.serverUrl = serverUrl
        state.connectionTimeout = connectionTimeout
        state.enableStreaming = enableStreaming
        state.autoStartSession = autoStartSession
        
        // Save API key securely
        if (apiKey.isNotEmpty()) {
            secureStorage.storeApiKey(apiKey)
        }
        
        // Notify that configuration has changed
        ApplicationManager.getApplication().messageBus
            .syncPublisher(GooseConfigurationListener.TOPIC)
            .configurationChanged()
    }

    override fun reset() {
        loadSettings()
    }

    private fun loadSettings() {
        val state = configService.getState()
        serverUrl = state.serverUrl
        apiKey = secureStorage.getApiKey() ?: ""
        connectionTimeout = state.connectionTimeout
        enableStreaming = state.enableStreaming
        autoStartSession = state.autoStartSession
    }

    private fun testConnection() {
        if (serverUrl.isBlank() || apiKey.isBlank()) {
            Messages.showWarningDialog(
                "Please enter both Server URL and API Key before testing connection.",
                "Missing Configuration"
            )
            return
        }

        Messages.showInfoMessage(
            "Connection testing will be implemented in a future version.\n\nFor now, please verify your settings manually.",
            "Connection Test"
        )
    }

    private fun importTerminalConfig() {
        Messages.showInfoMessage(
            "Configuration migration will be implemented in a future version.\n\nFor now, please configure settings manually.",
            "Import Configuration"
        )
    }

    private fun clearCredentials() {
        val result = Messages.showYesNoDialog(
            "This will remove all stored API keys and reset configuration to defaults.\n\n" +
            "Are you sure you want to continue?",
            "Clear Stored Credentials",
            Messages.getWarningIcon()
        )
        
        if (result == Messages.YES) {
            secureStorage.clearApiKey()
            
            // Reset to defaults
            val defaultState = GooseServerConfigurationService.State()
            serverUrl = defaultState.serverUrl
            apiKey = ""
            connectionTimeout = defaultState.connectionTimeout
            enableStreaming = defaultState.enableStreaming
            autoStartSession = defaultState.autoStartSession
            
            Messages.showInfoMessage(
                "✅ All stored credentials have been cleared and settings reset to defaults.",
                "Credentials Cleared"
            )
        }
    }
}