package com.github.strogolsky.autoissue.agent.context

import com.github.strogolsky.autoissue.agent.context.components.ContextComponent

interface RendererFactory {
    fun renderComponent(component: ContextComponent): String

    fun buildPrompt(block: PromptBuilder.() -> Unit): String
}
