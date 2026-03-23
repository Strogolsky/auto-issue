package com.github.strogolsky.autoissue.agent.strategy

import ai.koog.agents.core.agent.entity.AIAgentGraphStrategy
import ai.koog.agents.core.dsl.builder.node
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.extension.nodeLLMRequest
import ai.koog.agents.core.dsl.extension.nodeLLMRequestStructured
import ai.koog.prompt.message.Message
import ai.koog.prompt.structure.StructuredResponse
import com.github.strogolsky.autoissue.agent.input.AgentInput
import com.github.strogolsky.autoissue.agent.output.JiraTaskCandidate
import com.intellij.openapi.diagnostic.thisLogger
import kotlinx.serialization.json.Json

class JiraIssueStrategyFactory : IssueStrategyFactory<AgentInput, JiraTaskCandidate> {

    override fun createStrategy(): AIAgentGraphStrategy<AgentInput, JiraTaskCandidate> {
        return strategy("jira_issue_generation") {
            val nodePrepareContext by node<AgentInput, String>("prepare_context") { input ->
                thisLogger().debug("Preparing context for LLM prompt")
                input.toPrompt()
            }

            val nodeCallLLM by nodeLLMRequestStructured<JiraTaskCandidate>(
                name = "llm_structured_request"
            )

            val nodeProcessResult by node<Result<StructuredResponse<JiraTaskCandidate>>, JiraTaskCandidate>("process_result") { result ->
                if (result.isSuccess) {
                    val candidate = result.getOrNull()?.data
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
