package com.block.gooseintellij.client

import com.block.gooseintellij.model.GooseContext
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfSystemProperty

/**
 * Integration tests for GooseHttpClient against a real Goose server
 * These tests are disabled by default and require a running Goose server
 * 
 * To run these tests:
 * ./gradlew test -Dgoose.integration.enabled=true -Dgoose.server.port=57326
 */
@EnabledIfSystemProperty(named = "goose.integration.enabled", matches = "true")
class GooseHttpClientIntegrationTest {

    private val serverPort = System.getProperty("goose.server.port", "57326")
    private val baseUrl = "http://127.0.0.1:$serverPort"
    private val apiKey = System.getProperty("goose.api.key", "test-key")
    
    private val client = GooseHttpClient(
        baseUrl = baseUrl,
        apiKey = apiKey
    )

    @Test
    fun `should connect to real Goose server and list sessions`() = runBlocking {
        try {
            val sessions = client.listSessions()
            println("✅ Successfully connected to Goose server at $baseUrl")
            println("📋 Found ${sessions.size} sessions:")
            sessions.forEach { session ->
                println("  - ${session.id}: ${session.name} (${session.status})")
            }
        } catch (e: Exception) {
            println("❌ Failed to connect to Goose server: ${e.message}")
            println("💡 Make sure Goose is running and accessible at $baseUrl")
            throw e
        }
    }

    @Test
    fun `should create a new session with project context`() = runBlocking {
        try {
            val context = GooseContext(
                projectPath = "/Users/jswigut/Development/goose-intellij",
                projectName = "goose-intellij",
                language = "kotlin",
                framework = "intellij-plugin"
            )
            
            val sessionId = client.createSession(context)
            println("✅ Successfully created session: $sessionId")
            
            // Verify the session was created by listing sessions
            val sessions = client.listSessions()
            val createdSession = sessions.find { it.id == sessionId }
            
            if (createdSession != null) {
                println("✅ Session verified in session list: ${createdSession.name}")
            } else {
                println("⚠️  Session created but not found in session list")
            }
        } catch (e: Exception) {
            println("❌ Failed to create session: ${e.message}")
            throw e
        }
    }

    @Test
    fun `should send a message to a session`() = runBlocking {
        try {
            // First create a session
            val context = GooseContext(
                projectPath = "/Users/jswigut/Development/goose-intellij",
                projectName = "goose-intellij-test",
                language = "kotlin"
            )
            
            val sessionId = client.createSession(context)
            println("✅ Created test session: $sessionId")
            
            // Send a simple message
            val message = "Hello! This is a test message from the HTTP client integration test."
            val response = client.sendMessage(sessionId, message)
            
            println("✅ Successfully sent message and received response:")
            println("📤 Sent: $message")
            println("📥 Response: ${response.take(200)}${if (response.length > 200) "..." else ""}")
            
        } catch (e: Exception) {
            println("❌ Failed to send message: ${e.message}")
            throw e
        }
    }
}