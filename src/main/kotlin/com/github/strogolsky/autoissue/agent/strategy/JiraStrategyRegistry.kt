package com.github.strogolsky.autoissue.agent.strategy

import ai.koog.agents.core.agent.entity.AIAgentGraphStrategy
import com.github.strogolsky.autoissue.agent.input.AgentInput
import com.github.strogolsky.autoissue.agent.output.JiraTaskCandidate
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import java.util.concurrent.ConcurrentHashMap

@Service(Service.Level.PROJECT)
class JiraStrategyRegistry {

    private val strategies = ConcurrentHashMap<String, () -> AIAgentGraphStrategy<AgentInput, JiraTaskCandidate>>()

    init {
        register("prod-jira-strategy") { JiraIssueStrategyFactory().createStrategy() }
        thisLogger().info("JiraStrategyRegistry initialized with default strategies.")
    }

    fun register(id: String, factory: () -> AIAgentGraphStrategy<AgentInput, JiraTaskCandidate>) {
        strategies[id] = factory
        thisLogger().debug("Registered new strategy with id: '$id'")
    }

    fun getStrategy(id: String): AIAgentGraphStrategy<AgentInput, JiraTaskCandidate> {
        val factory = strategies[id]
            ?: run {
                thisLogger().error("Strategy not found for id: '$id'. Available: ${strategies.keys}")
                throw IllegalArgumentException("Strategy not found for id: '$id'. Available: ${strategies.keys}")
            }
        thisLogger().debug("Instantiating strategy: '$id'")
        return factory.invoke()
    }
}