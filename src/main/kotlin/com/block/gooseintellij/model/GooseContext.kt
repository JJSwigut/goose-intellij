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
) {
    /**
     * Convert context to a human-readable string for prompt inclusion
     */
    fun toPromptString(): String {
        return buildString {
            append("Project: $projectName")
            append(", Language: $language")
            append(", Framework: $framework")
            if (additionalContext.isNotEmpty()) {
                append(", Additional: ")
                append(additionalContext.entries.joinToString(", ") { "${it.key}=${it.value}" })
            }
        }
    }
}