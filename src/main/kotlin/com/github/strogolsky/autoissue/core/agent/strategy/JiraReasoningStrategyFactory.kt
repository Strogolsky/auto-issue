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
import ai.koog.prompt.structure.StructuredResponse
import com.github.strogolsky.autoissue.core.context.render.PromptRenderService
import com.github.strogolsky.autoissue.core.input.IssueGenerationInput
import com.github.strogolsky.autoissue.core.output.JiraIssueCandidate
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project

class JiraReasoningStrategyFactory(
    private val project: Project,
) : IssueStrategyFactory<IssueGenerationInput, JiraIssueCandidate> {
    private val renderService = project.service<PromptRenderService>()

    override fun createStrategy(): AIAgentGraphStrategy<IssueGenerationInput, JiraIssueCandidate> {
        var originalInput: IssueGenerationInput? = null

        return strategy("jira_reasoning_strategy") {
            val analysisSubgraph by subgraph<IssueGenerationInput, AnalysisStageResult>(
                name = "analysis",
            ) {
                val nodePrepare by node<IssueGenerationInput, String>("prepare_analysis_prompt") { input ->
                    originalInput = input
                    thisLogger().debug("Analysis stage: preparing prompt")
                    buildAnalysisPrompt(input)
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
            ) {
                val nodeBuildPrompt by node<AnalysisStageResult, String>("build_structuring_prompt") { context ->
                    buildStructuringPrompt(context)
                }

                val nodeStructured by nodeLLMRequestStructured<JiraIssueCandidate>(
                    name = "llm_structured_request",
                )

                val nodeUnwrap by node<Result<StructuredResponse<JiraIssueCandidate>>, JiraIssueCandidate>(
                    name = "unwrap_structured",
                ) { result ->
                    if (result.isSuccess) {
                        result.getOrNull()?.data
                            ?: throw IllegalStateException("Structured result was success but data was null")
                    } else {
                        val err = result.exceptionOrNull()
                        thisLogger().error(
                            "Structured output failed: LLM could not map response to JiraIssueCandidate schema.",
                            err,
                        )
                        throw err ?: RuntimeException("Unknown structured parsing error")
                    }
                }

                edge(nodeStart forwardTo nodeBuildPrompt)
                edge(nodeBuildPrompt forwardTo nodeStructured)
                edge(nodeStructured forwardTo nodeUnwrap)
                edge(nodeUnwrap forwardTo nodeFinish)
            }

            nodeStart then analysisSubgraph then structuringSubgraph then nodeFinish
        }
    }

    private fun buildAnalysisPrompt(input: IssueGenerationInput): String =
        renderService.buildPrompt {
            instruction("Stage 1 of 2 — analysis only. Do not produce the ticket yet; the next stage will.")
            instruction("Write a concise technical analysis of the TODO based on the context below, in plain prose.")

            components(input.components)
        }

    private fun buildStructuringPrompt(context: AnalysisStageResult): String =
        renderService.buildPrompt {
            instruction("Stage 2 of 2 — produce the ticket now, strictly according to the schema and the rules in the system prompt.")
            instruction("Use the analysis below as input; the original context is included again for reference.")

            section("Analysis", context.analysisText)
            components(context.originalInput.components)
        }
}
