package com.github.strogolsky.autoissue.services.agent

import ai.koog.agents.core.agent.AIAgent

class KoogIssueGenerationAgent<I, O>(
    private val agent: AIAgent<I, O>,
) : IssueGenerationAgent<I, O> {
    override suspend fun generate(input: I): O? {
        return try {
            agent.run(input)
        } catch (e: Exception) {
            null
        }
    }
}
