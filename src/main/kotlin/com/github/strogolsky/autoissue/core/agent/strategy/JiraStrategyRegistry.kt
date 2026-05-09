package com.github.strogolsky.autoissue.core.agent.strategy

import com.github.strogolsky.autoissue.core.input.IssueGenerationInput
import com.github.strogolsky.autoissue.core.output.JiraIssueCandidate
import com.intellij.openapi.components.Service
import com.intellij.openapi.extensions.ExtensionPointName
import org.jetbrains.annotations.TestOnly

/**
 * Registry for issue generation strategies.
 *
 * Dynamically discovers and registers strategies for different LLM providers.
 * Allows agents to use different generation approaches based on configuration.
 */
@Service(Service.Level.APP)
class JiraStrategyRegistry {
    private val strategies: List<IssueStrategyFactory<*, *>>

    companion object {
        val EP_NAME: ExtensionPointName<IssueStrategyFactory<*, *>> =
            ExtensionPointName.create("com.github.strogolsky.autoissue.issueStrategyFactory")
    }

    /**
     * Initializes registry by loading strategies from extension points.
     * Used in production.
     */
    constructor() {
        this.strategies = EP_NAME.extensionList
    }

    /**
     * Initializes registry with specific strategies.
     * Used in tests.
     *
     * @param testStrategies List of test strategies
     */
    @TestOnly
    internal constructor(testStrategies: List<IssueStrategyFactory<*, *>>) {
        this.strategies = testStrategies
    }

    /**
     * Gets all strategies for a specific LLM provider.
     *
     * @param providerKey The provider key (case-insensitive)
     * @return List of available strategies for this provider
     */
    @Suppress("UNCHECKED_CAST")
    fun strategiesFor(providerKey: String): List<IssueStrategyFactory<IssueGenerationInput, JiraIssueCandidate>> =
        strategies
            .filter { it.providerKey.equals(providerKey, ignoreCase = true) }
            .map { it as IssueStrategyFactory<IssueGenerationInput, JiraIssueCandidate> }

    /**
     * Finds a specific strategy for a provider.
     *
     * @param providerKey The LLM provider key
     * @param strategyId The strategy ID
     * @return The strategy factory, or null if not found
     */
    fun findFactory(
        providerKey: String,
        strategyId: String,
    ): IssueStrategyFactory<IssueGenerationInput, JiraIssueCandidate>? = strategiesFor(providerKey).find { it.id == strategyId }
}
