package com.github.strogolsky.autoissue.core

import com.github.strogolsky.autoissue.core.agent.JiraIssueAgentFactory
import com.github.strogolsky.autoissue.core.agent.KoogAgentAdapter
import com.github.strogolsky.autoissue.core.context.ContextEnvironment
import com.github.strogolsky.autoissue.core.context.ContextRegistry
import com.github.strogolsky.autoissue.core.context.components.ContextComponent
import com.github.strogolsky.autoissue.core.exceptions.IssueGenerationException
import com.github.strogolsky.autoissue.core.input.IssueGenerationInput
import com.github.strogolsky.autoissue.core.output.JiraIssueCandidate
import com.github.strogolsky.autoissue.plugin.config.LlmAgentConfig
import com.github.strogolsky.autoissue.plugin.config.LlmAgentConfigService
import com.intellij.openapi.application.Application
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class JiraIssueGenerationServiceTest {

    private val project = mockk<Project>()
    private val application = mockk<Application>()

    private val factory = mockk<JiraIssueAgentFactory>()
    private val registry = mockk<ContextRegistry>()
    private val agentConfigService = mockk<LlmAgentConfigService>()

    private val agent = mockk<KoogAgentAdapter<IssueGenerationInput, JiraIssueCandidate>>()
    private val config = mockk<LlmAgentConfig>()
    private val env = mockk<ContextEnvironment>()

    private lateinit var service: JiraIssueGenerationService

    @Before
    fun setUp() {
        mockkStatic(ApplicationManager::class)
        every { ApplicationManager.getApplication() } returns application

        every { project.getService(JiraIssueAgentFactory::class.java) } returns factory
        every { project.getService(ContextRegistry::class.java) } returns registry
        every { application.getService(LlmAgentConfigService::class.java) } returns agentConfigService

        service = JiraIssueGenerationService(project)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun should_ReturnCandidate_When_ContextIsValid() = runTest {
        // --- TEST FLOW ---
        // 1. ARRANGE
        val mockComponent = mockk<ContextComponent>()
        every { agentConfigService.getEffectiveConfig() } returns config
        coEvery { registry.gatherAll(env) } returns listOf(mockComponent)
        every { factory.createAgent(config) } returns agent

        val expectedCandidate = JiraIssueCandidate("Fix Auth", "Description")
        coEvery { agent.generate(any()) } returns expectedCandidate

        // 2. ACT
        val result = service.generateTask("Fix auth system", env)

        // 3. ASSERT
        assertEquals(expectedCandidate, result)
        coVerify(exactly = 1) { agent.generate(match { it.components.size == 2 }) }
    }

    @Test
    fun should_ReturnCandidate_When_ContextIsEmpty() = runTest {
        // --- TEST FLOW ---
        // 1. ARRANGE
        every { agentConfigService.getEffectiveConfig() } returns config
        coEvery { registry.gatherAll(env) } returns emptyList()
        every { factory.createAgent(config) } returns agent

        val expectedCandidate = JiraIssueCandidate("Title", "Desc")
        coEvery { agent.generate(any()) } returns expectedCandidate

        // 2. ACT
        val result = service.generateTask("Instruction", env)

        // 3. ASSERT
        assertEquals("Title", result.title)
        coVerify(exactly = 1) { agent.generate(match { it.components.size == 1 }) }
    }

    @Test
    fun should_ThrowException_When_AgentReturnsNull() = runTest {
        // --- TEST FLOW ---
        // 1. ARRANGE
        every { agentConfigService.getEffectiveConfig() } returns config
        coEvery { registry.gatherAll(env) } returns emptyList()
        every { factory.createAgent(config) } returns agent

        coEvery { agent.generate(any()) } returns null

        // 2. ACT & 3. ASSERT
        try {
            service.generateTask("Instruction", env)
            fail("Expected IssueGenerationException to be thrown")
        } catch (e: IssueGenerationException) {
            assertEquals("The AI agent returned an empty response. Please check your prompt or API limits.", e.message)
        }
    }

    @Test
    fun should_AbortGeneration_When_ConfigIsInvalid() = runTest {
        // --- TEST FLOW ---
        // 1. ARRANGE
        every { agentConfigService.getEffectiveConfig() } throws IllegalArgumentException("Invalid API Key")

        // 2. ACT & 3. ASSERT
        try {
            service.generateTask("Instruction", env)
            fail("Expected IssueGenerationException to be thrown")
        } catch (e: IssueGenerationException) {
            assertEquals("Invalid API Key", e.message)
        }

        coVerify(exactly = 0) { registry.gatherAll(any()) }
    }
}