package com.github.strogolsky.autoissue.core.context

import com.github.strogolsky.autoissue.core.context.components.ContextComponent
import com.github.strogolsky.autoissue.core.context.providers.ContextComponentProvider
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.extensions.ExtensionPointName
import org.jetbrains.annotations.TestOnly
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Registry for context component providers.
 *
 * This service discovers and manages all context providers that contribute information
 * to the AI agent (project structure, JIRA metadata, file content, etc.).
 *
 * Providers are loaded from the extension point at initialization.
 * Context gathering is resilient: failures in one provider don't affect others.
 */
@Service(Service.Level.PROJECT)
class ContextRegistry {
    private val providers = CopyOnWriteArrayList<ContextComponentProvider>()

    companion object {
        val EP_NAME: ExtensionPointName<ContextComponentProvider> =
            ExtensionPointName.create("com.github.strogolsky.autoissue.contextComponentProvider")
    }

    /**
     * Initializes the registry by loading providers from the extension point.
     * Used in production to dynamically discover all registered providers.
     */
    constructor() {
        EP_NAME.extensionList.forEach { providers.add(it) }
    }

    /**
     * Creates registry with a specific set of providers.
     * Used in tests to inject mock/stub providers.
     *
     * @param testProviders List of test providers
     */
    @TestOnly
    internal constructor(testProviders: List<ContextComponentProvider>) {
        providers.addAll(testProviders)
        thisLogger().debug("ContextRegistry initialized for testing with ${testProviders.size} test providers")
    }

    /**
     * Gathers context components from all registered providers.
     *
     * This method calls each provider to collect information about the project context.
     * If a provider fails, it's skipped and a warning is logged, but the process continues.
     *
     * Context includes:
     * - Project structure and file information
     * - JIRA project metadata (issue types, fields, etc.)
     * - User instructions
     * - Code context around the TODO location
     *
     * @param env The context environment containing project and code location info
     * @return List of gathered context components from all successful providers
     */
    suspend fun gatherAll(env: ContextEnvironment): List<ContextComponent> {
        thisLogger().info("Starting to gather context from ${providers.size} providers...")

        val components =
            providers.mapNotNull { provider ->
                try {
                    thisLogger().debug("Executing provider: ${provider.javaClass.simpleName}")
                    val component = provider.provide(env)
                    if (component != null) {
                        thisLogger().debug("Provider ${provider.javaClass.simpleName} returned: ${component.javaClass.simpleName}")
                    }
                    component
                } catch (e: Exception) {
                    thisLogger().warn(
                        "Failed to gather context from ${provider.javaClass.simpleName}. Skipping component.",
                        e,
                    )
                    null
                }
            }

        thisLogger().info("Successfully gathered ${components.size} context components from ${providers.size} providers")
        return components
    }
}
