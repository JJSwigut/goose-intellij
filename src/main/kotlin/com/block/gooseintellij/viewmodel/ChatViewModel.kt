package com.block.gooseintellij.viewmodel

import com.block.gooseintellij.config.GooseServerConfigurationService
import com.block.gooseintellij.config.SecureApiKeyStorage
import com.block.gooseintellij.service.GooseService
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.ex.EditorEx
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import javax.swing.JTextArea
import javax.swing.SwingUtilities

class ChatViewModel(private val editor: EditorEx) {
    
    companion object {
        const val MAX_LINE_COUNT = 25
    }

    fun handleTextChange(inputField: JTextArea) {
        updateEditorLayout()
    }
    
    fun handleSendAction(text: String, onSend: (String) -> Unit) {
        val trimmedText = text.trim()
        if (trimmedText.isNotEmpty()) {
            onSend(trimmedText.replace("\n", " "))
        }
    }

    /**
     * Handles streaming send action with callbacks for different streaming states
     * @param text The message text to send
     * @param onStreamStart Callback when streaming starts
     * @param onStreamChunk Callback for each chunk received
     * @param onStreamComplete Callback when streaming completes successfully
     * @param onStreamError Callback when an error occurs
     */
    fun handleStreamingSendAction(
        text: String,
        onStreamStart: () -> Unit,
        onStreamChunk: (String) -> Unit,
        onStreamComplete: () -> Unit,
        onStreamError: (Exception) -> Unit
    ) {
        val trimmedText = text.trim()
        if (trimmedText.isEmpty()) return

        val project = editor.project ?: return

        // Validate configuration before starting
        val configService = project.service<GooseServerConfigurationService>()
        val secureStorage = SecureApiKeyStorage()

        if (!configService.isConfigurationValid() || !secureStorage.hasApiKey()) {
            onStreamError(IllegalStateException("Goose server not configured"))
            return
        }

        val gooseService = project.service<GooseService>()
        
        // Notify that streaming is starting
        onStreamStart()

        // Start streaming in background
        CoroutineScope(Dispatchers.IO).launch {
            try {
                gooseService.streamCommand(trimmedText).collect { chunk ->
                    SwingUtilities.invokeLater {
                        onStreamChunk(chunk)
                    }
                }
                
                SwingUtilities.invokeLater {
                    onStreamComplete()
                }
            } catch (e: CancellationException) {
                // Handle cancellation gracefully - don't treat as error
                SwingUtilities.invokeLater {
                    onStreamComplete()
                }
            } catch (e: Exception) {
                SwingUtilities.invokeLater {
                    onStreamError(e)
                }
            }
        }
    }
    
    private fun updateEditorLayout() {
        editor.contentComponent.revalidate()
        editor.contentComponent.repaint()
    }
}