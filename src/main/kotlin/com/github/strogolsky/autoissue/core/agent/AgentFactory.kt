package com.github.strogolsky.autoissue.core.agent

import com.github.strogolsky.autoissue.plugin.config.LlmAgentConfig

interface AgentFactory<T> {
    fun createAgent(config: LlmAgentConfig): T
}
