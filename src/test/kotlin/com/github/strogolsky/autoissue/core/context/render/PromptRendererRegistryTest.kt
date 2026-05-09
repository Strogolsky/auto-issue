package com.github.strogolsky.autoissue.core.context.render

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import io.mockk.every
import io.mockk.mockk

class PromptRendererRegistryTest : BasePlatformTestCase() {
    private lateinit var registry: PromptRendererRegistry

    override fun setUp() {
        super.setUp()
        registry = PromptRendererRegistry()
    }

    fun testShouldResolveRendererAfterRegister() {
        // --- TEST FLOW ---
        // 1. ARRANGE
        val renderer = mockk<PromptRenderer>()
        every { renderer.rendererKey() } returns "CUSTOM"
        registry.register(renderer)

        // 2. ACT
        val resolved = registry.resolve("CUSTOM")

        // 3. ASSERT
        assertSame(renderer, resolved)
    }

    fun testShouldThrowWhenResolvingUnknownKey() {
        // --- TEST FLOW ---
        // 1. ARRANGE: registry has no renderer for "UNKNOWN"

        // 2. ACT & 3. ASSERT
        try {
            registry.resolve("UNKNOWN")
            fail("Expected IllegalStateException to be thrown")
        } catch (e: IllegalStateException) {
            assertTrue(e.message!!.contains("UNKNOWN"))
        }
    }
}
