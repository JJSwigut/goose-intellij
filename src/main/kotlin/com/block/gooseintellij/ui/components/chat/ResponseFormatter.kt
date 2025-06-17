package com.block.gooseintellij.ui.components.chat

import com.block.gooseintellij.client.GooseAuthException
import com.block.gooseintellij.client.GooseApiException
import com.block.gooseintellij.client.GooseNetworkException
import kotlinx.coroutines.CancellationException
import java.net.ConnectException
import java.net.SocketTimeoutException

/**
 * Handles formatting of streaming responses and error messages for the UI
 */
class ResponseFormatter {

    /**
     * Formats a streaming chunk for display
     * @param chunk The raw chunk from the stream
     * @param isFirst Whether this is the first chunk in the stream
     * @return Formatted chunk ready for display
     */
    fun formatStreamingChunk(chunk: String, isFirst: Boolean): String {
        return if (isFirst) {
            // First chunk might need special handling - remove leading whitespace
            chunk.trimStart()
        } else {
            chunk
        }
    }

    /**
     * Formats a complete response for display
     * @param response The complete response text
     * @return Formatted response ready for display
     */
    fun formatCompleteResponse(response: String): String {
        return response.trim()
    }

    /**
     * Formats error messages for user-friendly display
     * @param error The exception that occurred
     * @return User-friendly error message
     */
    fun formatErrorMessage(error: Exception): String {
        return when (error) {
            is GooseNetworkException -> {
                when (error.cause) {
                    is ConnectException -> "Cannot connect to Goose server. Please check your configuration and ensure the server is running."
                    is SocketTimeoutException -> "Request timed out. The server may be busy or unresponsive."
                    else -> "Network error: ${error.message ?: "Unknown network issue"}"
                }
            }
            is GooseAuthException -> "Authentication failed. Please check your API key in settings."
            is GooseApiException -> "Server error: ${error.message ?: "Unknown server error"}"
            is CancellationException -> "Request was cancelled."
            is IllegalStateException -> error.message ?: "Configuration error"
            else -> "Error: ${error.message ?: "Unknown error occurred"}"
        }
    }

    /**
     * Formats connection status messages
     * @param isConnected Whether the connection is successful
     * @param serverUrl The server URL being connected to
     * @return Formatted status message
     */
    fun formatConnectionStatus(isConnected: Boolean, serverUrl: String): String {
        return if (isConnected) {
            "✅ Connected to Goose server at $serverUrl"
        } else {
            "❌ Cannot connect to Goose server at $serverUrl"
        }
    }

    /**
     * Formats progress messages during streaming
     * @param state The current streaming state
     * @return Formatted progress message
     */
    fun formatProgressMessage(state: String): String {
        return when (state.lowercase()) {
            "connecting" -> "Connecting to Goose..."
            "streaming" -> "Receiving response..."
            "complete" -> "Response complete"
            "cancelled" -> "Request cancelled"
            "error" -> "Error occurred"
            else -> state
        }
    }
}