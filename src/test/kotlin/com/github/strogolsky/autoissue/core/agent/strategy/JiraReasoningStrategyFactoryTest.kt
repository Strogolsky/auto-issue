package com.github.strogolsky.autoissue.core.agent.strategy

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.environment.ReceivedToolResult
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.testing.feature.*
import ai.koog.agents.testing.tools.getMockExecutor
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.clients.google.GoogleModels
import ai.koog.prompt.message.Message
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

class JiraReasoningStrategyFactoryTest {

    private lateinit var project: Project
    private lateinit var promptRenderService: PromptRenderService
    private lateinit var factory: JiraReasoningStrategyFactory

    @Before
    fun setup() {
        project = mockk()
        promptRenderService = mockk()
        factory = JiraReasoningStrategyFactory()

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
    ) = JiraIssueCandidate(title = title, description = description)

    private fun createMockLLMResponse(candidate: JiraIssueCandidate) =
        """{"title": "${candidate.title}", "description": "${candidate.description}", "labels": []}"""

    @OptIn(ExperimentalTime::class)
    private fun createAgentConfig() = AIAgentConfig(
        prompt = prompt("test-agent") {},
        model = mockk(relaxed = true),
        maxAgentIterations = 50
    )

    private fun setupPromptRenderService(
        analysisPrompt: String = "Stage 1: analysis prompt",
        structuringPrompt: String = "Stage 2: structuring prompt"
    ) {
        every { promptRenderService.buildPrompt(any()) } returnsMany listOf(
            analysisPrompt,
            structuringPrompt
        )
    }

    @OptIn(ExperimentalTime::class)
    private fun createMockExecutor(
        analysisPrompt: String,
        structuringPrompt: String,
        candidate: JiraIssueCandidate,
        analysisResponse: String = "Analysis complete: found issues in the code"
    ) = getMockExecutor {
        mockLLMAnswer(analysisResponse) onRequestContains analysisPrompt
        mockLLMAnswer(createMockLLMResponse(candidate)) onRequestContains structuringPrompt
    }


