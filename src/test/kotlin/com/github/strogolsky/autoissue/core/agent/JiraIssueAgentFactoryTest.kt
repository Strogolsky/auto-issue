package com.github.strogolsky.autoissue.core.agent

import ai.koog.agents.core.agent.entity.AIAgentGraphStrategy
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLModel
import com.github.strogolsky.autoissue.core.agent.strategy.IssueStrategyFactory
import com.github.strogolsky.autoissue.core.agent.strategy.JiraStrategyRegistry
import com.github.strogolsky.autoissue.core.input.IssueGenerationInput
import com.github.strogolsky.autoissue.core.output.JiraIssueCandidate
import com.github.strogolsky.autoissue.plugin.config.LlmAgentConfig
import com.github.strogolsky.autoissue.plugin.startup.LangfuseConfigLoader
import com.intellij.openapi.application.Application
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

class JiraIssueAgentFactoryTest {
    private val project = mockk<Project>()
    private val application = mockk<Application>()

    private val providerRegistry = mockk<LlmProviderRegistry>()
    private val strategyRegistry = mockk<JiraStrategyRegistry>()

    private val provider = mockk<LlmProvider>()
    private val executor = mockk<PromptExecutor>()
    private val llmModel = mockk<LLModel>()

    private val strategyFactory = mockk<IssueStrategyFactory<IssueGenerationInput, JiraIssueCandidate>>()
    private val strategy = mockk<AIAgentGraphStrategy<IssueGenerationInput, JiraIssueCandidate>>()

    private val config =
        LlmAgentConfig(
            apiKey = "test-key",
            provider = "google",
            systemPrompt = "prompt",
            temperature = 0.7,
            maxIterations = 5,
            strategyId = "jira-strategy",
        )

    private lateinit var factory: JiraIssueAgentFactory

    @Before
    fun setUp() {
        val mockLogger = mockk<Logger>(relaxed = true)
        mockkStatic(Logger::class)
        every { Logger.getInstance(any<Class<*>>()) } returns mockLogger
        every { Logger.getInstance(any<String>()) } returns mockLogger

        mockkStatic(ApplicationManager::class)
        mockkObject(LangfuseConfigLoader)

        every { ApplicationManager.getApplication() } returns application

        every { application.getService(LlmProviderRegistry::class.java) } returns providerRegistry
        every { application.getService(JiraStrategyRegistry::class.java) } returns strategyRegistry

        every { LangfuseConfigLoader.load() } returns null

        factory = JiraIssueAgentFactory(project)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun shouldCreateAgentAdapterWhenConfigIsValid() {
        // --- TEST FLOW ---
        // 1. ARRANGE
        every { providerRegistry.getProvider(config.provider) } returns provider
        every { provider.createExecutor(config.apiKey) } returns executor
        every { provider.defaultModel } returns llmModel

        every { strategyRegistry.findFactory(config.provider, config.strategyId) } returns strategyFactory
        every { strategyFactory.createStrategy(project) } returns strategy

        // 2. ACT
        val result = factory.createAgent(config)

        // 3. ASSERT
        assertNotNull(result)
        verify(exactly = 1) { providerRegistry.getProvider(config.provider) }
        verify(exactly = 1) { strategyRegistry.findFactory(config.provider, config.strategyId) }
    }

    @Test
    fun shouldThrowErrorWhenStrategyIsNotFound() {
        // --- TEST FLOW ---
        // 1. ARRANGE
        every { providerRegistry.getProvider(any()) } returns provider
        every { provider.createExecutor(any()) } returns executor
        every { provider.defaultModel } returns llmModel

        every { strategyRegistry.findFactory(any(), any()) } returns null

        // 2. ACT & 3. ASSERT
        assertThrows(IllegalStateException::class.java) {
            factory.createAgent(config)
        }
    }
}
