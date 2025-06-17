package com.block.gooseintellij.model

import kotlinx.serialization.Serializable

/**
 * Represents a request to the actual Goose /ask endpoint
 */
@Serializable
data class GooseAskRequest(
    val prompt: String,
    val session_working_dir: String
)

/**
 * Represents a request to the actual Goose /reply endpoint (for streaming)
 */
@Serializable
data class GooseReplyRequest(
    val prompt: String,
    val session_working_dir: String
)