package com.block.gooseintellij.ui.components.chat

import com.block.gooseintellij.client.GooseAuthException
import com.block.gooseintellij.client.GooseNetworkException
import com.block.gooseintellij.config.GooseServerConfigurationService
import com.block.gooseintellij.config.SecureApiKeyStorage
import com.block.gooseintellij.service.GooseService
import com.block.gooseintellij.ui.components.common.RoundedPanel
import com.intellij.icons.AllIcons
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.Disposable
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.util.Ref
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBScrollPane
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.*

class InlineChatPanel(
    private val editor: EditorEx,
    event: AnActionEvent,
    private val inlayRef: Ref<Disposable>,
    onSend: (String) -> Unit
) : RoundedPanel(BorderLayout()), Disposable {
    
    /**
     * Represents the current streaming state
     */
    enum class StreamingState {
        CONNECTING, STREAMING, COMPLETE, CANCELLED, ERROR
    }

    private val project = editor.project!!
    private val configService = service<GooseServerConfigurationService>()
    private val secureStorage = SecureApiKeyStorage()
    private val responseFormatter = ResponseFormatter()
    private val cancellationHandler = StreamingCancellationHandler()

    // Add response area for streaming output
    private val responseArea = JTextArea().apply {
        isEditable = false
        lineWrap = true
        wrapStyleWord = true
        background = JBColor.background()
        border = BorderFactory.createEmptyBorder(8, 8, 8, 8)
    }

    private val scrollPane = JBScrollPane(responseArea).apply {
        preferredSize = Dimension(500, 120)
        border = null
        isVisible = false // Initially hidden
    }

    private var currentStreamingJob: Job? = null
    private var currentStreamingState = StreamingState.COMPLETE

    // Enhanced close/cancel button management
    private val closeAction = object : AnAction({ "Close" }, AllIcons.Actions.Close) {
        override fun actionPerformed(e: AnActionEvent) {
            dispose()
        }
    }

    private val cancelAction = object : AnAction({ "Cancel" }, AllIcons.Actions.Cancel) {
        override fun actionPerformed(e: AnActionEvent) {
            currentStreamingJob?.cancel()
            setStreamingState(StreamingState.CANCELLED)
        }
    }

    private val closeButton = ActionButton(
        closeAction,
        closeAction.templatePresentation.clone(),
        ActionPlaces.TOOLBAR,
        ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE
    )

    private val cancelButton = ActionButton(
        cancelAction,
        cancelAction.templatePresentation.clone(),
        ActionPlaces.TOOLBAR,
        ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE
    ).apply { isVisible = false }

    init {
        layout = BorderLayout(5, 5)
        
        // Response area at top (hidden initially)
        add(scrollPane, BorderLayout.NORTH)
        
        // Enhanced chat input with streaming support
        val chatInputPanel = ChatInputPanel(
            com.block.gooseintellij.utils.GooseIcons.SendToGooseDisabled, 
            editor
        ) { userInput ->
            handleStreamingMessage(userInput)
        }
        
        // Listen for cancellation events from the input panel
        chatInputPanel.addPropertyChangeListener("streaming.cancelled") { _ ->
            currentStreamingJob?.cancel()
            setStreamingState(StreamingState.CANCELLED)
        }

        add(chatInputPanel, BorderLayout.CENTER)

        // Button panel with both close and cancel buttons
        val buttonPanel = createButtonPanel()
        add(buttonPanel, BorderLayout.EAST)

        // Handle hierarchy changes for proper layout
        addHierarchyListener { e ->
            if ((e.changeFlags and java.awt.event.HierarchyEvent.DISPLAYABILITY_CHANGED.toLong()) != 0L) {
                SwingUtilities.invokeLater {
                    revalidate()
                    repaint()
                    editor.contentComponent.validate()
                }
            }
        }

        addAncestorListener(object : javax.swing.event.AncestorListener {
            override fun ancestorAdded(e: javax.swing.event.AncestorEvent) {
                SwingUtilities.invokeLater {
                    revalidate()
                    repaint()
                    val parent = SwingUtilities.getAncestorOfClass(Disposable::class.java, this@InlineChatPanel)
                    parent?.validate()
                    parent?.revalidate()
                    parent?.repaint()
                }
            }
            override fun ancestorRemoved(e: javax.swing.event.AncestorEvent) {}
            override fun ancestorMoved(e: javax.swing.event.AncestorEvent) {}
        })
    }

    private fun createButtonPanel(): JComponent {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        panel.add(closeButton)
        panel.add(cancelButton)
        return panel
    }

    private fun handleStreamingMessage(userInput: String) {
        // Validate configuration first
        if (!validateConfiguration()) return

        // Cancel any ongoing stream
        currentStreamingJob?.cancel()

        // Show response area and set loading state
        scrollPane.isVisible = true
        setStreamingState(StreamingState.CONNECTING)
        responseArea.text = responseFormatter.formatProgressMessage("connecting")

        val gooseService = project.service<GooseService>()

        currentStreamingJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                setStreamingState(StreamingState.STREAMING)
                SwingUtilities.invokeLater {
                    responseArea.text = ""
                }

                var isFirstChunk = true
                gooseService.streamCommand(userInput).collect { chunk ->
                    SwingUtilities.invokeLater {
                        appendToResponse(chunk, isFirstChunk)
                        isFirstChunk = false
                    }
                }

                SwingUtilities.invokeLater {
                    setStreamingState(StreamingState.COMPLETE)
                }

            } catch (e: CancellationException) {
                SwingUtilities.invokeLater {
                    setStreamingState(StreamingState.CANCELLED)
                }
            } catch (e: GooseNetworkException) {
                SwingUtilities.invokeLater {
                    handleStreamingError(e)
                }
            } catch (e: GooseAuthException) {
                SwingUtilities.invokeLater {
                    handleAuthError(e)
                }
            } catch (e: Exception) {
                SwingUtilities.invokeLater {
                    handleStreamingError(e)
                }
            }
        }

        // Track the job for cancellation
        cancellationHandler.startStreaming(currentStreamingJob!!)
    }

    private fun validateConfiguration(): Boolean {
        if (!configService.isConfigurationValid()) {
            showConfigurationError("Server configuration is incomplete. Please check your settings.")
            return false
        }

        if (!secureStorage.hasApiKey()) {
            showConfigurationError("API key is not configured. Please set your API key in settings.")
            return false
        }

        return true
    }

    private fun showConfigurationError(message: String) {
        Notifications.Bus.notify(
            Notification(
                "Goose Notifications",
                "Configuration Error",
                "$message\n\nClick to open settings.",
                NotificationType.ERROR
            ).addAction(object : AnAction("Open Settings") {
                override fun actionPerformed(e: AnActionEvent) {
                    ShowSettingsUtil.getInstance().showSettingsDialog(project, "Goose Server")
                }
            }),
            project
        )
    }

    private fun appendToResponse(chunk: String, isFirst: Boolean) {
        val formattedChunk = responseFormatter.formatStreamingChunk(chunk, isFirst)
        if (responseArea.text.isEmpty() || responseArea.text.startsWith("Connecting")) {
            responseArea.text = formattedChunk
        } else {
            responseArea.append(formattedChunk)
        }

        // Auto-scroll to bottom
        responseArea.caretPosition = responseArea.document.length

        // Adjust panel size based on content
        adjustPanelSize()
    }

    private fun adjustPanelSize() {
        val lines = responseArea.lineCount
        val lineHeight = responseArea.getFontMetrics(responseArea.font).height
        val newHeight = minOf(maxOf(lines * lineHeight + 16, 80), 300) // Min 80, max 300
        scrollPane.preferredSize = Dimension(scrollPane.preferredSize.width, newHeight)

        SwingUtilities.invokeLater {
            revalidate()
            repaint()
            editor.contentComponent.revalidate()
            editor.contentComponent.repaint()
        }
    }

    private fun setStreamingState(state: StreamingState) {
        currentStreamingState = state
        when (state) {
            StreamingState.CONNECTING -> {
                closeButton.isVisible = false
                cancelButton.isVisible = true
            }
            StreamingState.STREAMING -> {
                closeButton.isVisible = false
                cancelButton.isVisible = true
            }
            StreamingState.COMPLETE -> {
                closeButton.isVisible = true
                cancelButton.isVisible = false
                scheduleAutoClose()
            }
            StreamingState.CANCELLED -> {
                responseArea.text = responseFormatter.formatProgressMessage("cancelled")
                closeButton.isVisible = true
                cancelButton.isVisible = false
            }
            StreamingState.ERROR -> {
                closeButton.isVisible = true
                cancelButton.isVisible = false
            }
        }
        revalidate()
        repaint()
    }

    private fun handleStreamingError(error: Exception) {
        setStreamingState(StreamingState.ERROR)
        val errorMessage = responseFormatter.formatErrorMessage(error)
        responseArea.text = errorMessage

        // Show detailed error notification
        Notifications.Bus.notify(
            Notification(
                "Goose Notifications",
                "Streaming Error",
                errorMessage,
                NotificationType.ERROR
            ),
            project
        )
    }

    private fun handleAuthError(error: GooseAuthException) {
        setStreamingState(StreamingState.ERROR)
        responseArea.text = "Authentication failed. Please check your API key."

        Notifications.Bus.notify(
            Notification(
                "Goose Notifications",
                "Authentication Error",
                "Your API key appears to be invalid. Please update your configuration.",
                NotificationType.ERROR
            ).addAction(object : AnAction("Update API Key") {
                override fun actionPerformed(e: AnActionEvent) {
                    ShowSettingsUtil.getInstance().showSettingsDialog(project, "Goose Server")
                }
            }),
            project
        )
    }

    private fun scheduleAutoClose() {
        // Optional: Auto-close after 30 seconds of inactivity
        Timer(30000) {
            if (currentStreamingJob?.isActive != true) {
                SwingUtilities.invokeLater {
                    dispose()
                }
            }
        }.apply {
            isRepeats = false
            start()
        }
    }

    override fun dispose() {
        currentStreamingJob?.cancel()
        cancellationHandler.cleanup()
        inlayRef.get()?.dispose()
    }
}
