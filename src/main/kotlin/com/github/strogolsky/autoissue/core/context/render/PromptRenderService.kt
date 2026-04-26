package com.github.strogolsky.autoissue.core.context.render

import com.intellij.openapi.components.Service

@Service(Service.Level.PROJECT)
class PromptRenderService {
    private var factory: RendererFactory? = null

    fun initialize(rendererFactory: RendererFactory) {
        this.factory = rendererFactory
    }

    fun buildPrompt(block: PromptBuilder.() -> Unit): String {
        val currentFactory =
            factory
                ?: throw IllegalStateException("PromptRenderService was not initialized by StartupActivity")
        return currentFactory.buildPrompt(block)
    }
}