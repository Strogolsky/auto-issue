package com.github.strogolsky.autoissue.context

interface ContextRenderer {
    fun render(component: ContextComponent): String
}
