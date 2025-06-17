package com.block.gooseintellij.client

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfSystemProperty

/**
 * Integration tests for GooseHttpClient against the real Goose server
 * 
 * To run these tests:
 * ./gradlew test -Dgoose.integration.enabled=true
 */
@EnabledIfSystemProperty(named = "goose.integration.enabled", matches = "true")
class GooseHttpClientRealIntegrationTest {

    private val serverPort = "57326" // From running Goose process
    private val baseUrl = "http://127.0.0.1:$serverPort"
    
    private val client = GooseHttpClient(
        baseUrl = baseUrl,
        apiKey = "not-needed-for-local" // Local server doesn't seem to require auth
    )

    @Test
    fun `should send message to real Goose server using ask endpoint`() = runBlocking {
        try {
            val prompt = "Hello! This is a test from our Kotlin HTTP client integration test. Please respond with a brief confirmation that you received this message."
            val workingDir = "/Users/jswigut/Development/goose-intellij"
            
            println("📤 Sending prompt: $prompt")
            println("📂 Working directory: $workingDir")
            
            val response = client.askGoose(prompt, workingDir)
            
            println("✅ Successfully received response from Goose server!")
            println("📥 Response: ${response.take(200)}${if (response.length > 200) "..." else ""}")
            
            // Basic validation
            assert(response.isNotEmpty()) { "Response should not be empty" }
            
        } catch (e: Exception) {
            println("❌ Failed to communicate with Goose server: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    @Test
    fun `should handle project-specific context`() = runBlocking {
        try {
            val prompt = """
                I'm working on a Kotlin IntelliJ plugin project. Can you tell me:
                1. What build system is being used?
                2. What are the main source directories?
                3. What's the main purpose of this project based on the files you can see?
                
                Please keep your response concise.
            """.trimIndent()
            
            val workingDir = "/Users/jswigut/Development/goose-intellij"
            
            println("📤 Sending project analysis request...")
            
            val response = client.askGoose(prompt, workingDir)
            
            println("✅ Successfully received project analysis!")
            println("📥 Analysis: ${response.take(500)}${if (response.length > 500) "..." else ""}")
            
            // Validate that Goose understood the project context
            assert(response.contains("Kotlin") || response.contains("IntelliJ") || response.contains("plugin")) {
                "Response should mention project-specific details"
            }
            
        } catch (e: Exception) {
            println("❌ Failed project analysis: ${e.message}")
            throw e
        }
    }
}