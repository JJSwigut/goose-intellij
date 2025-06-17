package com.block.gooseintellij.service.impl

import com.block.gooseintellij.client.GooseHttpClient
import com.block.gooseintellij.client.GooseException
import com.block.gooseintellij.config.GooseServerConfigurationService
import com.block.gooseintellij.config.SecureApiKeyStorage
import com.block.gooseintellij.context.IntelliJContextBridge
import com.block.gooseintellij.service.ConfigurationService
import com.block.gooseintellij.service.GooseService
import com.block.gooseintellij.service.SessionService
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking

class GooseServiceImpl(
    private val project: Project,
    private val sessionService: SessionService,
    private val configurationService: ConfigurationService
) : GooseService {
    
    companion object {
        private val LOG = Logger.getInstance(GooseServiceImpl::class.java)
    }
    
    private val serverConfigService by lazy { service<GooseServerConfigurationService>() }
    private val secureStorage by lazy { SecureApiKeyStorage() }
    private lateinit var httpClient: GooseHttpClient
    private lateinit var contextBridge: IntelliJContextBridge
    private var currentSessionId: String? = null
    
    override fun initialize() {
        try {
            // Initialize context bridge
            contextBridge = IntelliJContextBridge(project)
            
            // Get server configuration from new configuration service
            val serverUrl = serverConfigService.getServerUrl()
            val apiKey = secureStorage.getApiKey() ?: ""
            
            if (apiKey.isEmpty()) {
                LOG.warn("No API key configured. Please configure Goose server settings.")
                throw IllegalStateException("No API key configured. Please go to File → Settings → Tools → Goose Server to configure your connection.")
            }
            
            // Initialize HTTP client with configured settings
            httpClient = GooseHttpClient(
                baseUrl = serverUrl,
                apiKey = apiKey
            )
            
            // Create session with project context
            runBlocking {
                try {
                    val context = contextBridge.createProjectContext(project)
                    currentSessionId = httpClient.createSession(context)
                    sessionService.createSession()
                    LOG.info("Goose session initialized successfully with ID: $currentSessionId")
                } catch (e: Exception) {
                    LOG.warn("Failed to create Goose session, will use local session management", e)
                    // Fallback: just create a local session
                    sessionService.createSession()
                }
            }
        } catch (e: Exception) {
            LOG.error("Failed to initialize Goose service", e)
            throw IllegalStateException("Failed to initialize Goose service: ${e.message}", e)
        }
    }

    override suspend fun executeCommand(command: String): String {
        if (!isSessionActive()) {
            throw IllegalStateException("No active session")
        }
        
        return try {
            val workingDir = project.basePath ?: ""
            httpClient.askGoose(command, workingDir)
        } catch (e: GooseException) {
            LOG.warn("Goose API error: ${e.message}", e)
            throw e
        } catch (e: Exception) {
            LOG.error("Unexpected error executing command", e)
            throw RuntimeException("Failed to execute command: ${e.message}", e)
        }
    }

    override suspend fun streamCommand(command: String): Flow<String> {
        if (!isSessionActive()) {
            throw IllegalStateException("No active session")
        }
        
        return try {
            val workingDir = project.basePath ?: ""
            httpClient.replyToGoose(command, workingDir)
        } catch (e: GooseException) {
            LOG.warn("Goose streaming API error: ${e.message}", e)
            throw e
        } catch (e: Exception) {
            LOG.error("Unexpected error streaming command", e)
            throw RuntimeException("Failed to stream command: ${e.message}", e)
        }
    }

    override fun isSessionActive(): Boolean {
        return sessionService.hasActiveSession()
    }

    override fun terminateSession() {
        try {
            sessionService.endSession()
            currentSessionId = null
            LOG.info("Goose session terminated")
        } catch (e: Exception) {
            LOG.warn("Error terminating session", e)
        }
    }
    
    /**
     * Updates server configuration
     */
    fun updateServerConfiguration(baseUrl: String, apiKey: String) {
        serverConfigService.setServerUrl(baseUrl)
        secureStorage.storeApiKey(apiKey)
        
        // Reinitialize HTTP client with new configuration
        httpClient = GooseHttpClient(baseUrl = baseUrl, apiKey = apiKey)
        LOG.info("Server configuration updated")
    }
    
    /**
     * Gets current server configuration
     */
    fun getServerConfiguration(): Pair<String, String> {
        val baseUrl = serverConfigService.getServerUrl()
        val apiKey = secureStorage.getApiKey() ?: ""
        return Pair(baseUrl, apiKey)
    }
    
    /**
     * Tests connection to Goose server
     */
    suspend fun testConnection(): Boolean {
        return try {
            httpClient.listSessions()
            true
        } catch (e: Exception) {
            LOG.warn("Connection test failed", e)
            false
        }
    }
}