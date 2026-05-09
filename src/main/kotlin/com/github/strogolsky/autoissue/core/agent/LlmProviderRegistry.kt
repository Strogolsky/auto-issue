package com.github.strogolsky.autoissue.core.agent

import com.intellij.openapi.components.Service
import com.intellij.openapi.extensions.ExtensionPointName
import org.jetbrains.annotations.TestOnly

/**
 * Registry for LLM provider implementations.
 *
 * Dynamically discovers and registers LLM providers via the extension point system.
 * Providers are looked up by key (case-insensitive).
 *
 * Example providers:
 * - GOOGLE (for Google Gemini)
 * - ANTHROPIC (for Claude)
 * - OPENAI (for GPT models)
 */
@Service(Service.Level.APP)
class LlmProviderRegistry {
    private val providers = mutableMapOf<String, LlmProvider>()

    companion object {
        val EP_NAME: ExtensionPointName<LlmProvider> =
            ExtensionPointName.create("com.github.strogolsky.autoissue.llmProviderFactory")
    }

    /**
     * Initializes the registry by loading providers from the extension point.
     * Used in production.
     */
    constructor() {
        loadExtensions(EP_NAME.extensionList)
    }

    /**
     * Creates registry with specific providers.
     * Used in tests.
     *
     * @param testProviders List of test providers
     */
    @TestOnly
    internal constructor(testProviders: List<LlmProvider>) {
        loadExtensions(testProviders)
    }

    /**
     * Loads providers into the registry map.
     *
     * Provider keys are stored uppercase for case-insensitive lookup.
     *
     * @param extensions List of provider implementations
     */
    private fun loadExtensions(extensions: List<LlmProvider>) {
        extensions.forEach { provider ->
            providers[provider.providerKey.uppercase()] = provider
        }
    }

    /**
     * Gets all registered provider keys.
     *
     * @return Set of provider keys (uppercase)
     */
    fun providers(): Set<String> = providers.keys

    /**
     * Gets a provider by its key.
     *
     * @param key The provider key (case-insensitive)
     * @return The provider instance
     * @throws IllegalArgumentException if provider not found
     */
    fun getProvider(key: String): LlmProvider = providers[key.uppercase()] ?: error("Unknown LLM provider: $key")
}
