package com.block.gooseintellij.service.impl

import com.block.gooseintellij.client.GooseHttpClient
import com.block.gooseintellij.client.GooseException
import com.block.gooseintellij.context.IntelliJContextBridge
import com.block.gooseintellij.service.ConfigurationService
import com.block.gooseintellij.service.GooseService
import com.block.gooseintellij.service.SessionService
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
        private const val DEFAULT_BASE_URL = "http://127.0.0.1:8000"
        private const val DEFAULT_API_KEY = "your-api-key-here"
        private const val CONFIG_KEY_BASE_URL = "goose.server.url"
        private const val CONFIG_KEY_API_KEY = "goose.server.apikey"
    }
    
    private lateinit var httpClient: GooseHttpClient
    private lateinit var contextBridge: IntelliJContextBridge
    private var currentSessionId: String? = null
    
    override fun initialize() {
        try {
            // Initialize context bridge
            contextBridge = IntelliJContextBridge(project)
            
            // Get server configuration
            val baseUrl = configurationService.getConfig(CONFIG_KEY_BASE_URL) ?: DEFAULT_BASE_URL
            val apiKey = configurationService.getConfig(CONFIG_KEY_API_KEY) ?: DEFAULT_API_KEY
            
            // Initialize HTTP client
            httpClient = GooseHttpClient(baseUrl = baseUrl, apiKey = apiKey)
            
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
        configurationService.setConfig(CONFIG_KEY_BASE_URL, baseUrl)
        configurationService.setConfig(CONFIG_KEY_API_KEY, apiKey)
        
        // Reinitialize HTTP client with new configuration
        httpClient = GooseHttpClient(baseUrl = baseUrl, apiKey = apiKey)
        LOG.info("Server configuration updated")
    }
    
    /**
     * Gets current server configuration
     */
    fun getServerConfiguration(): Pair<String, String> {
        val baseUrl = configurationService.getConfig(CONFIG_KEY_BASE_URL) ?: DEFAULT_BASE_URL
        val apiKey = configurationService.getConfig(CONFIG_KEY_API_KEY) ?: DEFAULT_API_KEY
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