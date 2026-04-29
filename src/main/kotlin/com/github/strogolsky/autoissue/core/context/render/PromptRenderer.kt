package com.github.strogolsky.autoissue.core.context.render

import com.github.strogolsky.autoissue.core.context.components.ContextComponent

interface PromptRenderer {
    fun rendererKey(): String

    fun renderComponent(component: ContextComponent): String

    fun buildPrompt(block: PromptBuilder.() -> Unit): String
}
