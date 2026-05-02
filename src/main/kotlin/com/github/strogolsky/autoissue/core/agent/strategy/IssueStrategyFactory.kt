package com.github.strogolsky.autoissue.core.agent.strategy

import ai.koog.agents.core.agent.entity.AIAgentGraphStrategy
import com.intellij.openapi.project.Project

interface IssueStrategyFactory<I, O> {
    val providerKey: String
    val id: String
    val displayName: String

    fun createStrategy(project: Project): AIAgentGraphStrategy<I, O>
}
