package com.github.strogolsky.autoissue.context

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger

@Service(Service.Level.PROJECT)
class ContextRegistry {
    private val providers = mutableListOf<ContextComponentProvider>()

    fun register(provider: ContextComponentProvider) {
        providers.add(provider)
    }

    suspend fun gatherAll(env: ContextEnvironment): List<ContextComponent> {
        return providers.mapNotNull { provider ->
            try {
                provider.provide(env)
            } catch (e: Exception) {
                thisLogger().warn("Failed to gather context from ${provider.javaClass.simpleName}", e)
                null
            }
        }
    }
}