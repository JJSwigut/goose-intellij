package com.block.gooseintellij.ui.components.chat

import com.block.gooseintellij.ui.components.common.CustomFocusTraversalPolicy
import com.block.gooseintellij.utils.GooseIcons
import com.block.gooseintellij.ui.components.common.GooseRoundedActionButton
import com.block.gooseintellij.viewmodel.ChatViewModel
import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.ui.IdeBorderFactory
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import javax.swing.*
import java.awt.*
import java.awt.event.ActionEvent

class ChatInputPanel(
    icon: Icon,
    private val editor: EditorEx,
    private val sendAction: (String) -> Unit
) : JPanel(BorderLayout()) {
    
    /**
     * Represents the current state of the chat input panel
     */
    private enum class State {
        READY,      // Ready to send
        SENDING,    // Message being sent
        STREAMING,  // Receiving response
        ERROR       // Error occurred
    }

    private var currentState = State.READY
    private val viewModel: ChatViewModel = ChatViewModel(editor)
    private val bd = IdeBorderFactory.createRoundedBorder(9, 1)
    private val scrollPane: JScrollPane

    // Add progress indicator
    private val progressBar = JProgressBar().apply {
        isIndeterminate = true
        isVisible = false
        preferredSize = Dimension(preferredSize.width, 4)
        background = JBColor.background()
    }

    private val inputField: JTextArea = JTextArea().apply {
        background = JBColor.background()
        margin = JBUI.insets(10)
        lineWrap = true
        wrapStyleWord = true
        
        val inputMap = getInputMap(JComponent.WHEN_FOCUSED)
        val actionMap = actionMap
        inputMap.put(KeyStroke.getKeyStroke("ENTER"), "sendMessage")
        inputMap.put(KeyStroke.getKeyStroke("shift ENTER"), "insertNewLine")
        
        actionMap.put("sendMessage", object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent) {
                if (currentState == State.READY) {
                    handleSendAction()
                }
            }
        })
        
        actionMap.put("insertNewLine", object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent) {
                append("\n")
                viewModel.handleTextChange(this@apply)
            }
        })
        
        addKeyListener(object : java.awt.event.KeyAdapter() {
            override fun keyPressed(e: java.awt.event.KeyEvent) {
                if (currentState == State.READY) {
                    viewModel.handleTextChange(this@apply)
                    toggleSendButton()
                }
            }
        })

        document.addDocumentListener(object : javax.swing.event.DocumentListener {
            override fun insertUpdate(e: javax.swing.event.DocumentEvent) {
                if (currentState == State.READY) {
                    viewModel.handleTextChange(this@apply)
                    toggleSendButton()
                }
            }
            override fun removeUpdate(e: javax.swing.event.DocumentEvent) {
                if (currentState == State.READY) {
                    viewModel.handleTextChange(this@apply)
                    toggleSendButton()
                }
            }
            override fun changedUpdate(e: javax.swing.event.DocumentEvent) {
                if (currentState == State.READY) {
                    viewModel.handleTextChange(this@apply)
                    toggleSendButton()
                }
            }
        })

        addFocusListener(object : java.awt.event.FocusAdapter() {
            override fun focusGained(e: java.awt.event.FocusEvent) {
                bd.setColor(JBColor.BLUE)
            }
            override fun focusLost(e: java.awt.event.FocusEvent) {
                bd.setColor(JBColor.GRAY)
            }
        })
    }

    // Enhanced button management
    private val sendButton = GooseRoundedActionButton(icon, 10).apply {
        addActionListener { handleSendAction() }
        background = JBColor.background()
        cursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR)
        isEnabled = false
    }

    private val cancelButton = GooseRoundedActionButton(AllIcons.Actions.Cancel, 10).apply {
        addActionListener { handleCancelAction() }
        background = JBColor.background()
        cursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR)
        isVisible = false
    }

    private val iconLabel = JLabel(GooseIcons.GooseAction).apply {
        border = IdeBorderFactory.createEmptyBorder(JBUI.insets(10))
    }

    init {
        putClientProperty(UIUtil.HIDE_EDITOR_FROM_DATA_CONTEXT_PROPERTY, true)
        focusTraversalPolicy = CustomFocusTraversalPolicy(listOf(inputField, sendButton, cancelButton, iconLabel))
        isFocusCycleRoot = true
        setFocusable(true)
        
        bd.setColor(JBColor.GRAY)
        val padding = IdeBorderFactory.createEmptyBorder(JBUI.insets(10))
        border = BorderFactory.createCompoundBorder(padding, bd)

        // Add progress bar at top
        add(progressBar, BorderLayout.NORTH)

        add(iconLabel, BorderLayout.WEST)
        scrollPane = JBScrollPane(inputField).apply {
            border = null
            verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED
            val lineHeight = inputField.getFontMetrics(inputField.font).height
            preferredSize = null
            maximumSize = Dimension(Int.MAX_VALUE, lineHeight * ChatViewModel.MAX_LINE_COUNT)
        }
        
        add(scrollPane, BorderLayout.CENTER)
        
        // Button panel with both send and cancel buttons
        val buttonPanel = JPanel(FlowLayout(FlowLayout.RIGHT, 0, 0))
        buttonPanel.add(sendButton)
        buttonPanel.add(cancelButton)
        add(buttonPanel, BorderLayout.EAST)
        
        maximumSize = Dimension(Int.MAX_VALUE, inputField.getFontMetrics(inputField.font).height * ChatViewModel.MAX_LINE_COUNT)

        updateUIForState(State.READY)
        SwingUtilities.invokeLater { inputField.requestFocusInWindow() }
    }

    private fun handleSendAction() {
        val message = inputField.text.trim()
        if (message.isNotEmpty() && currentState == State.READY) {
            updateUIForState(State.SENDING)
            
            // Use enhanced send action with streaming callbacks
            viewModel.handleStreamingSendAction(
                text = message,
                onStreamStart = {
                    SwingUtilities.invokeLater {
                        updateUIForState(State.STREAMING)
                        inputField.text = ""
                    }
                },
                onStreamChunk = { chunk ->
                    // Handled by parent component (InlineChatPanel)
                    // This is just a placeholder for potential future use
                },
                onStreamComplete = {
                    SwingUtilities.invokeLater {
                        updateUIForState(State.READY)
                    }
                },
                onStreamError = { error ->
                    SwingUtilities.invokeLater {
                        updateUIForState(State.ERROR)
                        // Error handling delegated to parent
                    }
                }
            )
            
            // Also call the original send action for compatibility
            sendAction(message)
        }
    }

    private fun handleCancelAction() {
        // Signal cancellation to parent component
        firePropertyChange("streaming.cancelled", false, true)
        updateUIForState(State.READY)
    }

    private fun updateUIForState(newState: State) {
        currentState = newState
        when (newState) {
            State.READY -> {
                sendButton.isVisible = true
                cancelButton.isVisible = false
                progressBar.isVisible = false
                inputField.isEnabled = true
                sendButton.isEnabled = inputField.text.trim().isNotEmpty()
                toggleSendButton()
            }
            State.SENDING -> {
                sendButton.isVisible = true
                cancelButton.isVisible = false
                progressBar.isVisible = true
                inputField.isEnabled = false
                sendButton.isEnabled = false
            }
            State.STREAMING -> {
                sendButton.isVisible = false
                cancelButton.isVisible = true
                progressBar.isVisible = true
                inputField.isEnabled = false
            }
            State.ERROR -> {
                sendButton.isVisible = true
                cancelButton.isVisible = false
                progressBar.isVisible = false
                inputField.isEnabled = true
                sendButton.isEnabled = true
                
                // Show error state briefly, then return to ready
                Timer(3000) {
                    SwingUtilities.invokeLater {
                        updateUIForState(State.READY)
                    }
                }.apply {
                    isRepeats = false
                    start()
                }
            }
        }
        revalidate()
        repaint()
    }

    // Public methods for parent component to control state
    fun onStreamingStart() {
        updateUIForState(State.STREAMING)
    }

    fun onStreamingComplete() {
        updateUIForState(State.READY)
    }

    fun onStreamingError() {
        updateUIForState(State.ERROR)
    }

    fun onStreamingCancelled() {
        updateUIForState(State.READY)
    }

    private fun toggleSendButton() {
        if (currentState == State.READY) {
            sendButton.isEnabled = inputField.text.trim().isNotEmpty()
            sendButton.icon = if (sendButton.isEnabled) GooseIcons.SendToGoose else GooseIcons.SendToGooseDisabled
        }
    }

    fun adjustViewport() {
        val parent = SwingUtilities.getAncestorOfClass(Disposable::class.java, this)
        parent?.validate()
        parent?.revalidate()
        parent?.repaint()
    }
}
