package com.github.strogolsky.autoissue.agent

import ai.koog.agents.core.agent.AIAgent
import com.github.strogolsky.autoissue.agent.strategy.JiraStrategyRegistry
import com.github.strogolsky.autoissue.settings.AgentConfig
import com.github.strogolsky.autoissue.agent.input.AgentInput
import com.github.strogolsky.autoissue.agent.output.JiraTaskCandidate
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import kotlin.time.ExperimentalTime

@Service(Service.Level.PROJECT)
class JiraIssueAgentFactory(private val project: Project) :
    AgentFactory<KoogIssueGenerationAgent<AgentInput, JiraTaskCandidate>> {

    @OptIn(ExperimentalTime::class)
    override fun createAgent(config: AgentConfig): KoogIssueGenerationAgent<AgentInput, JiraTaskCandidate> {

        val modelResolver = project.service<ModelProviderResolver>()
        val strategyRegistry = project.service<JiraStrategyRegistry>()

        val (executor, model) = modelResolver.resolve(config.provider, config.modelName, config.apiKey)

        val strategy = strategyRegistry.getStrategy(config.strategyId)

        val rawKoogAgent = AIAgent(
            promptExecutor = executor,
            llmModel = model,
            strategy = strategy,
            systemPrompt = config.systemPrompt,
            temperature = config.temperature,
            maxIterations = config.maxIterations
        )

        return KoogIssueGenerationAgent(rawKoogAgent)
    }
}