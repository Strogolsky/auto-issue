package com.github.strogolsky.autoissue.core.agent

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.tools.ToolRegistry
import com.github.strogolsky.autoissue.core.agent.strategy.JiraStrategyRegistry
import com.github.strogolsky.autoissue.core.input.AgentInput
import com.github.strogolsky.autoissue.core.output.JiraTaskCandidate
import com.github.strogolsky.autoissue.plugin.config.AgentConfig
import com.github.strogolsky.autoissue.plugin.startup.PluginConfigLoader
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import kotlin.time.ExperimentalTime

@Service(Service.Level.PROJECT)
class JiraIssueAgentFactory(private val project: Project) :
    AgentFactory<KoogIssueGenerationAgent<AgentInput, JiraTaskCandidate>> {
    @OptIn(ExperimentalTime::class)
    override fun createAgent(config: AgentConfig): KoogIssueGenerationAgent<AgentInput, JiraTaskCandidate> {
        thisLogger().info("Creating Jira Issue Agent...")

        val modelResolver = project.service<ModelProviderResolver>()
        val strategyRegistry = project.service<JiraStrategyRegistry>()
        val pluginConfig = PluginConfigLoader.load()

        val (executor, model) = modelResolver.resolve(config.provider, config.modelName, config.apiKey)

        val strategy = strategyRegistry.getStrategy(pluginConfig.llm.strategyId)

        thisLogger().debug(
            "Agent components resolved. Provider: ${config.provider}, Model: ${config.modelName}, Strategy: ${pluginConfig.llm.strategyId}",
        )

        val toolRegistry = ToolRegistry { }

        val rawKoogAgent =
            AIAgent(
                promptExecutor = executor,
                llmModel = model,
                strategy = strategy,
                systemPrompt = config.systemPrompt,
                temperature = config.temperature,
                maxIterations = config.maxIterations,
                toolRegistry = toolRegistry,
            )

        thisLogger().info("Jira Issue Agent successfully created.")
        return KoogIssueGenerationAgent(rawKoogAgent)
    }
}
