package com.block.gooseintellij.model

import kotlinx.serialization.Serializable

/**
 * Represents the context information for creating a Goose session
 */
@Serializable
data class GooseContext(
    val projectPath: String,
    val projectName: String,
    val language: String = "kotlin",
    val framework: String = "intellij-plugin",
    val additionalContext: Map<String, String> = emptyMap()
)