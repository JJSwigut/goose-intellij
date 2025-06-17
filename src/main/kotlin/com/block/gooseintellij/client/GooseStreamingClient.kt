package com.block.gooseintellij.client

import com.block.gooseintellij.model.GooseReplyRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.serialization.json.Json
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import java.util.concurrent.TimeUnit

/**
 * Specialized client for handling Server-Sent Events (SSE) streaming from Goose
 */
class GooseStreamingClient(
    private val baseUrl: String = "http://localhost:8000",
    private val apiKey: String,
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS) // No timeout for streaming
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
    
    private val mediaType = "application/json".toMediaType()

    /**
     * Streams messages from a Goose session using the actual /reply endpoint
     */
    fun streamMessage(prompt: String, workingDirectory: String): Flow<StreamEvent> = callbackFlow {
        val replyRequest = GooseReplyRequest(
            prompt = prompt,
            session_working_dir = workingDirectory
        )
        
        val requestBody = json.encodeToString(GooseReplyRequest.serializer(), replyRequest)
            .toRequestBody(mediaType)

        val request = Request.Builder()
            .url("$baseUrl/reply")
            .addHeader("Accept", "text/event-stream")
            .addHeader("Cache-Control", "no-cache")
            .post(requestBody)
            .build()

        val eventSource = EventSources.createFactory(httpClient).newEventSource(
            request = request,
            listener = object : EventSourceListener() {
                override fun onOpen(eventSource: EventSource, response: Response) {
                    trySend(StreamEvent.Connected)
                }

                override fun onEvent(
                    eventSource: EventSource,
                    id: String?,
                    type: String?,
                    data: String
                ) {
                    when (type) {
                        "message" -> trySend(StreamEvent.Message(data))
                        "error" -> trySend(StreamEvent.Error(data))
                        "done" -> {
                            trySend(StreamEvent.Complete)
                            eventSource.cancel()
                        }
                        else -> trySend(StreamEvent.Data(data, type))
                    }
                }

                override fun onClosed(eventSource: EventSource) {
                    trySend(StreamEvent.Closed)
                    close()
                }

                override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                    val error = t?.message ?: response?.message ?: "Unknown streaming error"
                    trySend(StreamEvent.Error(error))
                    close(t)
                }
            }
        )

        awaitClose {
            eventSource.cancel()
        }
    }
}

/**
 * Represents different types of events that can be received from the stream
 */
sealed class StreamEvent {
    object Connected : StreamEvent()
    object Closed : StreamEvent()
    object Complete : StreamEvent()
    data class Message(val content: String) : StreamEvent()
    data class Error(val message: String) : StreamEvent()
    data class Data(val content: String, val type: String?) : StreamEvent()
}