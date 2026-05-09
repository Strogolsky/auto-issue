package com.github.strogolsky.autoissue.plugin.config

import com.github.strogolsky.autoissue.core.agent.strategy.JiraStrategyRegistry
import com.github.strogolsky.autoissue.plugin.state.LlmAgentState
import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.generateServiceName
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger

/**
 * Manages LLM (Large Language Model) agent configuration.
 *
 * Stores and retrieves:
 * - LLM Provider (Google, Anthropic, etc.)
 * - API Key (securely via IDE credential store)
 * - Generation Strategy (direct, reasoning, etc.)
 * - Temperature and max iterations
 * - System prompt
 *
 * This service persists settings in AutoIssuePlugin.xml and uses the IDE's
 * PasswordSafe to securely store the API key.
 */
@Service(Service.Level.APP)
@State(
    name = "JiraAgentConfiguration",
    storages = [Storage("AutoIssuePlugin.xml")],
)
class LlmAgentConfigService : PersistentStateComponent<LlmAgentState> {
    private var state = LlmAgentState()
    private val tokenKey = CredentialAttributes(generateServiceName("AutoIssue", "LlmApiKey"))

    override fun getState() = state

    override fun loadState(s: LlmAgentState) {
        state = s
    }

    /**
     * Updates LLM agent configuration and optionally updates the API key.
     *
     * Ensures the strategy is valid for the selected provider.
     *
     * @param newState The new configuration state (provider, strategy, temperature, etc.)
     * @param newKey Optional new API key to store
     */
    fun updateSettings(
        newState: LlmAgentState,
        newKey: String?,
    ) {
        thisLogger().info("Updating LLM Agent settings. Provider: ${newState.provider}, Strategy: ${newState.strategyId}")

        state = newState
        newKey?.let { PasswordSafe.instance.setPassword(tokenKey, it) }
        ensureStrategyValid()
    }

    /**
     * Retrieves the stored LLM API key from IDE credential store.
     *
     * @return API key string, or null if not set
     */
    fun getApiKey(): String? = PasswordSafe.instance.getPassword(tokenKey)

    /**
     * Saves the LLM API key to IDE credential store.
     *
     * @param key The API key to store securely
     */
    fun saveApiKey(key: String) {
        thisLogger().debug("Saving LLM API key to credential store")
        PasswordSafe.instance.setPassword(tokenKey, key)
    }

    /**
     * Applies default values to empty configuration fields.
     *
     * Used during first-time setup to populate configuration with sensible defaults
     * if the user hasn't customized settings yet.
     *
     * @param defaults The default values to apply
     */
    fun applyDefaults(defaults: LlmDefaults) {
        if (state.provider.isBlank()) state.provider = defaults.provider
        if (state.strategyId.isBlank()) state.strategyId = defaults.strategyId
        if (state.temperature == 0.0) state.temperature = defaults.temperature
        if (state.maxIterations == 0) state.maxIterations = defaults.maxIterations
        if (state.systemPrompt.isBlank()) state.systemPrompt = defaults.systemPrompt

        ensureStrategyValid()
        thisLogger().debug("Default LLM configuration applied: provider=${state.provider}, strategy=${state.strategyId}")
    }

    /**
     * Checks if LLM configuration is complete and ready for use.
     *
     * Only checks for API key presence. Other configuration uses defaults if not set.
     *
     * @return true if API key is configured
     */
    fun isReady(): Boolean {
        val apiKey = PasswordSafe.instance.getPassword(tokenKey)
        return !apiKey.isNullOrBlank()
    }

    /**
     * Gets complete LLM agent configuration for AI agent creation.
     *
     * Validates that API key is present.
     *
     * @return LlmAgentConfig with all fields populated (using defaults for missing values)
     * @throws IllegalArgumentException if API key is missing
     */
    fun getEffectiveConfig(): LlmAgentConfig {
        val apiKey = PasswordSafe.instance.getPassword(tokenKey)
        require(!apiKey.isNullOrBlank()) { "LLM API Key is missing in PasswordSafe" }

        thisLogger().debug("LLM configuration resolved: provider=${state.provider}, strategy=${state.strategyId}, temperature=${state.temperature}")

        return LlmAgentConfig(
            apiKey = apiKey,
            provider = state.provider,
            systemPrompt = state.systemPrompt,
            temperature = state.temperature,
            maxIterations = state.maxIterations,
            strategyId = state.strategyId,
        )
    }

    /**
     * Validates and corrects the current strategy.
     *
     * If the strategy is not valid for the current provider, falls back to
     * the first available strategy for that provider.
     */
    private fun ensureStrategyValid() {
        val strategyRegistry = ApplicationManager.getApplication().service<JiraStrategyRegistry>()
        val isValid =
            state.strategyId.isNotBlank() &&
                strategyRegistry.findFactory(state.provider, state.strategyId) != null
        if (!isValid) {
            val fallback = strategyRegistry.strategiesFor(state.provider).firstOrNull()
            if (fallback != null) {
                thisLogger().warn("Strategy ${state.strategyId} not found for provider ${state.provider}. Falling back to ${fallback.id}")
                state.strategyId = fallback.id
            }
        }
    }
}
