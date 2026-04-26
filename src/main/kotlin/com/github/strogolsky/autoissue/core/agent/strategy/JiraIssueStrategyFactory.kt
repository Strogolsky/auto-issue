package com.github.strogolsky.autoissue.core.agent.strategy

import ai.koog.agents.core.agent.entity.AIAgentGraphStrategy
import ai.koog.agents.core.dsl.builder.node
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.extension.nodeLLMRequestStructured
import ai.koog.prompt.structure.StructuredResponse
import com.github.strogolsky.autoissue.core.context.render.PromptRenderService
import com.github.strogolsky.autoissue.core.input.AgentInput
import com.github.strogolsky.autoissue.core.output.JiraTaskCandidate
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project

class JiraIssueStrategyFactory(
    private val project: Project,
) : IssueStrategyFactory<AgentInput, JiraTaskCandidate> {
    private val renderService = project.service<PromptRenderService>()

    override fun createStrategy(): AIAgentGraphStrategy<AgentInput, JiraTaskCandidate> {
        return strategy("jira_issue_generation") {
            val nodePrepareContext by node<AgentInput, String>("prepare_context") { input ->
                thisLogger().debug("Preparing context for LLM prompt")
                renderService.buildPrompt {
                    components(input.components)
                }
            }

            val nodeCallLLM by nodeLLMRequestStructured<JiraTaskCandidate>(
                name = "llm_structured_request",
            )

            val nodeProcessResult by node<Result<StructuredResponse<JiraTaskCandidate>>, JiraTaskCandidate>("process_result") { result ->
                if (result.isSuccess) {
                    val candidate =
                        result.getOrNull()?.data
                            ?: throw IllegalStateException("Success result returned null data")

                    thisLogger().info("Successfully generated structured Jira task")
                    candidate
                } else {
                    val error = result.exceptionOrNull()
                    thisLogger().error("Structured output failed: LLM could not map response to JiraTaskCandidate schema.", error)
                    throw error ?: RuntimeException("Unknown structured parsing error")
                }
            }

            edge(nodeStart forwardTo nodePrepareContext)
            edge(nodePrepareContext forwardTo nodeCallLLM)
            edge(nodeCallLLM forwardTo nodeProcessResult)
            edge(nodeProcessResult forwardTo nodeFinish)
        }
    }
}
