package com.github.strogolsky.autoissue.core.agent.strategy

import ai.koog.agents.core.agent.entity.AIAgentGraphStrategy
import com.github.strogolsky.autoissue.core.input.IssueGenerationInput
import com.github.strogolsky.autoissue.core.output.JiraIssueCandidate
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import java.util.concurrent.ConcurrentHashMap

@Service(Service.Level.PROJECT)
class JiraStrategyRegistry(private val project: Project) {
    private val strategies = ConcurrentHashMap<String, () -> AIAgentGraphStrategy<IssueGenerationInput, JiraIssueCandidate>>()

    init {
        register("jira-direct-strategy") { JiraDirectStrategyFactory(project).createStrategy() }
        register("jira-reasoning-strategy") { JiraReasoningStrategyFactory(project).createStrategy() }
        thisLogger().info("JiraStrategyRegistry initialized with default strategies.")
    }

    fun register(
        id: String,
        factory: () -> AIAgentGraphStrategy<IssueGenerationInput, JiraIssueCandidate>,
    ) {
        strategies[id] = factory
        thisLogger().debug("Registered new strategy with id: '$id'")
    }

    fun getStrategy(id: String): AIAgentGraphStrategy<IssueGenerationInput, JiraIssueCandidate> {
        val factory =
            strategies[id]
                ?: run {
                    thisLogger().error("Strategy not found for id: '$id'. Available: ${strategies.keys}")
                    error("Strategy not found for id: '$id'. Available: ${strategies.keys}")
                }
        thisLogger().debug("Instantiating strategy: '$id'")
        return factory.invoke()
    }
}
