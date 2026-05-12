package com.github.strogolsky.autoissue.core

import com.github.strogolsky.autoissue.core.agent.JiraIssueAgentFactory
import com.github.strogolsky.autoissue.core.context.ContextEnvironment
import com.github.strogolsky.autoissue.core.context.ContextRegistry
import com.github.strogolsky.autoissue.core.context.components.IssueInstruction
import com.github.strogolsky.autoissue.core.exceptions.IssueGenerationException
import com.github.strogolsky.autoissue.core.input.IssueGenerationInput
import com.github.strogolsky.autoissue.core.output.JiraIssueCandidate
import com.github.strogolsky.autoissue.plugin.config.LlmAgentConfigService
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project

/**
 * Core service for generating JIRA issues using AI agents.
 *
 * Orchestrates the issue generation pipeline by:
 * 1. Validating LLM configuration
 * 2. Gathering context components from the project
 * 3. Creating an AI agent with the specified strategy
 * 4. Sending the instruction to the agent for processing
 * 5. Returning the generated issue candidate
 *
 * This is a project-level service that handles the AI-driven issue generation logic.
 */
@Service(Service.Level.PROJECT)
class JiraIssueGenerationService(private val project: Project) {
    private val factory = project.service<JiraIssueAgentFactory>()
    private val registry = project.service<ContextRegistry>()
    private val agentConfigService = ApplicationManager.getApplication().service<LlmAgentConfigService>()

    /**
     * Generates a JIRA issue candidate using an AI agent.
     *
     * This method:
     * 1. Validates LLM configuration is available
     * 2. Gathers context components from the environment (code, JIRA metadata, etc.)
     * 3. Creates an AI agent based on the configured strategy
     * 4. Sends the instruction with context to the agent
     * 5. Returns the generated issue candidate
     *
     * @param instruction The user's issue description or request
     * @param env The context environment containing project, file, and code information
     * @return A JiraIssueCandidate with generated title, description, and other fields
     * @throws IssueGenerationException if LLM configuration is missing or invalid
     */
    suspend fun generate(
        instruction: String,
        env: ContextEnvironment,
    ): JiraIssueCandidate {
        thisLogger().info("Initiating Jira task generation process.")

        val config =
            try {
                agentConfigService.getEffectiveConfig()
            } catch (e: IllegalArgumentException) {
                thisLogger().warn("Task generation aborted: LLM configuration missing or invalid - ${e.message}")
                throw IssueGenerationException(e.message ?: "LLM configuration is missing")
            }

        thisLogger().debug("Gathering context components from environment for instruction: '$instruction'")
        val contextComponents = registry.gatherAll(env)
        thisLogger().debug("Gathered ${contextComponents.size} context components")

        thisLogger().debug("Creating AI agent with provider: ${config.provider}, strategy: ${config.strategyId}")
        val agent = factory.create(config)

        val taskInstruction = IssueInstruction(instruction)
        val input = IssueGenerationInput(listOf(taskInstruction) + contextComponents)

        thisLogger().info("Sending prompt to AI agent. Waiting for response...")
        val result = agent.generate(input)

        thisLogger().info("Successfully generated task candidate: title='${result.title}', description='${result.description}'")
        return result
    }
}
