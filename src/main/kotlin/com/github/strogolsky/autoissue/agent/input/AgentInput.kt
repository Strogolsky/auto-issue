package com.github.strogolsky.autoissue.agent.input

import com.github.strogolsky.autoissue.agent.context.components.ContextComponent

data class AgentInput(
    val components: List<ContextComponent>,
)
