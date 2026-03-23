package com.github.strogolsky.autoissue.context

import aws.smithy.kotlin.runtime.telemetry.logging.debug
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import java.util.concurrent.CopyOnWriteArrayList

@Service(Service.Level.PROJECT)
class ContextRegistry {
    private val providers = CopyOnWriteArrayList<ContextComponentProvider>()

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
