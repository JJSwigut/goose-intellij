package com.block.gooseintellij.config

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Assertions.*

/**
 * Unit tests for ConfigurationMigrator
 */
class ConfigurationMigratorTest {

    private lateinit var migrator: ConfigurationMigrator

    @BeforeEach
    fun setUp() {
        migrator = ConfigurationMigrator()
    }

    @Test
    fun `test migrator can be instantiated`() {
        // Just verify that the migrator can be created without errors
        assertNotNull(migrator)
    }

    @Test
    fun `test migrator methods exist`() {
        // Verify that the migrator has the expected public interface
        val migratorClass = migrator::class.java
        
        val migrateMethod = migratorClass.methods.find { it.name == "migrateFromTerminalConfig" }
        assertNotNull(migrateMethod, "migrateFromTerminalConfig method should exist")
    }

    @Test
    fun `test ServerConfig data class`() {
        // Test the ServerConfig data class
        val config = ServerConfig(
            serverUrl = "http://test.com",
            apiKey = "test-key",
            connectionTimeout = 60,
            enableStreaming = false,
            profileName = "test-profile",
            provider = "test-provider"
        )
        
        assertEquals("http://test.com", config.serverUrl)
        assertEquals("test-key", config.apiKey)
        assertEquals(60, config.connectionTimeout)
        assertEquals(false, config.enableStreaming)
        assertEquals("test-profile", config.profileName)
        assertEquals("test-provider", config.provider)
    }

    @Test
    fun `test MigrationResult sealed class structure`() {
        // Test that all MigrationResult types can be created
        val successResult = MigrationResult.Success(
            ServerConfig(
                serverUrl = "http://test.com",
                apiKey = "key",
                connectionTimeout = 30,
                enableStreaming = true,
                profileName = "test"
            )
        )
        assertTrue(successResult is MigrationResult.Success)
        
        val noConfigResult = MigrationResult.NoConfigFound
        assertTrue(noConfigResult is MigrationResult.NoConfigFound)
        
        val errorResult = MigrationResult.Error("test error")
        assertTrue(errorResult is MigrationResult.Error)
        assertEquals("test error", errorResult.message)
    }
}