package com.github.strogolsky.autoissue.core

import com.github.strogolsky.autoissue.core.agent.JiraIssueAgentFactory
import com.github.strogolsky.autoissue.core.context.ContextEnvironment
import com.github.strogolsky.autoissue.core.context.ContextRegistry
import com.github.strogolsky.autoissue.core.context.components.IssueInstruction
import com.github.strogolsky.autoissue.core.exceptions.IssueGenerationException
import com.github.strogolsky.autoissue.core.input.IssueGenerationInput
import com.github.strogolsky.autoissue.core.output.JiraIssueCandidate
import com.github.strogolsky.autoissue.plugin.config.LlmAgentConfigService
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class JiraIssueGenerationService(private val project: Project) {
    private val factory = project.service<JiraIssueAgentFactory>()
    private val registry = project.service<ContextRegistry>()
    private val agentConfigService = project.service<LlmAgentConfigService>()

    suspend fun generateTask(
        instruction: String,
        env: ContextEnvironment,
    ): JiraIssueCandidate {
        thisLogger().info("Initiating Jira task generation process.")

        val config =
            try {
                agentConfigService.getEffectiveConfig()
            } catch (e: IllegalArgumentException) {
                thisLogger().error("Task generation aborted: ${e.message}")
                throw IssueGenerationException(e.message ?: "LLM configuration is missing")
            }

        thisLogger().debug("Gathering context components from environment...")
        val contextComponents = registry.gatherAll(env)

        thisLogger().debug("Creating AI agent based on current configuration...")
        val agent = factory.createAgent(config)

        val taskInstruction = IssueInstruction(instruction)
        val input = IssueGenerationInput(listOf(taskInstruction) + contextComponents)

        thisLogger().info("Sending prompt to AI agent. Waiting for response...")
        val result = agent.generate(input)
            ?: run {
                thisLogger().error("Task generation failed: AI Agent returned a null result.")
                throw IssueGenerationException("The AI agent returned an empty response. Please check your prompt or API limits.")
            }

        thisLogger().info("Successfully generated task candidate: '${result.title}', '${result.description}'")
        return result
    }
}
