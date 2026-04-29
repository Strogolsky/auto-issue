package com.github.strogolsky.autoissue.core.context.render

import com.github.strogolsky.autoissue.core.context.components.ContextComponent

interface PromptBuilder {
    fun instruction(text: String)

    fun section(
        title: String,
        content: String,
    )

    fun components(components: List<ContextComponent>)
}
