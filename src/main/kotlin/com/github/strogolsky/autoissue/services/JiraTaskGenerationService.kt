package com.github.strogolsky.autoissue.services

import com.github.strogolsky.autoissue.agent.JiraIssueAgentFactory
import com.github.strogolsky.autoissue.agent.input.IssueGenerationInput
import com.github.strogolsky.autoissue.agent.output.JiraTaskCandidate
import com.github.strogolsky.autoissue.context.ContextEnvironment
import com.github.strogolsky.autoissue.context.ContextRegistry
import com.github.strogolsky.autoissue.context.TaskInstruction
import com.github.strogolsky.autoissue.context.TestRenderer
import com.github.strogolsky.autoissue.exceptions.TaskGenerationException
import com.github.strogolsky.autoissue.settings.AgentConfigService
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class JiraTaskGenerationService(private val project: Project) {

    private val factory = project.service<JiraIssueAgentFactory>()
    private val registry = project.service<ContextRegistry>()
    private val agentConfigService = project.service<AgentConfigService>()

    suspend fun generateTask(instruction: String, env: ContextEnvironment): JiraTaskCandidate {
        val config = agentConfigService.getEffectiveConfig()
            ?: throw IllegalStateException("API Key or configuration is missing.")

        val contextComponents = registry.gatherAll(env)
        val agent = factory.createAgent(config)

        val renderer = TestRenderer();

        val taskInstruction = TaskInstruction(instruction)
        val input = IssueGenerationInput(taskInstruction, contextComponents, renderer)

        return agent.generate(input) ?: throw TaskGenerationException("Agent returned null")
    }
}