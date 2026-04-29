package com.github.strogolsky.autoissue.core.context.render

import com.intellij.openapi.components.Service
import com.intellij.openapi.extensions.ExtensionPointName

@Service(Service.Level.PROJECT)
class PromptRendererRegistry {
    private val renderers = mutableMapOf<String, PromptRenderer>()

    companion object {
        val EP_NAME: ExtensionPointName<PromptRenderer> =
            ExtensionPointName.create("com.github.strogolsky.autoissue.promptRenderer")
    }

    init {
        EP_NAME.extensionList.forEach { renderers[it.rendererKey()] = it }
    }

    fun register(renderer: PromptRenderer) {
        renderers[renderer.rendererKey()] = renderer
    }

    fun resolve(key: String): PromptRenderer = renderers[key] ?: error("AutoIssue: no PromptRenderer registered for key: $key")
}
