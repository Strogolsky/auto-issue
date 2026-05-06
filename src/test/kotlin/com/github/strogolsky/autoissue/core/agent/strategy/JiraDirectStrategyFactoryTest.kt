package com.github.strogolsky.autoissue.core.agent.strategy

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.testing.feature.*
import ai.koog.agents.testing.tools.getMockExecutor
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.structure.StructuredResponse
import com.github.strogolsky.autoissue.core.context.components.IssueInstruction
import com.github.strogolsky.autoissue.core.context.render.PromptRenderService
import com.github.strogolsky.autoissue.core.input.IssueGenerationInput
import com.github.strogolsky.autoissue.core.output.JiraIssueCandidate
import com.intellij.openapi.project.Project
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.time.ExperimentalTime

class JiraDirectStrategyFactoryTest {

    private lateinit var project: Project
    private lateinit var promptRenderService: PromptRenderService
    private lateinit var factory: JiraDirectStrategyFactory

    @Before
    fun setup() {
        project = mockk()
        promptRenderService = mockk()
        factory = JiraDirectStrategyFactory()

        every { project.getService(PromptRenderService::class.java) } returns promptRenderService
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    private fun createTestInput() = IssueGenerationInput(
        components = listOf(
            IssueInstruction("Core"),
            IssueInstruction("UI")
        )
    )

    private fun createExpectedCandidate(
        title: String = "Fix null pointer exception",
        description: String = "A critical null pointer exception occurs when processing user data"
    ) = JiraIssueCandidate(
        title = title,
        description = description
    )

    private fun createMockLLMResponse(candidate: JiraIssueCandidate): String {
        return """{"title": "${candidate.title}", "description": "${candidate.description}", "labels": []}"""
    }

    @OptIn(ExperimentalTime::class)
    private fun createAgentConfig() = AIAgentConfig(
        prompt = prompt("test-agent") {
            system("test".trimIndent())
        },
        model = mockk<LLModel>(relaxed = true),
        maxAgentIterations = 10
    )

    @OptIn(ExperimentalTime::class)
    private fun createMockExecutor(response: String) = getMockExecutor {
        mockLLMAnswer(response).asDefaultResponse
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun testGraphStructure() = runTest {
        // --- TEST FLOW ---
        // 1. ARRANGE
        val testInput = createTestInput()
        val expectedCandidate = createExpectedCandidate()
        val renderedPrompt = "Generated context for UI, Core"

        every { promptRenderService.buildPrompt(any()) } returns renderedPrompt

        val mockLLMApi = createMockExecutor(createMockLLMResponse(expectedCandidate))
        val strategy = factory.createStrategy(project)
        val agentConfig = createAgentConfig()

        val agent = AIAgent(
            promptExecutor = mockLLMApi,
            strategy = strategy,
            agentConfig = agentConfig
        ) {
            withTesting()

            // 2. ACT & 3. ASSERT
            testGraph<IssueGenerationInput, JiraIssueCandidate>("jira_issue_generation") {
                val nodePrepareContext = assertNodeByName<IssueGenerationInput, String>("prepare_context")
                val nodeCallLLM =
                    assertNodeByName<String, Result<StructuredResponse<JiraIssueCandidate>>>("llm_structured_request")
                val nodeProcessResult =
                    assertNodeByName<Result<StructuredResponse<JiraIssueCandidate>>, JiraIssueCandidate>("process_result")

                assertEdges {
                    startNode() alwaysGoesTo nodePrepareContext
                    nodePrepareContext alwaysGoesTo nodeCallLLM
                    nodeCallLLM alwaysGoesTo nodeProcessResult
                    nodeProcessResult alwaysGoesTo finishNode()
                }

                assertReachable(startNode(), nodePrepareContext)
                assertReachable(nodePrepareContext, nodeCallLLM)
                assertReachable(nodeCallLLM, nodeProcessResult)
                assertReachable(nodeProcessResult, finishNode())
                assertReachable(startNode(), finishNode())
            }
        }

        agent.run(testInput)
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun testGraphNodesBehave() = runTest {
        // --- TEST FLOW ---
        // 1. ARRANGE
        val testInput = createTestInput()
        val expectedCandidate = createExpectedCandidate()
        val renderedPrompt = "Rendered prompt with components: Core, UI"

        every { promptRenderService.buildPrompt(any()) } returns renderedPrompt

        val mockLLMApi = createMockExecutor(createMockLLMResponse(expectedCandidate))
        val strategy = factory.createStrategy(project)
        val agentConfig = createAgentConfig()

        val agent = AIAgent(
            promptExecutor = mockLLMApi,
            strategy = strategy,
            agentConfig = agentConfig
        ) {
            withTesting()

            // 2. ACT & 3. ASSERT
            testGraph<IssueGenerationInput, JiraIssueCandidate>("jira_issue_generation") {
                val nodePrepareContext =
                    assertNodeByName<IssueGenerationInput, String>("prepare_context")
                val nodeCallLLM =
                    assertNodeByName<String, Result<StructuredResponse<JiraIssueCandidate>>>("llm_structured_request")
                val nodeProcessResult =
                    assertNodeByName<Result<StructuredResponse<JiraIssueCandidate>>, JiraIssueCandidate>("process_result")

                assertEdges {
                    nodePrepareContext withOutput renderedPrompt goesTo nodeCallLLM
                    nodeCallLLM alwaysGoesTo nodeProcessResult
                }
            }
        }

        agent.run(testInput)

        verify { promptRenderService.buildPrompt(any()) }
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun testGraphExecution() = runTest {
        // --- TEST FLOW ---
        // 1. ARRANGE
        val testInput = createTestInput()
        val expectedCandidate = createExpectedCandidate(
            title = "Fix null pointer exception",
            description = "A critical null pointer exception occurs when processing user data"
        )
        val renderedPrompt = "Generate Jira issue for: Core, UI components"

        every { promptRenderService.buildPrompt(any()) } returns renderedPrompt

        val mockLLMApi = createMockExecutor(createMockLLMResponse(expectedCandidate))
        val strategy = factory.createStrategy(project)
        val agentConfig = createAgentConfig()

        val agent = AIAgent(
            promptExecutor = mockLLMApi,
            strategy = strategy,
            agentConfig = agentConfig
        )

        // 2. ACT
        val result = agent.run(testInput)

        // 3. ASSERT
        assertEquals(expectedCandidate.title, result.title, "Title should match")
        assertEquals(expectedCandidate.description, result.description, "Description should match")

        verify { promptRenderService.buildPrompt(any()) }
    }
}