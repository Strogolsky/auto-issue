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

class JiraDirectStrategyFactory(
    private val project: Project,
) : IssueStrategyFactory<IssueGenerationInput, JiraIssueCandidate> {
    private val renderService = project.service<PromptRenderService>()

    override fun createStrategy(): AIAgentGraphStrategy<IssueGenerationInput, JiraIssueCandidate> =
        strategy("jira_issue_generation") {
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
