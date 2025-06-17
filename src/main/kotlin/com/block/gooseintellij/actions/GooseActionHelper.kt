package com.block.gooseintellij.actions

import com.block.gooseintellij.context.IntelliJContextBridge
import com.block.gooseintellij.model.GooseContext
import com.block.gooseintellij.service.GooseService
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import kotlinx.coroutines.runBlocking

object GooseActionHelper {

    private val logger = Logger.getInstance(GooseActionHelper::class.java)

    /**
     * Main entry point for sending commands to Goose via HTTP client
     */
    fun checkAndSendToGoose(
        event: AnActionEvent, 
        commandFormat: String, 
        dataExtractor: (AnActionEvent) -> Triple<String?, String?, Boolean>
    ) {
        val project = event.project ?: return
        
        if (!checkGooseServerAvailability(project)) return

        val (selectedText, filePath, isEditor) = dataExtractor(event)
        
        // Enhanced context collection
        val contextBridge = IntelliJContextBridge(project)
        val context = contextBridge.createActionContext(event)
        
        // Build enhanced prompt with context
        val enhancedPrompt = buildEnhancedPrompt(commandFormat, selectedText, filePath, context, event)
        
        // Use HTTP client instead of terminal
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Asking Goose...") {
            override fun run(indicator: ProgressIndicator) {
                try {
                    val gooseService = project.service<GooseService>()
                    runBlocking {
                        val response = gooseService.executeCommand(enhancedPrompt)
                        ApplicationManager.getApplication().invokeLater {
                            showResponse(project, response)
                        }
                    }
                } catch (e: Exception) {
                    logger.error("Failed to communicate with Goose", e)
                    ApplicationManager.getApplication().invokeLater {
                        showError(project, e)
                    }
                }
            }
        })
    }

    /**
     * Check if Goose server is available and properly configured
     */
    private fun checkGooseServerAvailability(project: Project): Boolean {
        return try {
            val gooseService = project.service<GooseService>()
            if (!gooseService.isSessionActive()) {
                // Try to initialize if not active
                gooseService.initialize()
            }
            gooseService.isSessionActive()
        } catch (e: Exception) {
            logger.warn("Goose server not available", e)
            Messages.showErrorDialog(
                project, 
                "Cannot connect to Goose server. Please check your configuration and ensure Goose is running.", 
                "Goose Connection Error"
            )
            false
        }
    }

    /**
     * Build enhanced prompt with rich context instead of simple string formatting
     */
    private fun buildEnhancedPrompt(
        commandFormat: String, 
        selectedText: String?, 
        filePath: String?, 
        context: GooseContext,
        event: AnActionEvent
    ): String {
        return buildString {
            // Base command
            append(commandFormat)
            
            // Add selected text if available
            if (!selectedText.isNullOrEmpty()) {
                append("\n\nSelected code:\n```\n$selectedText\n```")
            }
            
            // Add file context
            if (!filePath.isNullOrEmpty()) {
                append("\n\nFile: $filePath")
                
                // Add surrounding code context for better understanding
                val surroundingCode = getSurroundingCode(event, 10)
                if (surroundingCode.isNotEmpty()) {
                    append("\n\nSurrounding code context:\n```\n$surroundingCode\n```")
                }
            }
            
            // Add enhanced project context
            append("\n\nProject context:")
            append("\n- Project: ${context.projectName}")
            append("\n- Language: ${context.language}")
            append("\n- Working directory: ${context.projectPath}")
            
            // Add additional context information
            context.additionalContext.forEach { (key, value) ->
                if (value.isNotEmpty()) {
                    append("\n- ${key.capitalize()}: $value")
                }
            }
            
            // Add current method/class context if available
            val methodContext = getCurrentMethodContext(event)
            if (methodContext.isNotEmpty()) {
                append("\n\nCurrent context: $methodContext")
            }
        }
    }

    /**
     * Get surrounding code context for better AI understanding
     */
    private fun getSurroundingCode(event: AnActionEvent, linesBefore: Int = 10, linesAfter: Int = 10): String {
        return try {
            val editor = event.getData(CommonDataKeys.EDITOR) ?: return ""
            val document = editor.document
            val caretOffset = editor.caretModel.offset
            val currentLine = document.getLineNumber(caretOffset)
            
            val startLine = maxOf(0, currentLine - linesBefore)
            val endLine = minOf(document.lineCount - 1, currentLine + linesAfter)
            
            val startOffset = document.getLineStartOffset(startLine)
            val endOffset = document.getLineEndOffset(endLine)
            
            document.getText(com.intellij.openapi.util.TextRange(startOffset, endOffset))
        } catch (e: Exception) {
            logger.debug("Could not get surrounding code context", e)
            ""
        }
    }

    /**
     * Get current method/class context
     */
    private fun getCurrentMethodContext(event: AnActionEvent): String {
        return try {
            val psiFile = event.getData(CommonDataKeys.PSI_FILE) ?: return ""
            val editor = event.getData(CommonDataKeys.EDITOR) ?: return ""
            val offset = editor.caretModel.offset
            
            // Find the current element at caret
            val element = psiFile.findElementAt(offset)
            val contexts = mutableListOf<String>()
            
            // Walk up the PSI tree to find method/class context
            var parent = element?.parent
            while (parent != null) {
                when {
                    parent.toString().contains("METHOD") -> {
                        contexts.add("Method: ${parent.text.lines().firstOrNull()?.trim() ?: "unknown"}")
                    }
                    parent.toString().contains("CLASS") -> {
                        contexts.add("Class: ${parent.text.lines().firstOrNull()?.trim() ?: "unknown"}")
                        break // Stop at class level
                    }
                }
                parent = parent.parent
            }
            
            contexts.reversed().joinToString(" -> ")
        } catch (e: Exception) {
            logger.debug("Could not determine method/class context", e)
            ""
        }
    }

    /**
     * Show response in a user-friendly way
     */
    private fun showResponse(project: Project, response: String) {
        // Create a notification with the response
        val notification = Notification(
            "Goose Notifications",
            "Goose Response",
            truncateResponse(response),
            NotificationType.INFORMATION
        )
        
        // Add action to show full response if truncated
        if (response.length > 500) {
            notification.addAction(object : com.intellij.openapi.actionSystem.AnAction("Show Full Response") {
                override fun actionPerformed(e: com.intellij.openapi.actionSystem.AnActionEvent) {
                    Messages.showMessageDialog(
                        project,
                        response,
                        "Full Goose Response",
                        Messages.getInformationIcon()
                    )
                }
            })
        }
        
        Notifications.Bus.notify(notification, project)
    }

    /**
     * Truncate response for notification display
     */
    private fun truncateResponse(response: String): String {
        return if (response.length > 500) {
            response.take(500) + "... (click 'Show Full Response' to see more)"
        } else {
            response
        }
    }

    /**
     * Show error in a user-friendly way
     */
    private fun showError(project: Project, error: Exception) {
        val errorMessage = when {
            error.message?.contains("Connection refused") == true -> 
                "Cannot connect to Goose server. Please ensure Goose is running and accessible."
            error.message?.contains("401") == true || error.message?.contains("Unauthorized") == true -> 
                "Authentication failed. Please check your Goose server configuration."
            error.message?.contains("timeout") == true -> 
                "Request timed out. The Goose server may be busy or unresponsive."
            else -> 
                "An error occurred while communicating with Goose: ${error.message}"
        }
        
        val notification = Notification(
            "Goose Notifications",
            "Goose Error",
            errorMessage,
            NotificationType.ERROR
        )
        
        // Add action to show full error details
        notification.addAction(object : com.intellij.openapi.actionSystem.AnAction("Show Details") {
            override fun actionPerformed(e: com.intellij.openapi.actionSystem.AnActionEvent) {
                Messages.showErrorDialog(
                    project,
                    "Error: ${error.javaClass.simpleName}\nMessage: ${error.message}\n\nPlease check the IDE logs for more details.",
                    "Goose Error Details"
                )
            }
        })
        
        Notifications.Bus.notify(notification, project)
    }

    // Legacy methods for backward compatibility - marked as deprecated
    
    @Deprecated("Use checkAndSendToGoose instead", ReplaceWith("checkAndSendToGoose"))
    fun checkGooseAvailability(project: Project?): Boolean {
        return project?.let { checkGooseServerAvailability(it) } ?: false
    }
    
    @Deprecated("Terminal-based approach is deprecated. Use HTTP client via GooseService instead.")
    fun getGooseTerminal(event: AnActionEvent): Any? {
        logger.warn("getGooseTerminal() is deprecated. Use HTTP client via GooseService instead.")
        return null
    }
    
    @Deprecated("Terminal-based approach is deprecated. Use HTTP client via GooseService instead.")
    fun askGooseToGenerateTests(gooseTerminal: Any?, question: String) {
        logger.warn("askGooseToGenerateTests() is deprecated. Use HTTP client via GooseService instead.")
    }
}