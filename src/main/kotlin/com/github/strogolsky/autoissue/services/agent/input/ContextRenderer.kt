package com.github.strogolsky.autoissue.services.agent.input

interface ContextRenderer {
    fun render(component: ContextComponent): String
}
