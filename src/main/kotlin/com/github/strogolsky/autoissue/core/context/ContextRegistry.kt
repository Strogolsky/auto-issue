package com.github.strogolsky.autoissue.core.context

import com.github.strogolsky.autoissue.core.context.components.ContextComponent
import com.github.strogolsky.autoissue.core.context.providers.ContextComponentProvider
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.extensions.ExtensionPointName
import org.jetbrains.annotations.TestOnly
import java.util.concurrent.CopyOnWriteArrayList

@Service(Service.Level.PROJECT)
class ContextRegistry {
    private val providers = CopyOnWriteArrayList<ContextComponentProvider>()

    companion object {
        val EP_NAME: ExtensionPointName<ContextComponentProvider> =
            ExtensionPointName.create("com.github.strogolsky.autoissue.contextComponentProvider")
    }

    init {
        EP_NAME.extensionList.forEach { providers.add(it) }
    }

    fun register(provider: ContextComponentProvider) {
        providers.add(provider)
        thisLogger().debug("Registered ContextComponentProvider: ${provider.javaClass.simpleName}")
    }

    suspend fun gatherAll(env: ContextEnvironment): List<ContextComponent> {
        thisLogger().info("Starting to gather context from ${providers.size} providers...")

        val components =
            providers.mapNotNull { provider ->
                try {
                    thisLogger().debug("Executing provider: ${provider.javaClass.simpleName}")
                    provider.provide(env)
                } catch (e: Exception) {
                    thisLogger().warn("Failed to gather context from ${provider.javaClass.simpleName}. Skipping component.", e)
                    null
                }
            }

        thisLogger().info("Successfully gathered ${components.size} context components.")
        return components
    }
}
