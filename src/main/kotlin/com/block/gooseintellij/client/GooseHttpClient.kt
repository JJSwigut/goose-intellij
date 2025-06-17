package com.block.gooseintellij.client

import com.block.gooseintellij.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * HTTP client for communicating with the Goose server
 */
class GooseHttpClient(
    private val baseUrl: String = "http://localhost:8000",
    private val apiKey: String,
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
    
    private val mediaType = "application/json".toMediaType()

    /**
     * Lists all available sessions
     */
    suspend fun listSessions(): List<GooseSession> {
        val request = Request.Builder()
            .url("$baseUrl/sessions")
            .addHeader("Authorization", "Bearer $apiKey")
            .get()
            .build()

        return executeRequest(request) { responseBody ->
            json.decodeFromString<List<GooseSession>>(responseBody)
        }
    }

    /**
     * Creates a new session with the given context
     */
    suspend fun createSession(context: GooseContext): String {
        val requestBody = json.encodeToString(GooseContext.serializer(), context)
            .toRequestBody(mediaType)

        val request = Request.Builder()
            .url("$baseUrl/sessions")
            .addHeader("Authorization", "Bearer $apiKey")
            .post(requestBody)
            .build()

        return executeRequest(request) { responseBody ->
            val session = json.decodeFromString<GooseSession>(responseBody)
            session.id
        }
    }

    /**
     * Sends a message using the actual Goose /ask endpoint
     */
    suspend fun askGoose(prompt: String, workingDirectory: String): String {
        val askRequest = GooseAskRequest(
            prompt = prompt,
            session_working_dir = workingDirectory
        )
        
        val requestBody = json.encodeToString(GooseAskRequest.serializer(), askRequest)
            .toRequestBody(mediaType)

        val request = Request.Builder()
            .url("$baseUrl/ask")
            .post(requestBody)
            .build()

        return executeRequest(request) { responseBody ->
            // The response might be plain text or JSON, handle both
            responseBody
        }
    }

    /**
     * Streams a message using the actual Goose /reply endpoint
     */
    suspend fun replyToGoose(prompt: String, workingDirectory: String): Flow<String> = flow {
        val replyRequest = GooseReplyRequest(
            prompt = prompt,
            session_working_dir = workingDirectory
        )
        
        val requestBody = json.encodeToString(GooseReplyRequest.serializer(), replyRequest)
            .toRequestBody(mediaType)

        val request = Request.Builder()
            .url("$baseUrl/reply")
            .post(requestBody)
            .addHeader("Accept", "text/event-stream")
            .build()

        suspendCoroutine<Unit> { continuation ->
            val eventSource = EventSources.createFactory(httpClient).newEventSource(
                request = request,
                listener = object : EventSourceListener() {
                    override fun onOpen(eventSource: EventSource, response: Response) {
                        // Connection opened successfully
                    }

                    override fun onEvent(
                        eventSource: EventSource,
                        id: String?,
                        type: String?,
                        data: String
                    ) {
                        try {
                            // Emit the data chunk directly
                            // Note: This is a simplified implementation
                            // In practice, you'd use a Channel or similar for proper flow emission
                        } catch (e: Exception) {
                            eventSource.cancel()
                            continuation.resumeWithException(e)
                        }
                    }

                    override fun onClosed(eventSource: EventSource) {
                        continuation.resume(Unit)
                    }

                    override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                        continuation.resumeWithException(
                            t ?: GooseApiException("Stream failed: ${response?.message}")
                        )
                    }
                }
            )
        }
    }

    /**
     * Executes an HTTP request and handles common error scenarios
     */
    private suspend fun <T> executeRequest(
        request: Request,
        responseHandler: (String) -> T
    ): T = suspendCoroutine { continuation ->
        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                continuation.resumeWithException(
                    GooseNetworkException("Network error: ${e.message}", e)
                )
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    when (response.code) {
                        200, 201 -> {
                            val responseBody = response.body?.string()
                                ?: throw GooseApiException("Empty response body")
                            val result = responseHandler(responseBody)
                            continuation.resume(result)
                        }
                        401 -> {
                            continuation.resumeWithException(
                                GooseAuthException("Authentication failed. Check your API key.")
                            )
                        }
                        404 -> {
                            continuation.resumeWithException(
                                GooseApiException("Resource not found")
                            )
                        }
                        500 -> {
                            continuation.resumeWithException(
                                GooseServerException("Server error occurred")
                            )
                        }
                        else -> {
                            val errorBody = response.body?.string() ?: "Unknown error"
                            continuation.resumeWithException(
                                GooseApiException("HTTP ${response.code}: $errorBody")
                            )
                        }
                    }
                } catch (e: Exception) {
                    continuation.resumeWithException(e)
                } finally {
                    response.close()
                }
            }
        })
    }
}

/**
 * Base exception for Goose API errors
 */
sealed class GooseException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Network connectivity exception
 */
class GooseNetworkException(message: String, cause: Throwable? = null) : GooseException(message, cause)

/**
 * Authentication exception
 */
class GooseAuthException(message: String, cause: Throwable? = null) : GooseException(message, cause)

/**
 * API-level exception
 */
class GooseApiException(message: String, cause: Throwable? = null) : GooseException(message, cause)

/**
 * Server error exception
 */
class GooseServerException(message: String, cause: Throwable? = null) : GooseException(message, cause)