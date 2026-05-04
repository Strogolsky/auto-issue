package com.github.strogolsky.autoissue.core.agent.strategy

import com.github.strogolsky.autoissue.core.input.IssueGenerationInput
import com.github.strogolsky.autoissue.core.output.JiraIssueCandidate
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JiraStrategyRegistryTest {

    @Test
    fun should_ReturnStrategies_When_ProviderKeyMatches() {
        // --- TEST FLOW ---
        // 1. ARRANGE: Create mock factories for different providers.
        val googleFactory1 = mockk<IssueStrategyFactory<IssueGenerationInput, JiraIssueCandidate>> {
            every { providerKey } returns "google"
            every { id } returns "strategy-1"
        }
        val googleFactory2 = mockk<IssueStrategyFactory<IssueGenerationInput, JiraIssueCandidate>> {
            every { providerKey } returns "GOOGLE" // Test case insensitivity
            every { id } returns "strategy-2"
        }
        val openAiFactory = mockk<IssueStrategyFactory<IssueGenerationInput, JiraIssueCandidate>> {
            every { providerKey } returns "openai"
            every { id } returns "strategy-3"
        }

        // 2. ACT: Initialize registry and fetch strategies for "GoOgLe".
        val registry = JiraStrategyRegistry(listOf(googleFactory1, googleFactory2, openAiFactory))
        val strategies = registry.strategiesFor("GoOgLe")

        // 3. ASSERT: Verify only Google strategies are returned.
        assertEquals("Should return exactly 2 strategies", 2, strategies.size)
        assertTrue(strategies.contains(googleFactory1))
        assertTrue(strategies.contains(googleFactory2))
    }

    @Test
    fun should_ReturnEmptyList_When_ProviderIsUnknown() {
        // --- TEST FLOW ---
        // 1. ARRANGE: Create a mock factory for a known provider.
        val googleFactory = mockk<IssueStrategyFactory<IssueGenerationInput, JiraIssueCandidate>> {
            every { providerKey } returns "google"
        }

        // 2. ACT: Initialize registry and fetch strategies for an unknown provider.
        val registry = JiraStrategyRegistry(listOf(googleFactory))
        val strategies = registry.strategiesFor("unknown-provider")

        // 3. ASSERT: Verify the result is an empty list.
        assertTrue("Strategies list should be empty", strategies.isEmpty())
    }

    @Test
    fun should_ReturnSpecificFactory_When_IdExists() {
        // --- TEST FLOW ---
        // 1. ARRANGE: Create mock factories.
        val targetFactory = mockk<IssueStrategyFactory<IssueGenerationInput, JiraIssueCandidate>> {
            every { providerKey } returns "google"
            every { id } returns "jira-direct-strategy"
        }
        val otherFactory = mockk<IssueStrategyFactory<IssueGenerationInput, JiraIssueCandidate>> {
            every { providerKey } returns "google"
            every { id } returns "jira-reasoning-strategy"
        }

        // 2. ACT: Search for the specific factory by ID.
        val registry = JiraStrategyRegistry(listOf(targetFactory, otherFactory))
        val found = registry.findFactory("google", "jira-direct-strategy")

        // 3. ASSERT: Verify the correct factory is returned.
        assertNotNull("Factory should be found", found)
        assertEquals(targetFactory, found)
    }

    @Test
    fun should_ReturnNull_When_IdDoesNotExist() {
        // --- TEST FLOW ---
        // 1. ARRANGE: Create a mock factory.
        val factory = mockk<IssueStrategyFactory<IssueGenerationInput, JiraIssueCandidate>> {
            every { providerKey } returns "google"
            every { id } returns "jira-direct-strategy"
        }

        // 2. ACT: Search for a non-existent ID.
        val registry = JiraStrategyRegistry(listOf(factory))
        val found = registry.findFactory("google", "nonexistent-id")

        // 3. ASSERT: Verify the result is null.
        assertNull("Factory should not be found", found)
    }
}