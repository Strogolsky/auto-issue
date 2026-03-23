package com.github.strogolsky.autoissue.agent.strategy

import ai.koog.agents.core.agent.entity.AIAgentGraphStrategy
import com.github.strogolsky.autoissue.agent.input.AgentInput
import com.github.strogolsky.autoissue.agent.output.JiraTaskCandidate
import com.intellij.openapi.components.Service

@Service(Service.Level.PROJECT)
class JiraStrategyRegistry {

    private val strategies = mutableMapOf<String, () -> AIAgentGraphStrategy<AgentInput, JiraTaskCandidate>>()

    init {

    }

    fun register(id: String, factory: () -> AIAgentGraphStrategy<AgentInput, JiraTaskCandidate>) {
        strategies[id] = factory
    }

    fun getStrategy(id: String): AIAgentGraphStrategy<AgentInput, JiraTaskCandidate> {
        val factory = strategies[id]
            ?: throw IllegalArgumentException("Strategy not found for id: '$id'. Available: ${strategies.keys}")
        return factory.invoke()
    }
}