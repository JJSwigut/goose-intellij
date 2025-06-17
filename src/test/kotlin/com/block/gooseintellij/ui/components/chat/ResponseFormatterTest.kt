package com.block.gooseintellij.ui.components.chat

import com.block.gooseintellij.client.GooseAuthException
import com.block.gooseintellij.client.GooseApiException
import com.block.gooseintellij.client.GooseNetworkException
import kotlinx.coroutines.CancellationException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.net.ConnectException
import java.net.SocketTimeoutException

/**
 * Unit tests for ResponseFormatter
 */
class ResponseFormatterTest {

    private lateinit var formatter: ResponseFormatter

    @BeforeEach
    fun setUp() {
        formatter = ResponseFormatter()
    }

    @Test
    fun `test formatStreamingChunk with first chunk`() {
        val chunk = "  Hello world"
        val result = formatter.formatStreamingChunk(chunk, isFirst = true)
        assertEquals("Hello world", result)
    }

    @Test
    fun `test formatStreamingChunk with subsequent chunk`() {
        val chunk = "  Hello world"
        val result = formatter.formatStreamingChunk(chunk, isFirst = false)
        assertEquals("  Hello world", result)
    }

    @Test
    fun `test formatCompleteResponse`() {
        val response = "  Complete response  "
        val result = formatter.formatCompleteResponse(response)
        assertEquals("Complete response", result)
    }

    @Test
    fun `test formatErrorMessage with GooseNetworkException ConnectException`() {
        val error = GooseNetworkException("Network error", ConnectException("Connection refused"))
        val result = formatter.formatErrorMessage(error)
        assertTrue(result.contains("Cannot connect to Goose server"))
    }

    @Test
    fun `test formatErrorMessage with GooseNetworkException SocketTimeoutException`() {
        val error = GooseNetworkException("Network error", SocketTimeoutException("Timeout"))
        val result = formatter.formatErrorMessage(error)
        assertTrue(result.contains("Request timed out"))
    }

    @Test
    fun `test formatErrorMessage with GooseAuthException`() {
        val error = GooseAuthException("Auth failed")
        val result = formatter.formatErrorMessage(error)
        assertTrue(result.contains("Authentication failed"))
    }

    @Test
    fun `test formatErrorMessage with GooseApiException`() {
        val error = GooseApiException("API error")
        val result = formatter.formatErrorMessage(error)
        assertTrue(result.contains("Server error"))
    }

    @Test
    fun `test formatErrorMessage with CancellationException`() {
        val error = CancellationException("Cancelled")
        val result = formatter.formatErrorMessage(error)
        assertEquals("Request was cancelled.", result)
    }

    @Test
    fun `test formatErrorMessage with IllegalStateException`() {
        val error = IllegalStateException("Configuration error")
        val result = formatter.formatErrorMessage(error)
        assertEquals("Configuration error", result)
    }

    @Test
    fun `test formatErrorMessage with generic Exception`() {
        val error = RuntimeException("Generic error")
        val result = formatter.formatErrorMessage(error)
        assertTrue(result.contains("Error: Generic error"))
    }

    @Test
    fun `test formatConnectionStatus connected`() {
        val result = formatter.formatConnectionStatus(true, "http://localhost:8000")
        assertEquals("✅ Connected to Goose server at http://localhost:8000", result)
    }

    @Test
    fun `test formatConnectionStatus not connected`() {
        val result = formatter.formatConnectionStatus(false, "http://localhost:8000")
        assertEquals("❌ Cannot connect to Goose server at http://localhost:8000", result)
    }

    @Test
    fun `test formatProgressMessage with known states`() {
        assertEquals("Connecting to Goose...", formatter.formatProgressMessage("connecting"))
        assertEquals("Receiving response...", formatter.formatProgressMessage("streaming"))
        assertEquals("Response complete", formatter.formatProgressMessage("complete"))
        assertEquals("Request cancelled", formatter.formatProgressMessage("cancelled"))
        assertEquals("Error occurred", formatter.formatProgressMessage("error"))
    }

    @Test
    fun `test formatProgressMessage with unknown state`() {
        val result = formatter.formatProgressMessage("unknown_state")
        assertEquals("unknown_state", result)
    }

    @Test
    fun `test formatProgressMessage case insensitive`() {
        assertEquals("Connecting to Goose...", formatter.formatProgressMessage("CONNECTING"))
        assertEquals("Receiving response...", formatter.formatProgressMessage("Streaming"))
    }
}