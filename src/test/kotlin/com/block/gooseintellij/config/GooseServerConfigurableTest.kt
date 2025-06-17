package com.block.gooseintellij.config

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

/**
 * Unit tests for GooseServerConfigurable
 * 
 * Note: These tests are limited because GooseServerConfigurable depends on IntelliJ services
 * that are not available in the unit test environment. For full integration testing,
 * use the IntelliJ platform test framework.
 */
class GooseServerConfigurableTest {

    @Test
    fun `test GooseServerConfigurable class exists`() {
        // Verify that the class can be referenced
        val clazz = GooseServerConfigurable::class.java
        assertNotNull(clazz)
        assertEquals("GooseServerConfigurable", clazz.simpleName)
    }

    @Test
    fun `test GooseServerConfigurable has required methods`() {
        // Verify that all required methods exist at the class level
        val configurableClass = GooseServerConfigurable::class.java
        
        val displayNameMethod = configurableClass.methods.find { it.name == "getDisplayName" }
        assertNotNull(displayNameMethod, "getDisplayName method should exist")
        
        val createComponentMethod = configurableClass.methods.find { it.name == "createComponent" }
        assertNotNull(createComponentMethod, "createComponent method should exist")
        
        val isModifiedMethod = configurableClass.methods.find { it.name == "isModified" }
        assertNotNull(isModifiedMethod, "isModified method should exist")
        
        val applyMethod = configurableClass.methods.find { it.name == "apply" }
        assertNotNull(applyMethod, "apply method should exist")
        
        val resetMethod = configurableClass.methods.find { it.name == "reset" }
        assertNotNull(resetMethod, "reset method should exist")
    }

    @Test
    fun `test GooseServerConfigurable implements expected interfaces`() {
        // Verify that the class implements the expected IntelliJ interfaces
        val interfaces = GooseServerConfigurable::class.java.interfaces
        val interfaceNames = interfaces.map { it.simpleName }
        
        // Should implement Configurable interface (directly or through inheritance)
        assertTrue(
            interfaceNames.any { it.contains("Configurable") } || 
            GooseServerConfigurable::class.java.superclass?.interfaces?.any { it.simpleName.contains("Configurable") } == true,
            "GooseServerConfigurable should implement Configurable interface"
        )
    }
}