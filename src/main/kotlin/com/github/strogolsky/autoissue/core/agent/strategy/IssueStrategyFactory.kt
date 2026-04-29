package com.github.strogolsky.autoissue.core.agent.strategy

import ai.koog.agents.core.agent.entity.AIAgentGraphStrategy

interface IssueStrategyFactory<I, O> {
    fun createStrategy(): AIAgentGraphStrategy<I, O>
}
