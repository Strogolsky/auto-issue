package com.github.strogolsky.autoissue.agent.context

import com.github.strogolsky.autoissue.agent.context.components.ContextComponent

interface PromptBuilder {
    fun instruction(text: String)

    fun section(
        title: String,
        content: String,
    )

    fun components(components: List<ContextComponent>)
}
