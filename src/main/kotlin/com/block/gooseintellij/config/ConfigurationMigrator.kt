package com.block.gooseintellij.config

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.ProjectManager
import java.io.File
import java.util.*

/**
 * Migrates configuration from terminal-based setup to HTTP client configuration
 */
class ConfigurationMigrator {
    
    companion object {
        private val LOG = Logger.getInstance(ConfigurationMigrator::class.java)
        
        // Known terminal configuration keys
        private const val TERMINAL_PROFILE_KEY = "goose.selected.profile"
        private const val TERMINAL_SESSION_KEY = "goose.saved.session"
        
        // Default mappings for common providers to server URLs
        private val PROVIDER_URL_MAPPINGS = mapOf(
            "openai" to "http://localhost:8000",
            "anthropic" to "http://localhost:8000", 
            "databricks" to "https://your-workspace.cloud.databricks.com",
            "azure" to "https://your-azure-openai.openai.azure.com",
            "local" to "http://localhost:8000"
        )
    }

    /**
     * Migrate from terminal-based configuration
     */
    fun migrateFromTerminalConfig(): MigrationResult {
        return try {
            LOG.info("Starting migration from terminal configuration")
            
            // Try to find configuration from any open project
            val projects = ProjectManager.getInstance().openProjects
            var foundConfig: ServerConfig? = null
            var profileName: String? = null
            
            for (project in projects) {
                val propertiesComponent = PropertiesComponent.getInstance(project)
                val savedProfile = propertiesComponent.getValue(TERMINAL_PROFILE_KEY)
                
                if (savedProfile != null) {
                    LOG.info("Found terminal profile configuration: $savedProfile")
                    profileName = savedProfile
                    foundConfig = extractConfigFromProfile(savedProfile, project.basePath)
                    break
                }
            }
            
            // If no project-specific config found, try to find global Goose configuration
            if (foundConfig == null) {
                foundConfig = extractConfigFromGooseFiles()
            }
            
            if (foundConfig != null) {
                LOG.info("Successfully migrated configuration from terminal setup")
                MigrationResult.Success(foundConfig.copy(profileName = profileName ?: "migrated"))
            } else {
                LOG.info("No terminal configuration found to migrate")
                MigrationResult.NoConfigFound
            }
            
        } catch (e: Exception) {
            LOG.error("Failed to migrate terminal configuration", e)
            MigrationResult.Error("Migration failed: ${e.message}")
        }
    }
    
    /**
     * Extract configuration from profile name and project context
     */
    private fun extractConfigFromProfile(profileName: String, projectPath: String?): ServerConfig? {
        return try {
            // Try to read Goose configuration files
            val gooseConfig = findGooseConfigFiles(projectPath)
            
            if (gooseConfig.isNotEmpty()) {
                parseGooseConfig(gooseConfig, profileName)
            } else {
                // Fallback: create basic config based on profile name
                createBasicConfigFromProfile(profileName)
            }
        } catch (e: Exception) {
            LOG.warn("Failed to extract config from profile: $profileName", e)
            null
        }
    }
    
    /**
     * Find Goose configuration files in common locations
     */
    private fun findGooseConfigFiles(projectPath: String?): Map<String, String> {
        val configFiles = mutableMapOf<String, String>()
        
        val searchPaths = listOfNotNull(
            projectPath?.let { "$it/.goose" },
            System.getProperty("user.home") + "/.config/goose",
            System.getProperty("user.home") + "/.goose"
        )
        
        for (searchPath in searchPaths) {
            val configDir = File(searchPath)
            if (configDir.exists() && configDir.isDirectory) {
                // Look for common config files
                listOf("config.yaml", "config.yml", "profiles.yaml", "profiles.yml").forEach { fileName ->
                    val configFile = File(configDir, fileName)
                    if (configFile.exists()) {
                        try {
                            configFiles[fileName] = configFile.readText()
                            LOG.debug("Found config file: ${configFile.absolutePath}")
                        } catch (e: Exception) {
                            LOG.warn("Failed to read config file: ${configFile.absolutePath}", e)
                        }
                    }
                }
            }
        }
        
        return configFiles
    }
    
    /**
     * Parse Goose configuration files to extract server settings
     */
    private fun parseGooseConfig(configFiles: Map<String, String>, profileName: String): ServerConfig? {
        // Simple parsing - in a real implementation, you might use a YAML parser
        for ((fileName, content) in configFiles) {
            try {
                // Look for provider information
                val providerMatch = Regex("provider:\\s*([\\w-]+)").find(content)
                val provider = providerMatch?.groupValues?.get(1)
                
                // Look for server URL
                val urlMatch = Regex("(?:server|url|endpoint):\\s*([^\\s]+)").find(content)
                val serverUrl = urlMatch?.groupValues?.get(1)
                
                // Look for API key patterns (though these might not be in config files for security)
                val keyMatch = Regex("(?:api_key|apikey|key):\\s*([^\\s]+)").find(content)
                val apiKey = keyMatch?.groupValues?.get(1)
                
                if (provider != null || serverUrl != null) {
                    return ServerConfig(
                        serverUrl = serverUrl ?: PROVIDER_URL_MAPPINGS[provider] ?: "http://localhost:8000",
                        apiKey = apiKey ?: "", // Usually empty for security
                        connectionTimeout = 30,
                        enableStreaming = true,
                        profileName = profileName,
                        provider = provider
                    )
                }
            } catch (e: Exception) {
                LOG.warn("Failed to parse config file: $fileName", e)
            }
        }
        
        return null
    }
    
    /**
     * Extract configuration from global Goose files
     */
    private fun extractConfigFromGooseFiles(): ServerConfig? {
        return try {
            val homeDir = System.getProperty("user.home")
            val gooseConfigDirs = listOf(
                "$homeDir/.config/goose",
                "$homeDir/.goose"
            )
            
            for (configDir in gooseConfigDirs) {
                val config = findGooseConfigFiles(configDir)
                if (config.isNotEmpty()) {
                    val serverConfig = parseGooseConfig(config, "default")
                    if (serverConfig != null) {
                        return serverConfig
                    }
                }
            }
            
            null
        } catch (e: Exception) {
            LOG.warn("Failed to extract config from global Goose files", e)
            null
        }
    }
    
    /**
     * Create basic configuration based on profile name
     */
    private fun createBasicConfigFromProfile(profileName: String): ServerConfig {
        // Try to infer provider from profile name
        val provider = when {
            profileName.contains("databricks", ignoreCase = true) -> "databricks"
            profileName.contains("openai", ignoreCase = true) -> "openai"
            profileName.contains("anthropic", ignoreCase = true) -> "anthropic"
            profileName.contains("azure", ignoreCase = true) -> "azure"
            else -> "local"
        }
        
        val serverUrl = PROVIDER_URL_MAPPINGS[provider] ?: "http://localhost:8000"
        
        return ServerConfig(
            serverUrl = serverUrl,
            apiKey = "", // User will need to configure this
            connectionTimeout = 30,
            enableStreaming = true,
            profileName = profileName,
            provider = provider
        )
    }
}

/**
 * Server configuration extracted from migration
 */
data class ServerConfig(
    val serverUrl: String,
    val apiKey: String,
    val connectionTimeout: Int,
    val enableStreaming: Boolean,
    val profileName: String,
    val provider: String? = null
)

/**
 * Result of configuration migration
 */
sealed class MigrationResult {
    data class Success(val serverConfig: ServerConfig) : MigrationResult()
    object NoConfigFound : MigrationResult()
    data class Error(val message: String) : MigrationResult()
}