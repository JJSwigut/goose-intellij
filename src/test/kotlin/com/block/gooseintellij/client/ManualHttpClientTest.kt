package com.block.gooseintellij.client

import com.block.gooseintellij.model.GooseContext
import kotlinx.coroutines.runBlocking

/**
 * Manual test runner for verifying HTTP client functionality
 * Run this as a main function to test against a live Goose server
 */
fun main() = runBlocking {
    println("🧪 Testing Goose HTTP Client Integration")
    println("=" * 50)
    
    val serverPort = "57326" // From the running Goose process
    val baseUrl = "http://127.0.0.1:$serverPort"
    val apiKey = "test-key" // This might need to be the actual secret key
    
    val client = GooseHttpClient(
        baseUrl = baseUrl,
        apiKey = apiKey
    )
    
    try {
        // Test 1: List existing sessions
        println("\n📋 Test 1: Listing existing sessions...")
        val sessions = client.listSessions()
        println("✅ Found ${sessions.size} sessions:")
        sessions.forEach { session ->
            println("  - ${session.id}: ${session.name} (${session.status})")
        }
        
        // Test 2: Test direct communication (no session needed for /ask endpoint)
        println("\n💬 Test 2: Sending a test message...")
        val message = "Hello from the HTTP client! Can you confirm you received this message and tell me what you know about this project?"
        val workingDir = "/Users/jswigut/Development/goose-intellij"
        val response = client.askGoose(message, workingDir)
        println("✅ Received response:")
        println("📥 ${response.take(300)}${if (response.length > 300) "..." else ""}")
        
        println("\n🎉 All tests passed! HTTP client is working correctly.")
        
    } catch (e: GooseNetworkException) {
        println("❌ Network Error: ${e.message}")
        println("💡 Check if Goose server is running at $baseUrl")
    } catch (e: GooseAuthException) {
        println("❌ Authentication Error: ${e.message}")
        println("💡 Check if the API key is correct")
    } catch (e: GooseApiException) {
        println("❌ API Error: ${e.message}")
        println("💡 Check the API endpoint and request format")
    } catch (e: Exception) {
        println("❌ Unexpected Error: ${e.message}")
        e.printStackTrace()
    }
}

private operator fun String.times(count: Int): String = this.repeat(count)