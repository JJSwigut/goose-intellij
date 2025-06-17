package com.block.gooseintellij.client

import com.block.gooseintellij.model.GooseContext
import com.block.gooseintellij.model.GooseSession
import kotlinx.coroutines.runBlocking
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import java.io.IOException

class GooseHttpClientTest {

    @Mock
    private lateinit var mockHttpClient: OkHttpClient

    @Mock
    private lateinit var mockCall: Call

    private lateinit var gooseHttpClient: GooseHttpClient
    private val testApiKey = "test-api-key"
    private val testBaseUrl = "http://test-server:8000"

    @BeforeEach
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        gooseHttpClient = GooseHttpClient(
            baseUrl = testBaseUrl,
            apiKey = testApiKey,
            httpClient = mockHttpClient
        )
    }

    @Test
    fun `listSessions should return list of sessions on success`() = runBlocking {
        // Given
        val responseJson = """
            [
                {
                    "id": "session-1",
                    "name": "Test Session 1",
                    "created": "2024-01-01T00:00:00Z",
                    "status": "active"
                },
                {
                    "id": "session-2", 
                    "name": "Test Session 2",
                    "created": "2024-01-02T00:00:00Z",
                    "status": "active"
                }
            ]
        """.trimIndent()

        val mockResponse = Response.Builder()
            .request(Request.Builder().url("$testBaseUrl/sessions").build())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(responseJson.toResponseBody("application/json".toMediaType()))
            .build()

        `when`(mockHttpClient.newCall(any())).thenReturn(mockCall)
        doAnswer { invocation ->
            val callback = invocation.getArgument<Callback>(0)
            callback.onResponse(mockCall, mockResponse)
            null
        }.`when`(mockCall).enqueue(any())

        // When
        val sessions = gooseHttpClient.listSessions()

        // Then
        assert(sessions.size == 2)
        assert(sessions[0].id == "session-1")
        assert(sessions[0].name == "Test Session 1")
        assert(sessions[1].id == "session-2")
        assert(sessions[1].name == "Test Session 2")
    }

    @Test
    fun `listSessions should throw GooseNetworkException on network failure`() = runBlocking {
        // Given
        `when`(mockHttpClient.newCall(any())).thenReturn(mockCall)
        doAnswer { invocation ->
            val callback = invocation.getArgument<Callback>(0)
            callback.onFailure(mockCall, IOException("Network error"))
            null
        }.`when`(mockCall).enqueue(any())

        // When & Then
        assertThrows<GooseNetworkException> {
            runBlocking { gooseHttpClient.listSessions() }
        }
    }

    @Test
    fun `listSessions should throw GooseAuthException on 401 response`() = runBlocking {
        // Given
        val mockResponse = Response.Builder()
            .request(Request.Builder().url("$testBaseUrl/sessions").build())
            .protocol(Protocol.HTTP_1_1)
            .code(401)
            .message("Unauthorized")
            .body("".toResponseBody())
            .build()

        `when`(mockHttpClient.newCall(any())).thenReturn(mockCall)
        doAnswer { invocation ->
            val callback = invocation.getArgument<Callback>(0)
            callback.onResponse(mockCall, mockResponse)
            null
        }.`when`(mockCall).enqueue(any())

        // When & Then
        assertThrows<GooseAuthException> {
            runBlocking { gooseHttpClient.listSessions() }
        }
    }

    @Test
    fun `createSession should return session ID on success`() = runBlocking {
        // Given
        val context = GooseContext(
            projectPath = "/test/project",
            projectName = "Test Project",
            language = "kotlin"
        )

        val responseJson = """
            {
                "id": "new-session-id",
                "name": "Test Project Session",
                "created": "2024-01-01T00:00:00Z",
                "status": "active"
            }
        """.trimIndent()

        val mockResponse = Response.Builder()
            .request(Request.Builder().url("$testBaseUrl/sessions").build())
            .protocol(Protocol.HTTP_1_1)
            .code(201)
            .message("Created")
            .body(responseJson.toResponseBody("application/json".toMediaType()))
            .build()

        `when`(mockHttpClient.newCall(any())).thenReturn(mockCall)
        doAnswer { invocation ->
            val callback = invocation.getArgument<Callback>(0)
            callback.onResponse(mockCall, mockResponse)
            null
        }.`when`(mockCall).enqueue(any())

        // When
        val sessionId = gooseHttpClient.createSession(context)

        // Then
        assert(sessionId == "new-session-id")
    }

    @Test
    fun `askGoose should return response message on success`() = runBlocking {
        // Given
        val message = "Hello, Goose!"
        val workingDir = "/test/project"

        val responseText = "Hello! How can I help you?"

        val mockResponse = Response.Builder()
            .request(Request.Builder().url("$testBaseUrl/ask").build())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(responseText.toResponseBody("text/plain".toMediaType()))
            .build()

        `when`(mockHttpClient.newCall(any())).thenReturn(mockCall)
        doAnswer { invocation ->
            val callback = invocation.getArgument<Callback>(0)
            callback.onResponse(mockCall, mockResponse)
            null
        }.`when`(mockCall).enqueue(any())

        // When
        val response = gooseHttpClient.askGoose(message, workingDir)

        // Then
        assert(response == "Hello! How can I help you?")
    }

    @Test
    fun `askGoose should throw GooseApiException on error response`() = runBlocking {
        // Given
        val message = "Hello, Goose!"
        val workingDir = "/test/project"

        val mockResponse = Response.Builder()
            .request(Request.Builder().url("$testBaseUrl/ask").build())
            .protocol(Protocol.HTTP_1_1)
            .code(400)
            .message("Bad Request")
            .body("Invalid request".toResponseBody())
            .build()

        `when`(mockHttpClient.newCall(any())).thenReturn(mockCall)
        doAnswer { invocation ->
            val callback = invocation.getArgument<Callback>(0)
            callback.onResponse(mockCall, mockResponse)
            null
        }.`when`(mockCall).enqueue(any())

        // When & Then
        assertThrows<GooseApiException> {
            runBlocking { gooseHttpClient.askGoose(message, workingDir) }
        }
    }

    @Test
    fun `askGoose should throw GooseServerException on 500 response`() = runBlocking {
        // Given
        val message = "Hello, Goose!"
        val workingDir = "/test/project"

        val mockResponse = Response.Builder()
            .request(Request.Builder().url("$testBaseUrl/ask").build())
            .protocol(Protocol.HTTP_1_1)
            .code(500)
            .message("Internal Server Error")
            .body("Server error occurred".toResponseBody())
            .build()

        `when`(mockHttpClient.newCall(any())).thenReturn(mockCall)
        doAnswer { invocation ->
            val callback = invocation.getArgument<Callback>(0)
            callback.onResponse(mockCall, mockResponse)
            null
        }.`when`(mockCall).enqueue(any())

        // When & Then
        assertThrows<GooseServerException> {
            runBlocking { gooseHttpClient.askGoose(message, workingDir) }
        }
    }
}