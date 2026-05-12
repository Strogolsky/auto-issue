package com.github.strogolsky.autoissue.core.context.render

import com.intellij.openapi.components.Service
import com.intellij.openapi.extensions.ExtensionPointName

/**
 * Registry for discovering and resolving PromptRenderer implementations.
 *
 * Discovers renderers via the IntelliJ extension point system. Renderers are registered
 * by plugin developers and loaded at startup, then accessed by key when needed.
 *
 * Built-in renderers:
 * - "XML" → XmlPromptRenderer
 * - "MARKDOWN" → MarkdownPromptRenderer
 * - "SIMPLE" → PlainTextPromptRenderer
 *
 * Project-level service (separate instance per open project).
 */
@Service(Service.Level.PROJECT)
class PromptRendererRegistry {
    private val renderers = mutableMapOf<String, PromptRenderer>()

    companion object {
        // Extension point for plugin developers to register custom PromptRenderer implementations
        val EP_NAME: ExtensionPointName<PromptRenderer> =
            ExtensionPointName.create("com.github.strogolsky.autoissue.promptRenderer")
    }

    init {
        // Load all registered renderers from the extension point at startup.
        // EP may be absent in lightweight test environments where the plugin descriptor isn't loaded.
        try {
            EP_NAME.extensionList.forEach { renderers[it.rendererKey()] = it }
        } catch (_: IllegalArgumentException) {
        }
    }

    /**
     * Registers a prompt renderer in the registry.
     * Used by AutoIssueSetupTool to add renderers, or by plugins to register custom renderers.
     *
     * @param renderer The renderer to register (must have a unique rendererKey())
     */
    fun register(renderer: PromptRenderer) {
        renderers[renderer.rendererKey()] = renderer
    }

    /**
     * Resolves a renderer by its key.
     * Returns the registered renderer, or throws an error if not found.
     *
     * @param key The renderer key (e.g., "XML", "MARKDOWN", "SIMPLE")
     * @return The PromptRenderer with the given key
     * @throws IllegalStateException If no renderer is registered for the key
     */
    fun resolve(key: String): PromptRenderer = renderers[key] ?: error("AutoIssue: no PromptRenderer registered for key: $key")
}
