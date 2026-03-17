package com.github.strogolsky.autoissue.services.agent.strategy

import ai.koog.agents.core.agent.entity.AIAgentGraphStrategy
import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.agents.core.dsl.builder.node
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.extension.nodeLLMRequest
import ai.koog.prompt.message.Message
import com.github.strogolsky.autoissue.services.agent.input.AgentInput
import com.github.strogolsky.autoissue.services.agent.output.JiraTaskCandidate
import kotlinx.serialization.json.Json

class JiraIssueStrategyFactory : IssueStrategyFactory<AgentInput, JiraTaskCandidate> {
    private val jsonParser = Json { ignoreUnknownKeys = true }

    override fun createStrategy(): AIAgentGraphStrategy<AgentInput, JiraTaskCandidate> {
        return strategy("jira_issue_generation") {
            val nodePrepareContext by node<AgentInput, String>("prepare_context") { input ->
                input.toPrompt()
            }

            val nodeCallLLM by nodeLLMRequest("llm_request")

            edge(nodeStart forwardTo nodePrepareContext)
            edge(nodePrepareContext forwardTo nodeCallLLM)

            edge(
                nodeCallLLM forwardTo nodeFinish
                    onCondition { it is Message.Assistant }
                    transformed { response ->
                        val rawText = (response as Message.Assistant).content

                        val cleanJson = rawText.removePrefix("```json").removeSuffix("```").trim()

                        jsonParser.decodeFromString<JiraTaskCandidate>(cleanJson)
                    },
            )
        }
    }
}
