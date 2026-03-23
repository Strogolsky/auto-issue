package com.github.strogolsky.autoissue.agent

import com.github.strogolsky.autoissue.settings.AgentConfig
interface AgentFactory<T> {
    fun createAgent(config: AgentConfig): T
}