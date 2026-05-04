package com.github.strogolsky.autoissue.core.agent

import com.intellij.openapi.components.Service
import com.intellij.openapi.extensions.ExtensionPointName
import org.jetbrains.annotations.TestOnly

@Service(Service.Level.APP)
class LlmProviderRegistry {
    private val providers = mutableMapOf<String, LlmProvider>()

    companion object {
        val EP_NAME: ExtensionPointName<LlmProvider> =
            ExtensionPointName.create("com.github.strogolsky.autoissue.llmProviderFactory")
    }

    constructor() {
        loadExtensions(EP_NAME.extensionList)
    }

    @TestOnly
    internal constructor(testProviders: List<LlmProvider>) {
        loadExtensions(testProviders)
    }

    private fun loadExtensions(extensions: List<LlmProvider>) {
        extensions.forEach { providers[it.providerKey.uppercase()] = it }
    }

    fun providers(): Set<String> = providers.keys

    fun getProvider(key: String): LlmProvider = providers[key.uppercase()] ?: error("Unknown LLM provider: $key")
}
