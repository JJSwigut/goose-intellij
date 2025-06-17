package com.block.gooseintellij.actions

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

/**
 * Simple unit tests for GooseActionHelper
 * Note: Full integration tests would require IntelliJ test framework setup
 */
class GooseActionHelperIntegrationTest {

    @Test
    fun `test deprecated methods exist and return expected values`() {
        // Test that deprecated methods still exist for backward compatibility
        
        // checkGooseAvailability with null project should return false
        val result = GooseActionHelper.checkGooseAvailability(null)
        assertFalse(result)
        
        // askGooseToGenerateTests should not throw (deprecated)
        assertDoesNotThrow {
            GooseActionHelper.askGooseToGenerateTests(null, "test question")
        }
    }

    @Test
    fun `test GooseActionHelper object exists and has expected methods`() {
        // Verify that the main methods exist
        val helperClass = GooseActionHelper::class.java
        
        // Check that main method exists
        val checkAndSendMethod = helperClass.methods.find { it.name == "checkAndSendToGoose" }
        assertNotNull(checkAndSendMethod, "checkAndSendToGoose method should exist")
        
        // Check that deprecated methods exist
        val checkAvailabilityMethod = helperClass.methods.find { it.name == "checkGooseAvailability" }
        assertNotNull(checkAvailabilityMethod, "checkGooseAvailability method should exist for backward compatibility")
        
        val getTerminalMethod = helperClass.methods.find { it.name == "getGooseTerminal" }
        assertNotNull(getTerminalMethod, "getGooseTerminal method should exist for backward compatibility")
        
        val askGooseMethod = helperClass.methods.find { it.name == "askGooseToGenerateTests" }
        assertNotNull(askGooseMethod, "askGooseToGenerateTests method should exist for backward compatibility")
    }

    @Test
    fun `test helper methods are accessible`() {
        // This test verifies that the GooseActionHelper object is properly accessible
        // and doesn't throw any initialization errors
        
        assertNotNull(GooseActionHelper)
        assertTrue(GooseActionHelper::class.java.kotlin.objectInstance != null)
    }
}