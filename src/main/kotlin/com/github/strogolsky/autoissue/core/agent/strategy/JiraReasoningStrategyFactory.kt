package com.github.strogolsky.autoissue.core.agent.strategy

import ai.koog.agents.core.agent.entity.AIAgentGraphStrategy
import ai.koog.agents.core.dsl.builder.node
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.builder.subgraph
import ai.koog.agents.core.dsl.extension.nodeExecuteTool
import ai.koog.agents.core.dsl.extension.nodeLLMRequest
import ai.koog.agents.core.dsl.extension.nodeLLMRequestStructured
import ai.koog.agents.core.dsl.extension.nodeLLMSendToolResult
import ai.koog.agents.core.dsl.extension.onAssistantMessage
import ai.koog.agents.core.dsl.extension.onToolCall
import ai.koog.prompt.executor.clients.google.GoogleModels
import ai.koog.prompt.structure.StructuredResponse
import com.github.strogolsky.autoissue.core.context.render.PromptRenderService
import com.github.strogolsky.autoissue.core.input.IssueGenerationInput
import com.github.strogolsky.autoissue.core.output.JiraIssueCandidate
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project

class JiraReasoningStrategyFactory : GoogleIssueStrategyFactory<IssueGenerationInput, JiraIssueCandidate>() {
    override val id = "jira-reasoning-strategy"
    override val displayName = "Reasoning (analysis + structuring)"

    override fun createStrategy(project: Project): AIAgentGraphStrategy<IssueGenerationInput, JiraIssueCandidate> {
        val renderService = project.service<PromptRenderService>()
        var originalInput: IssueGenerationInput? = null

        return strategy("jira_reasoning_strategy") {
            val analysisSubgraph by subgraph<IssueGenerationInput, AnalysisStageResult>(
                name = "analysis",
                llmModel = GoogleModels.Gemini2_5Flash,
            ) {
                val nodePrepare by node<IssueGenerationInput, String>("prepare_analysis_prompt") { input ->
                    originalInput = input
                    thisLogger().debug("Analysis stage: preparing prompt")
                    buildAnalysisPrompt(renderService, input)
                }

                val nodeAnalyze by nodeLLMRequest(name = "analyze")
                val nodeExecAnalysisTool by nodeExecuteTool()
                val nodeSendToolResult by nodeLLMSendToolResult()

                val nodeExtract by node<String, AnalysisStageResult>("extract_analysis_text") { content ->
                    thisLogger().info("Analysis stage produced ${content.length} chars")
                    val input =
                        originalInput
                            ?: error("IssueGenerationInput is missing — analysis subgraph must run before structuring")
                    AnalysisStageResult(input, content)
                }

                edge(nodeStart forwardTo nodePrepare)
                edge(nodePrepare forwardTo nodeAnalyze)
                edge(nodeAnalyze forwardTo nodeExtract onAssistantMessage { true })
                edge(nodeAnalyze forwardTo nodeExecAnalysisTool onToolCall { true })
                edge(nodeExecAnalysisTool forwardTo nodeSendToolResult)
                edge(nodeSendToolResult forwardTo nodeExecAnalysisTool onToolCall { true })
                edge(nodeSendToolResult forwardTo nodeExtract onAssistantMessage { true })
                edge(nodeExtract forwardTo nodeFinish)
            }

            val structuringSubgraph by subgraph<AnalysisStageResult, JiraIssueCandidate>(
                name = "structuring",
                llmModel = GoogleModels.Gemini2_5FlashLite,
            ) {
                val nodeBuildPrompt by node<AnalysisStageResult, String>("build_structuring_prompt") { context ->
                    buildStructuringPrompt(renderService, context)
                }

                val nodeStructured by nodeLLMRequestStructured<JiraIssueCandidate>(
                    name = "llm_structured_request",
                )

                val nodeUnwrap by node<Result<StructuredResponse<JiraIssueCandidate>>, JiraIssueCandidate>(
                    name = "unwrap_structured",
                ) { result ->
                    result
                        .onFailure {
                            thisLogger().error(
                                "Structured output failed: LLM could not map" +
                                    "response to JiraIssueCandidate schema.",
                                it,
                            )
                        }
                        .getOrThrow()
                        .data ?: error("Structured result was success but data was null")
                }

                edge(nodeStart forwardTo nodeBuildPrompt)
                edge(nodeBuildPrompt forwardTo nodeStructured)
                edge(nodeStructured forwardTo nodeUnwrap)
                edge(nodeUnwrap forwardTo nodeFinish)
            }

            nodeStart then analysisSubgraph then structuringSubgraph then nodeFinish
        }
    }

    private fun buildAnalysisPrompt(
        renderService: PromptRenderService,
        input: IssueGenerationInput,
    ): String =
        renderService.buildPrompt {
            instruction(
                """
                Stage 1 of 2 — Analysis. You are a technical analyst evaluating a TODO comment within its codebase context.
                Do not generate the final ticket yet. Instead, thoroughly investigate the surrounding code to understand the implementation details and dependencies.
                Use the available tools to explore the codebase as needed.
                Once you have gathered sufficient information, output a concise technical analysis in plain prose.
                """.trimIndent(),
            )

            components(input.components)
        }

    private fun buildStructuringPrompt(
        renderService: PromptRenderService,
        context: AnalysisStageResult,
    ): String =
        renderService.buildPrompt {
            instruction("Stage 2 of 2 — produce the ticket now, strictly according to the schema and the rules in the system prompt.")
            instruction("Use the analysis below as input; the original context is included again for reference.")

            section("Analysis", context.analysisText)
            components(context.originalInput.components)
        }
}
