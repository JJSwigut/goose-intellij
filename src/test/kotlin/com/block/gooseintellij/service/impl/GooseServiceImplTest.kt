package com.block.gooseintellij.service.impl

import com.block.gooseintellij.service.ConfigurationService
import com.block.gooseintellij.service.SessionService
import com.intellij.openapi.project.Project
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.mockito.Mockito.*

class GooseServiceImplTest {
    private lateinit var gooseService: GooseServiceImpl
    private lateinit var mockProject: Project
    private lateinit var mockSessionService: SessionService
    private lateinit var mockConfigurationService: ConfigurationService
    
    @BeforeEach
    fun setup() {
        mockProject = mock(Project::class.java)
        `when`(mockProject.basePath).thenReturn("/test/project")
        `when`(mockProject.name).thenReturn("TestProject")
        
        mockSessionService = mock(SessionService::class.java)
        `when`(mockSessionService.createSession()).thenReturn("test-session-id")
        `when`(mockSessionService.hasActiveSession()).thenReturn(true)
        
        mockConfigurationService = mock(ConfigurationService::class.java)
        `when`(mockConfigurationService.getConfig("goose.server.url")).thenReturn("http://localhost:8000")
        `when`(mockConfigurationService.getConfig("goose.server.apikey")).thenReturn("test-api-key")
        
        gooseService = GooseServiceImpl(mockProject, mockSessionService, mockConfigurationService)
    }
    
    @Test
    fun `initialize creates new session`() {
        gooseService.initialize()
        verify(mockSessionService).createSession()
    }
    
    @Test
    fun `isSessionActive returns session service state`() {
        `when`(mockSessionService.hasActiveSession()).thenReturn(true)
        assertTrue(gooseService.isSessionActive())
        
        `when`(mockSessionService.hasActiveSession()).thenReturn(false)
        assertFalse(gooseService.isSessionActive())
    }
    
    @Test
    fun `terminateSession ends session`() {
        gooseService.terminateSession()
        verify(mockSessionService).endSession()
    }
    
    @Test
    fun `executeCommand throws exception when no active session`() {
        `when`(mockSessionService.hasActiveSession()).thenReturn(false)
        assertThrows(IllegalStateException::class.java) {
            runBlocking { gooseService.executeCommand("test") }
        }
    }
}
