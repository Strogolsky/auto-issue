package com.github.strogolsky.autoissue.core.input

import com.github.strogolsky.autoissue.core.context.components.ContextComponent

data class AgentInput(
    val components: List<ContextComponent>,
)
