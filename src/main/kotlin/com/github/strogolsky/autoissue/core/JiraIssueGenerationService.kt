package com.github.strogolsky.autoissue.core

import com.github.strogolsky.autoissue.core.agent.JiraIssueAgentFactory
import com.github.strogolsky.autoissue.core.context.ContextEnvironment
import com.github.strogolsky.autoissue.core.context.ContextRegistry
import com.github.strogolsky.autoissue.core.context.components.TaskInstruction
import com.github.strogolsky.autoissue.core.input.AgentInput
import com.github.strogolsky.autoissue.core.output.JiraTaskCandidate
import com.github.strogolsky.autoissue.core.exceptions.TaskGenerationException
import com.github.strogolsky.autoissue.plugin.config.AgentConfigService
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class JiraIssueGenerationService(private val project: Project) {
    private val factory = project.service<JiraIssueAgentFactory>()
    private val registry = project.service<ContextRegistry>()
    private val agentConfigService = project.service<AgentConfigService>()

    suspend fun generateTask(
        instruction: String,
        env: ContextEnvironment,
    ): JiraTaskCandidate {
        thisLogger().info("Initiating Jira task generation process.")

        val config =
            agentConfigService.getEffectiveConfig()
                ?: run {
                    thisLogger().error("Task generation aborted: API Key or configuration is missing in settings.")
                    throw IllegalStateException("API Key or configuration is missing.")
                }

        thisLogger().debug("Gathering context components from environment...")
        val contextComponents = registry.gatherAll(env)

        thisLogger().debug("Creating AI agent based on current configuration...")
        val agent = factory.createAgent(config)

        val taskInstruction = TaskInstruction(instruction)
        val input = AgentInput(listOf(taskInstruction) + contextComponents)

        thisLogger().info("Sending prompt to AI agent. Waiting for response...")
        val result =
            agent.generate(input)
                ?: run {
                    thisLogger().error("Task generation failed: AI Agent returned a null result.")
                    throw TaskGenerationException("Agent returned null")
                }

        thisLogger().info("Successfully generated task candidate: '${result.title}', '${result.description}'")
        return result
    }
}