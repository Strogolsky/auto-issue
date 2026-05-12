package com.github.strogolsky.autoissue.core.agent.strategy

import ai.koog.agents.core.agent.entity.AIAgentGraphStrategy
import ai.koog.agents.core.dsl.builder.node
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.extension.nodeLLMRequestStructured
import ai.koog.prompt.structure.StructuredResponse
import com.github.strogolsky.autoissue.core.context.render.PromptRenderService
import com.github.strogolsky.autoissue.core.input.IssueGenerationInput
import com.github.strogolsky.autoissue.core.output.JiraIssueCandidate
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project

/**
 * Simple, single-step strategy for generating JIRA issues directly.
 *
 * The agent:
 * 1. Prepares a prompt from the input context
 * 2. Calls the LLM with structured output request
 * 3. Processes and returns the structured result
 *
 * Fast and efficient, works well when the LLM can generate correct issues in one call.
 * Does not use tool invocation or iterative refinement.
 *
 * Provider: Google (Gemini)
 * ID: "jira-direct-strategy"
 * Display Name: "Direct generation"
 */
class JiraDirectStrategyFactory : GoogleIssueStrategyFactory<IssueGenerationInput, JiraIssueCandidate>() {
    override val id = "jira-direct-strategy"
    override val displayName = "Direct generation"

    /**
     * Creates the agent strategy graph for direct issue generation.
     *
     * Graph structure:
     * start → prepare_context → llm_structured_request → process_result → finish
     *
     * @param project The IntelliJ project context
     * @return The configured strategy graph
     */
    override fun create(project: Project): AIAgentGraphStrategy<IssueGenerationInput, JiraIssueCandidate> {
        val renderService = project.service<PromptRenderService>()

        return strategy("jira_issue_generation") {
            val nodePrepareContext by node<IssueGenerationInput, String>("prepare_context") { input ->
                thisLogger().debug("Preparing context for LLM prompt")
                renderService.buildPrompt {
                    components(input.components)
                }
            }

            val nodeCallLLM by nodeLLMRequestStructured<JiraIssueCandidate>(
                name = "llm_structured_request",
            )

            val nodeProcessResult by node<Result<StructuredResponse<JiraIssueCandidate>>, JiraIssueCandidate>("process_result") { result ->
                val candidate =
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
                thisLogger().info("Successfully generated structured Jira task")
                candidate
            }

            edge(nodeStart forwardTo nodePrepareContext)
            edge(nodePrepareContext forwardTo nodeCallLLM)
            edge(nodeCallLLM forwardTo nodeProcessResult)
            edge(nodeProcessResult forwardTo nodeFinish)
        }
    }
}
