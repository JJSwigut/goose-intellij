package com.block.gooseintellij.context

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.ArgumentMatchers.eq

/**
 * Unit tests for IntelliJContextBridge
 * 
 * Note: These are simplified tests that focus on the core logic.
 * Full integration tests would require a more complex setup with IntelliJ test framework.
 */
class IntelliJContextBridgeTest {

    private lateinit var project: Project
    private lateinit var contextBridge: IntelliJContextBridge

    @BeforeEach
    fun setUp() {
        project = mock(Project::class.java)
        `when`(project.basePath).thenReturn("/test/project")
        `when`(project.name).thenReturn("TestProject")
        
        contextBridge = IntelliJContextBridge(project)
    }

    @Test
    fun `createProjectContext should create context with basic project information`() {
        // When
        val context = contextBridge.createProjectContext(project)
        
        // Then
        assertEquals("/test/project", context.projectPath)
        assertEquals("TestProject", context.projectName)
        assertEquals("intellij-plugin", context.framework)
        assertNotNull(context.additionalContext)
        assertTrue(context.additionalContext.containsKey("projectStructure"))
        assertTrue(context.additionalContext.containsKey("openFiles"))
        assertTrue(context.additionalContext.containsKey("recentChanges"))
    }

    @Test
    fun `createActionContext should handle null editor gracefully`() {
        // Given
        val actionEvent = mock(AnActionEvent::class.java)
        `when`(actionEvent.project).thenReturn(project)
        `when`(actionEvent.getData(eq(CommonDataKeys.EDITOR))).thenReturn(null)
        `when`(actionEvent.getData(eq(CommonDataKeys.VIRTUAL_FILE))).thenReturn(null)
        
        // When
        val context = contextBridge.createActionContext(actionEvent)
        
        // Then
        assertEquals("/test/project", context.projectPath)
        assertEquals("TestProject", context.projectName)
        assertEquals("intellij-plugin", context.framework)
        assertNotNull(context.additionalContext)
        
        val additionalContext = context.additionalContext
        assertEquals("", additionalContext["selectedText"])
        assertEquals("", additionalContext["currentFile"])
        assertEquals("-1", additionalContext["caretPosition"])
    }

    @Test
    fun `createProjectContext should handle missing basePath gracefully`() {
        // Given
        val projectWithoutBasePath = mock(Project::class.java)
        `when`(projectWithoutBasePath.basePath).thenReturn(null)
        `when`(projectWithoutBasePath.name).thenReturn("TestProject")
        
        // When
        val context = contextBridge.createProjectContext(projectWithoutBasePath)
        
        // Then
        assertEquals("", context.projectPath)
        assertEquals("TestProject", context.projectName)
        assertEquals("intellij-plugin", context.framework)
        assertNotNull(context.additionalContext)
    }
}