    @OptIn(ExperimentalTime::class)
    @Test
    fun testGraphStructure() = runTest {
        // --- TEST FLOW ---
        // 1. ARRANGE
        val testInput = createTestInput()
        val expectedCandidate = createExpectedCandidate()
        val analysisPrompt = "Stage 1: analysis prompt"
        val structuringPrompt = "Stage 2: structuring prompt"

        setupPromptRenderService(analysisPrompt, structuringPrompt)

        val mockLLMApi = createMockExecutor(analysisPrompt, structuringPrompt, expectedCandidate)
        val strategy = factory.createStrategy(project)
        val agentConfig = createAgentConfig()

        val agent = AIAgent(
            promptExecutor = mockLLMApi,
            strategy = strategy,
            agentConfig = agentConfig,
            toolRegistry = ToolRegistry { }
        ) {
            withTesting()

            // 2. ACT & 3. ASSERT
            testGraph<IssueGenerationInput, JiraIssueCandidate>("jira_reasoning_strategy") {

                val analysisSubgraph =
                    assertSubgraphByName<IssueGenerationInput, AnalysisStageResult>("analysis")
                val structuringSubgraph =
                    assertSubgraphByName<AnalysisStageResult, JiraIssueCandidate>("structuring")

                assertEdges {
                    startNode() alwaysGoesTo analysisSubgraph
                    analysisSubgraph alwaysGoesTo structuringSubgraph
                    structuringSubgraph alwaysGoesTo finishNode()
                }

                assertReachable(startNode(), analysisSubgraph)
                assertReachable(analysisSubgraph, structuringSubgraph)
                assertReachable(structuringSubgraph, finishNode())
                assertReachable(startNode(), finishNode())

                verifySubgraph(analysisSubgraph) {
                    val nodePrepare =
                        assertNodeByName<IssueGenerationInput, String>("prepare_analysis_prompt")
                    val nodeAnalyze =
                        assertNodeByName<String, Message.Response>("analyze")
                    val nodeExecTool =
                        assertNodeByName<Message.Tool.Call, ReceivedToolResult>("nodeExecAnalysisTool")
                    val nodeSendToolResult =
                        assertNodeByName<ReceivedToolResult, Message.Response>("nodeSendToolResult")
                    val nodeExtract =
                        assertNodeByName<String, AnalysisStageResult>("extract_analysis_text")

                    assertReachable(startNode(), nodePrepare)
                    assertReachable(nodePrepare, nodeAnalyze)
                    assertReachable(nodeAnalyze, nodeExtract)
                    assertReachable(nodeAnalyze, nodeExecTool)
                    assertReachable(nodeExecTool, nodeSendToolResult)
                    assertReachable(nodeSendToolResult, nodeExtract)
                    assertReachable(nodeExtract, finishNode())
                }

                verifySubgraph(structuringSubgraph) {
                    val nodeBuildPrompt =
                        assertNodeByName<AnalysisStageResult, String>("build_structuring_prompt")
                    val nodeStructured =
                        assertNodeByName<String, Result<StructuredResponse<JiraIssueCandidate>>>("llm_structured_request")
                    val nodeUnwrap =
                        assertNodeByName<Result<StructuredResponse<JiraIssueCandidate>>, JiraIssueCandidate>("unwrap_structured")

                    assertReachable(startNode(), nodeBuildPrompt)
                    assertReachable(nodeBuildPrompt, nodeStructured)
                    assertReachable(nodeStructured, nodeUnwrap)
                    assertReachable(nodeUnwrap, finishNode())
                }
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
        val analysisPrompt = "Stage 1: analysis prompt"
        val structuringPrompt = "Stage 2: structuring prompt"
        val analysisResponse = "Analysis complete: found issues in the code"

        setupPromptRenderService(analysisPrompt, structuringPrompt)

        val mockLLMApi = createMockExecutor(analysisPrompt, structuringPrompt, expectedCandidate, analysisResponse)
        val strategy = factory.createStrategy(project)
        val agentConfig = createAgentConfig()

        val agent = AIAgent(
            promptExecutor = mockLLMApi,
            strategy = strategy,
            agentConfig = agentConfig,
            toolRegistry = ToolRegistry { }
        ) {
            withTesting()

            // 2. ACT & 3. ASSERT
            testGraph<IssueGenerationInput, JiraIssueCandidate>("jira_reasoning_strategy") {
                val analysisSubgraph =
                    assertSubgraphByName<IssueGenerationInput, AnalysisStageResult>("analysis")
                val structuringSubgraph =
                    assertSubgraphByName<AnalysisStageResult, JiraIssueCandidate>("structuring")

                assertEdges {
                    startNode() alwaysGoesTo analysisSubgraph
                    analysisSubgraph alwaysGoesTo structuringSubgraph
                    structuringSubgraph alwaysGoesTo finishNode()
                }

                verifySubgraph(analysisSubgraph) {
                    val nodeAnalyze =
                        assertNodeByName<String, Message.Response>("analyze")
                    val nodeExecTool =
                        assertNodeByName<Message.Tool.Call, ReceivedToolResult>("nodeExecAnalysisTool")
                    val nodeSendToolResult =
                        assertNodeByName<ReceivedToolResult, Message.Response>("nodeSendToolResult")
                    val nodeExtract =
                        assertNodeByName<String, AnalysisStageResult>("extract_analysis_text")

                    assertEdges {
                        nodeAnalyze withOutput assistantMessage(analysisResponse) goesTo nodeExtract

                        nodeExecTool alwaysGoesTo nodeSendToolResult
                    }
                }

                verifySubgraph(structuringSubgraph) {
                    val nodeBuildPrompt =
                        assertNodeByName<AnalysisStageResult, String>("build_structuring_prompt")
                    val nodeStructured =
                        assertNodeByName<String, Result<StructuredResponse<JiraIssueCandidate>>>("llm_structured_request")
                    val nodeUnwrap =
                        assertNodeByName<Result<StructuredResponse<JiraIssueCandidate>>, JiraIssueCandidate>("unwrap_structured")

                    assertEdges {
                        nodeBuildPrompt withOutput structuringPrompt goesTo nodeStructured
                        nodeStructured alwaysGoesTo nodeUnwrap
                        nodeUnwrap alwaysGoesTo finishNode()
                    }
                }
            }
        }

        agent.run(testInput)

        verify(exactly = 2) { promptRenderService.buildPrompt(any()) }
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
        val analysisPrompt = "Stage 1: analysis prompt"
        val structuringPrompt = "Stage 2: structuring prompt"

        setupPromptRenderService(analysisPrompt, structuringPrompt)

        val mockLLMApi = createMockExecutor(analysisPrompt, structuringPrompt, expectedCandidate)
        val strategy = factory.createStrategy(project)
        val agentConfig = createAgentConfig()

        val agent = AIAgent(
            promptExecutor = mockLLMApi,
            strategy = strategy,
            agentConfig = agentConfig,
            toolRegistry = ToolRegistry { }
        )

        // 2. ACT
        val result = agent.run(testInput)

        // 3. ASSERT
        assertEquals(expectedCandidate.title, result.title, "Title should match")
        assertEquals(expectedCandidate.description, result.description, "Description should match")

        verify(exactly = 2) { promptRenderService.buildPrompt(any()) }
    }
}