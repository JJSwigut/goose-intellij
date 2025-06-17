package com.block.gooseintellij.model

import kotlinx.serialization.Serializable

/**
 * Represents a Goose session from the HTTP API
 */
@Serializable
data class GooseSession(
    val id: String,
    val name: String,
    val created: String,
    val lastActivity: String? = null,
    val status: String = "active"
)