package com.github.strogolsky.autoissue.plugin.config

import com.github.strogolsky.autoissue.core.agent.strategy.GoogleIssueStrategyFactory
import com.github.strogolsky.autoissue.core.agent.strategy.JiraStrategyRegistry
import com.github.strogolsky.autoissue.core.input.IssueGenerationInput
import com.github.strogolsky.autoissue.core.output.JiraIssueCandidate
import com.github.strogolsky.autoissue.plugin.state.LlmAgentState
import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.generateServiceName
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.application.ApplicationManager
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.testFramework.replaceService
import io.mockk.every
import io.mockk.mockk

class LlmAgentConfigServiceTest : BasePlatformTestCase() {
    private lateinit var service: LlmAgentConfigService
    private lateinit var tokenKey: CredentialAttributes
    private lateinit var strategyRegistryMock: JiraStrategyRegistry

    override fun setUp() {
        super.setUp()
        service = LlmAgentConfigService()
        tokenKey = CredentialAttributes(generateServiceName("AutoIssue", "LlmApiKey"))

        PasswordSafe.instance.setPassword(tokenKey, null)

        strategyRegistryMock = mockk<JiraStrategyRegistry>(relaxed = true)
        ApplicationManager.getApplication().replaceService(
            JiraStrategyRegistry::class.java,
            strategyRegistryMock,
            testRootDisposable,
        )
    }

    override fun tearDown() {
        PasswordSafe.instance.setPassword(tokenKey, null)
        super.tearDown()
    }

    fun testShouldReturnFalseWhenApiKeyIsMissing() {
        // 1. ACT
        val isReady = service.isReady()

        // 2. ASSERT
        assertFalse(isReady)
    }

    fun testShouldReturnTrueWhenApiKeyIsPresent() {
        // 1. ARRANGE
        PasswordSafe.instance.setPassword(tokenKey, "sk-12345")

        // 2. ACT
        val isReady = service.isReady()

        // 3. ASSERT
        assertTrue(isReady)
    }

    fun testShouldThrowExceptionWhenApiKeyIsMissingInEffectiveConfig() {
        // 1. ACT & 2. ASSERT
        try {
            service.getEffectiveConfig()
            fail("Expected IllegalArgumentException to be thrown")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("LLM API Key is missing"))
        }
    }

    fun testShouldPopulateEmptyFieldsWhenApplyDefaultsCalled() {
        // 1. ARRANGE
        val defaults =
            LlmDefaults(
                provider = "google",
                strategyId = "direct",
                temperature = 0.7,
                maxIterations = 3,
                systemPrompt = "You are an agent.",
            )
        every { strategyRegistryMock.findFactory("google", "direct") } returns mockk()

        // 2. ACT
        service.applyDefaults(defaults)

        // 3. ASSERT
        val state = service.state
        assertEquals("google", state.provider)
        assertEquals("direct", state.strategyId)
        assertEquals(0.7, state.temperature, 0.001)
        assertEquals(3, state.maxIterations)
        assertEquals("You are an agent.", state.systemPrompt)
    }

    fun testShouldFallbackToAvailableStrategyWhenCurrentIsInvalid() {
        // 1. ARRANGE
        service.loadState(
            LlmAgentState().apply {
                provider = "google"
                strategyId = "invalid-strategy"
            },
        )

        val fallbackFactory = mockk<GoogleIssueStrategyFactory<IssueGenerationInput, JiraIssueCandidate>>()
        every { fallbackFactory.id } returns "fallback-strategy"

        every { strategyRegistryMock.findFactory("google", "invalid-strategy") } returns null
        every { strategyRegistryMock.strategiesFor("google") } returns listOf(fallbackFactory)

        // 2. ACT
        service.applyDefaults(
            LlmDefaults(
                provider = "google",
                strategyId = "direct",
                temperature = 0.7,
                maxIterations = 3,
                systemPrompt = "Default prompt",
            ),
        )

        // 3. ASSERT
        assertEquals("fallback-strategy", service.state.strategyId)
    }
}
