package com.github.strogolsky.autoissue.agent.context

import com.github.strogolsky.autoissue.agent.context.components.ContextComponent

interface RendererFactory : ContextRenderer {
    fun renderAll(components: List<ContextComponent>): String = components.joinToString(separator = "\n\n") { render(it) }
}
