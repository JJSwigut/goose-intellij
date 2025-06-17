package com.block.gooseintellij.client

import com.block.gooseintellij.model.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Real-world test of the Goose HTTP client against the actual running server
 */
fun main() = runBlocking {
    println("🧪 Testing Real Goose Server Integration")
    println("=" * 50)
    
    val serverPort = "57326" // From running Goose process
    val baseUrl = "http://127.0.0.1:$serverPort"
    
    // Test the actual Goose API structure
    testActualGooseAPI(baseUrl)
}

/**
 * Test against the real Goose server API structure
 */
suspend fun testActualGooseAPI(baseUrl: String) {
    val client = OkHttpClient()
    val json = Json { ignoreUnknownKeys = true }
    val mediaType = "application/json".toMediaType()
    
    try {
        println("\n🔍 Testing /ask endpoint...")
        
        // Based on our curl testing, the API expects:
        // {"prompt": "...", "session_working_dir": "..."}
        val askRequest = ActualAskRequest(
            prompt = "Hello! This is a test from our Kotlin HTTP client. Can you confirm you received this?",
            session_working_dir = "/Users/jswigut/Development/goose-intellij"
        )
        
        val requestBody = json.encodeToString(ActualAskRequest.serializer(), askRequest)
            .toRequestBody(mediaType)
        
        val request = Request.Builder()
            .url("$baseUrl/ask")
            .post(requestBody)
            .build()
        
        println("📤 Sending request: ${askRequest.prompt}")
        println("📂 Working directory: ${askRequest.session_working_dir}")
        
        client.newCall(request).execute().use { response ->
            println("📊 Response code: ${response.code}")
            println("📋 Response headers: ${response.headers}")
            
            if (response.isSuccessful) {
                val responseBody = response.body?.string() ?: "No response body"
                println("✅ Success! Response:")
                println("📥 $responseBody")
            } else {
                val errorBody = response.body?.string() ?: "No error details"
                println("❌ Error response:")
                println("📥 $errorBody")
            }
        }
        
    } catch (e: Exception) {
        println("❌ Exception occurred: ${e.message}")
        e.printStackTrace()
    }
}

/**
 * Actual request structure based on API discovery
 */
@Serializable
data class ActualAskRequest(
    val prompt: String,
    val session_working_dir: String
)

private operator fun String.times(count: Int): String = this.repeat(count)