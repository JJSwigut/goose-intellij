package com.block.gooseintellij.service.impl

import com.block.gooseintellij.service.ConfigurationService
import com.block.gooseintellij.service.SessionService
import com.intellij.openapi.project.Project
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.mockito.Mockito.*

/**
 * Integration tests for GooseServiceImpl with HTTP client integration
 * These tests verify the integration between GooseServiceImpl and GooseHttpClient
 * 
 * To run with real Goose server: ./gradlew test -Dgoose.integration.enabled=true
 */
class GooseServiceImplIntegrationTest {

    private lateinit var project: Project
    private lateinit var sessionService: SessionService
    private lateinit var configurationService: ConfigurationService
    private lateinit var gooseService: GooseServiceImpl

    @BeforeEach
    fun setUp() {
        project = mock(Project::class.java)
        `when`(project.basePath).thenReturn("/test/project")
        `when`(project.name).thenReturn("TestProject")
        
        sessionService = mock(SessionService::class.java)
        `when`(sessionService.createSession()).thenReturn("test-session-id")
        `when`(sessionService.hasActiveSession()).thenReturn(true)
        
        configurationService = mock(ConfigurationService::class.java)
        `when`(configurationService.getConfig("goose.server.url")).thenReturn("http://localhost:8000")
        `when`(configurationService.getConfig("goose.server.apikey")).thenReturn("test-api-key")
        
        gooseService = GooseServiceImpl(project, sessionService, configurationService)
    }

    @Test
    fun `initialize should create session and setup HTTP client`() {
        // When
        gooseService.initialize()
        
        // Then
        assertTrue(gooseService.isSessionActive())
        verify(sessionService).createSession()
        verify(configurationService).getConfig("goose.server.url")
        verify(configurationService).getConfig("goose.server.apikey")
    }

    @Test
    fun `executeCommand should throw exception when no active session`() {
        // Given
        `when`(sessionService.hasActiveSession()).thenReturn(false)
        gooseService.initialize()
        
        // When/Then
        assertThrows(IllegalStateException::class.java) {
            runBlocking {
                gooseService.executeCommand("test command")
            }
        }
    }

    @Test
    fun `streamCommand should throw exception when no active session`() {
        // Given
        `when`(sessionService.hasActiveSession()).thenReturn(false)
        gooseService.initialize()
        
        // When/Then
        assertThrows(IllegalStateException::class.java) {
            runBlocking {
                gooseService.streamCommand("test command")
            }
        }
    }

    @Test
    fun `terminateSession should end session and clear session ID`() {
        // Given
        gooseService.initialize()
        
        // When
        gooseService.terminateSession()
        
        // Then
        verify(sessionService).endSession()
    }

    @Test
    fun `updateServerConfiguration should update config and reinitialize client`() {
        // Given
        gooseService.initialize()
        val newUrl = "http://localhost:9000"
        val newApiKey = "new-api-key"
        
        // When
        gooseService.updateServerConfiguration(newUrl, newApiKey)
        
        // Then
        verify(configurationService).setConfig("goose.server.url", newUrl)
        verify(configurationService).setConfig("goose.server.apikey", newApiKey)
    }

    @Test
    fun `getServerConfiguration should return current configuration`() {
        // Given
        gooseService.initialize()
        
        // When
        val (url, apiKey) = gooseService.getServerConfiguration()
        
        // Then
        assertEquals("http://localhost:8000", url)
        assertEquals("test-api-key", apiKey)
    }

    @Test
    fun `testConnection should return false when server unavailable`() {
        // Given
        gooseService.initialize()
        
        // When
        val result = runBlocking {
            gooseService.testConnection()
        }
        
        // Then - Should return false since we're not running a real server
        assertFalse(result)
    }
}