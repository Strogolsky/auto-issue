package com.github.strogolsky.autoissue.agent

import com.github.strogolsky.autoissue.settings.AgentConfig
import com.intellij.remoteDev.tests.AgentContext

interface AgentFactory<T> {
    fun createAgent(config: AgentConfig): T
}