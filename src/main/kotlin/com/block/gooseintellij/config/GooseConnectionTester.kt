package com.block.gooseintellij.config

import com.block.gooseintellij.client.GooseHttpClient
import com.block.gooseintellij.client.GooseAuthException
import com.block.gooseintellij.client.GooseNetworkException
import com.block.gooseintellij.client.GooseApiException
import com.intellij.openapi.diagnostic.Logger
import kotlinx.coroutines.withTimeoutOrNull
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/**
 * Tests connection to Goose server
 */
class GooseConnectionTester {
    
    companion object {
        private val LOG = Logger.getInstance(GooseConnectionTester::class.java)
    }

    /**
     * Test connection to Goose server
     */
    suspend fun testConnection(
        serverUrl: String, 
        apiKey: String, 
        timeoutSeconds: Int = 30
    ): ConnectionTestResult {
        val startTime = System.currentTimeMillis()
        
        return try {
            LOG.info("Testing connection to Goose server: $serverUrl")
            
            // Create HTTP client with specified timeout
            val httpClient = GooseHttpClient(
                baseUrl = serverUrl,
                apiKey = apiKey
            )
            
            // Test with timeout
            val result = withTimeoutOrNull((timeoutSeconds * 1000).toLong()) {
                try {
                    // Try to list sessions as a basic connectivity test
                    val sessions = httpClient.listSessions()
                    val responseTime = System.currentTimeMillis() - startTime
                    
                    LOG.info("Connection test successful: ${sessions.size} sessions found")
                    ConnectionTestResult.Success(
                        serverVersion = "Connected", // Could be enhanced to get actual version
                        responseTime = responseTime,
                        sessionCount = sessions.size
                    )
                } catch (e: GooseAuthException) {
                    LOG.warn("Authentication failed during connection test", e)
                    ConnectionTestResult.Failure(
                        error = "Authentication failed. Please check your API key.",
                        errorType = ConnectionErrorType.AUTHENTICATION,
                        suggestion = "Verify that your API key is correct and has the necessary permissions."
                    )
                } catch (e: GooseNetworkException) {
                    LOG.warn("Network error during connection test", e)
                    handleNetworkException(e)
                } catch (e: GooseApiException) {
                    LOG.warn("API error during connection test", e)
                    ConnectionTestResult.Failure(
                        error = "Server API error: ${e.message}",
                        errorType = ConnectionErrorType.SERVER_ERROR,
                        suggestion = "The server may be experiencing issues. Please try again later."
                    )
                }
            }
            
            result ?: ConnectionTestResult.Failure(
                error = "Connection timed out after $timeoutSeconds seconds",
                errorType = ConnectionErrorType.TIMEOUT,
                suggestion = "Try increasing the connection timeout or check if the server is responding slowly."
            )
            
        } catch (e: Exception) {
            LOG.error("Unexpected error during connection test", e)
            ConnectionTestResult.Failure(
                error = "Unexpected error: ${e.message}",
                errorType = ConnectionErrorType.UNKNOWN,
                suggestion = "Please check the logs for more details and try again."
            )
        }
    }
    
    private fun handleNetworkException(e: GooseNetworkException): ConnectionTestResult.Failure {
        return when (e.cause) {
            is ConnectException -> {
                ConnectionTestResult.Failure(
                    error = "Cannot connect to server. Connection refused.",
                    errorType = ConnectionErrorType.CONNECTION_REFUSED,
                    suggestion = "Check that the server URL is correct and the Goose server is running."
                )
            }
            is UnknownHostException -> {
                ConnectionTestResult.Failure(
                    error = "Cannot resolve server hostname.",
                    errorType = ConnectionErrorType.DNS_ERROR,
                    suggestion = "Check that the server URL is correct and you have internet connectivity."
                )
            }
            is SocketTimeoutException -> {
                ConnectionTestResult.Failure(
                    error = "Connection timed out.",
                    errorType = ConnectionErrorType.TIMEOUT,
                    suggestion = "The server may be slow to respond. Try increasing the timeout or check server status."
                )
            }
            is SSLException -> {
                ConnectionTestResult.Failure(
                    error = "SSL/TLS connection error.",
                    errorType = ConnectionErrorType.SSL_ERROR,
                    suggestion = "Check that the server supports HTTPS and has a valid SSL certificate."
                )
            }
            else -> {
                ConnectionTestResult.Failure(
                    error = "Network error: ${e.message}",
                    errorType = ConnectionErrorType.NETWORK_ERROR,
                    suggestion = "Check your network connection and server URL."
                )
            }
        }
    }
}

/**
 * Result of connection test
 */
sealed class ConnectionTestResult {
    data class Success(
        val serverVersion: String,
        val responseTime: Long,
        val sessionCount: Int = 0
    ) : ConnectionTestResult()

    data class Failure(
        val error: String,
        val errorType: ConnectionErrorType,
        val suggestion: String
    ) : ConnectionTestResult()
}

/**
 * Types of connection errors
 */
enum class ConnectionErrorType {
    AUTHENTICATION,
    CONNECTION_REFUSED,
    DNS_ERROR,
    TIMEOUT,
    SSL_ERROR,
    NETWORK_ERROR,
    SERVER_ERROR,
    UNKNOWN
}