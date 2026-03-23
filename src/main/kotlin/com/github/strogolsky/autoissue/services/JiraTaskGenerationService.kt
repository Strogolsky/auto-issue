package com.github.strogolsky.autoissue.services

import com.github.strogolsky.autoissue.agent.JiraIssueAgentFactory
import com.github.strogolsky.autoissue.agent.input.IssueGenerationInput
import com.github.strogolsky.autoissue.agent.output.JiraTaskCandidate
import com.github.strogolsky.autoissue.context.ContextEnvironment
import com.github.strogolsky.autoissue.context.ContextRegistry
import com.github.strogolsky.autoissue.context.TaskInstruction
import com.github.strogolsky.autoissue.context.SimpleRenderer
import com.github.strogolsky.autoissue.exceptions.TaskGenerationException
import com.github.strogolsky.autoissue.settings.AgentConfigService
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class JiraTaskGenerationService(private val project: Project) {

    private val factory = project.service<JiraIssueAgentFactory>()
    private val registry = project.service<ContextRegistry>()
    private val agentConfigService = project.service<AgentConfigService>()

    suspend fun generateTask(instruction: String, env: ContextEnvironment): JiraTaskCandidate {
        thisLogger().info("Initiating Jira task generation process.")

        val config = agentConfigService.getEffectiveConfig()
            ?: run {
                thisLogger().error("Task generation aborted: API Key or configuration is missing in settings.")
                throw IllegalStateException("API Key or configuration is missing.")
            }

        thisLogger().debug("Gathering context components from environment...")
        val contextComponents = registry.gatherAll(env)

        thisLogger().debug("Creating AI agent based on current configuration...")
        val agent = factory.createAgent(config)

        val renderer = SimpleRenderer();

        val taskInstruction = TaskInstruction(instruction)
        val input = IssueGenerationInput(taskInstruction, contextComponents, renderer)

        thisLogger().info("Sending prompt to AI agent. Waiting for response...")
        val result = agent.generate(input)
            ?: run {
                thisLogger().error("Task generation failed: AI Agent returned a null result.")
                throw TaskGenerationException("Agent returned null")
            }

        thisLogger().info("Successfully generated task candidate: '${result.title}', '${result.description}'")
        return result
    }
}