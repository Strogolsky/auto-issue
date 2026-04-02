package com.github.strogolsky.autoissue.agent.context

import com.github.strogolsky.autoissue.agent.context.components.ContextComponent

interface ContextRenderer {
    fun render(component: ContextComponent): String
}
