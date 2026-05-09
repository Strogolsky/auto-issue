package com.github.strogolsky.autoissue.core.agent

import com.github.strogolsky.autoissue.plugin.config.LlmAgentConfig

/**
 * Generic factory interface for creating AI agents.
 *
 * Implementations of this interface are responsible for instantiating agents
 * with the appropriate configuration, LLM providers, and strategies.
 *
 * @param T The type of agent this factory creates
 */
interface AgentFactory<T> {
    /**
     * Creates an AI agent instance with the given configuration.
     *
     * @param config The LLM agent configuration containing provider, strategy, and parameters
     * @return A new agent instance configured and ready for use
     */
    fun createAgent(config: LlmAgentConfig): T
}
