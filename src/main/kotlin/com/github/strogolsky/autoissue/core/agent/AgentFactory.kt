package com.github.strogolsky.autoissue.core.agent

import com.github.strogolsky.autoissue.plugin.config.AgentConfig

interface AgentFactory<T> {
    fun createAgent(config: AgentConfig): T
}
