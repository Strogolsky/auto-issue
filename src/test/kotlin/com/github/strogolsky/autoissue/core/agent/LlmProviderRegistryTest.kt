package com.github.strogolsky.autoissue.core.agent

import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class LlmProviderRegistryTest {

    @Test
    fun should_ReturnEmptySet_When_NoProvidersRegistered() {
        // --- TEST FLOW ---
        // 1. ARRANGE & ACT: Initialize registry with an empty list.
        val registry = LlmProviderRegistry(emptyList())

        // 2. ASSERT: Verify providers list is empty.
        assertTrue("Providers list should be empty", registry.providers().isEmpty())
    }

    @Test
    fun should_RegisterAndReturnProviders_When_ValidExtensionsExist() {
        // --- TEST FLOW ---
        // 1. ARRANGE: Create mock providers.
        val googleProvider = mockk<LlmProvider> {
            every { providerKey } returns "google"
        }
        val customProvider = mockk<LlmProvider> {
            every { providerKey } returns "CUSTOM_AI"
        }

        // 2. ACT: Initialize registry with injected providers.
        val registry = LlmProviderRegistry(listOf(googleProvider, customProvider))

        // 3. ASSERT: Verify all providers are accessible by key and added to the set.
        val keys = registry.providers()
        assertEquals("Should contain 2 providers", 2, keys.size)
        assertTrue(keys.contains("GOOGLE"))
        assertTrue(keys.contains("CUSTOM_AI"))

        assertEquals(googleProvider, registry.getProvider("GOOGLE"))
        assertEquals(customProvider, registry.getProvider("CUSTOM_AI"))
    }

    @Test
    fun should_FindProvider_When_KeyCaseDiffers() {
        // --- TEST FLOW ---
        // 1. ARRANGE: Register a provider with a mixed-case key.
        val openAiProvider = mockk<LlmProvider> {
            every { providerKey } returns "oPeNaI"
        }

        // 2. ACT: Initialize registry and fetch using a different case.
        val registry = LlmProviderRegistry(listOf(openAiProvider))
        val resolvedProvider = registry.getProvider("OpEnAi")

        // 3. ASSERT: Verify the correct provider is returned regardless of case.
        assertEquals(openAiProvider, resolvedProvider)
    }

    @Test
    fun should_ThrowException_When_ProviderNotFound() {
        // --- TEST FLOW ---
        // 1. ARRANGE & ACT: Initialize registry with no providers.
        val registry = LlmProviderRegistry(emptyList())

        // 2. ASSERT: Attempt to get an unknown provider and verify the exception.
        val exception = assertThrows(IllegalStateException::class.java) {
            registry.getProvider("unknown_provider")
        }

        assertTrue(
            "Exception message should contain the unknown key",
            exception.message!!.contains("Unknown LLM provider: unknown_provider")
        )
    }
}