package com.block.gooseintellij.model

import kotlinx.serialization.Serializable

/**
 * Represents a chat response from the Goose API
 */
@Serializable
data class ChatResponse(
    val message: String,
    val sessionId: String,
    val timestamp: String,
    val status: String = "success",
    val error: String? = null
)