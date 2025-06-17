package com.block.gooseintellij.context

import com.block.gooseintellij.model.GooseContext
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

/**
 * Bridge for creating IntelliJ-specific context for Goose requests
 */
class IntelliJContextBridge(private val project: Project) {

    /**
     * Creates project-wide context for session initialization
     */
    fun createProjectContext(project: Project): GooseContext {
        return GooseContext(
            projectPath = project.basePath ?: "",
            projectName = project.name,
            language = detectProjectLanguage(),
            framework = "intellij-plugin",
            additionalContext = mapOf(
                "projectStructure" to analyzeProjectStructure(),
                "openFiles" to getOpenFiles().joinToString(","),
                "recentChanges" to getRecentGitChanges().joinToString(";")
            )
        )
    }

    /**
     * Creates action-specific context for individual commands
     */
    fun createActionContext(event: AnActionEvent): GooseContext {
        val project = event.project ?: throw IllegalStateException("No project available")
        
        return GooseContext(
            projectPath = project.basePath ?: "",
            projectName = project.name,
            language = detectProjectLanguage(),
            framework = "intellij-plugin",
            additionalContext = mapOf(
                "selectedText" to (getSelectedText(event) ?: ""),
                "currentFile" to (getCurrentFile(event)?.path ?: ""),
                "caretPosition" to getCaretPosition(event).toString(),
                "openFiles" to getOpenFiles().joinToString(",")
            )
        )
    }

    /**
     * Detects the primary programming language of the project
     */
    private fun detectProjectLanguage(): String {
        // Simple heuristic: check for common files
        val basePath = project.basePath ?: return "unknown"
        val projectDir = java.io.File(basePath)
        
        return when {
            projectDir.resolve("build.gradle.kts").exists() || 
            projectDir.resolve("build.gradle").exists() -> "kotlin"
            projectDir.resolve("pom.xml").exists() -> "java"
            projectDir.resolve("package.json").exists() -> "javascript"
            projectDir.resolve("Cargo.toml").exists() -> "rust"
            projectDir.resolve("go.mod").exists() -> "go"
            else -> "unknown"
        }
    }

    /**
     * Analyzes basic project structure
     */
    private fun analyzeProjectStructure(): String {
        val basePath = project.basePath ?: return "unknown"
        val projectDir = java.io.File(basePath)
        
        val structure = mutableListOf<String>()
        
        // Check for common directories
        listOf("src", "test", "main", "resources", "build", "target", "node_modules").forEach { dir ->
            if (projectDir.resolve(dir).exists()) {
                structure.add(dir)
            }
        }
        
        return structure.joinToString(",")
    }

    /**
     * Gets list of currently open files
     */
    private fun getOpenFiles(): List<String> {
        return try {
            val fileEditorManager = FileEditorManager.getInstance(project)
            fileEditorManager.openFiles.map { it.name }
        } catch (e: Exception) {
            // In unit tests or if FileEditorManager is not available
            emptyList()
        }
    }

    /**
     * Gets recent Git changes (simplified)
     */
    private fun getRecentGitChanges(): List<String> {
        return try {
            // Simplified implementation without Git4Idea dependency
            // In a real implementation, you'd integrate with Git VCS
            val basePath = project.basePath
            if (basePath != null && java.io.File(basePath, ".git").exists()) {
                listOf("Git repository detected")
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            // Git not available or no repository
            emptyList()
        }
    }

    /**
     * Gets selected text from the current editor
     */
    private fun getSelectedText(event: AnActionEvent): String? {
        val editor = event.getData(CommonDataKeys.EDITOR)
        return editor?.selectionModel?.selectedText
    }

    /**
     * Gets the current file from the action event
     */
    private fun getCurrentFile(event: AnActionEvent): VirtualFile? {
        return event.getData(CommonDataKeys.VIRTUAL_FILE)
    }

    /**
     * Gets the current caret position
     */
    private fun getCaretPosition(event: AnActionEvent): Int {
        val editor = event.getData(CommonDataKeys.EDITOR)
        return editor?.caretModel?.offset ?: -1
    }
}