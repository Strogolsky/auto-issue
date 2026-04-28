package com.github.strogolsky.autoissue.plugin.config

import com.github.strogolsky.autoissue.plugin.state.LlmAgentState
import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.generateServiceName
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.diagnostic.thisLogger

@Service(Service.Level.PROJECT)
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

    fun updateSettings(
        newState: LlmAgentState,
        newKey: String?,
    ) {
        thisLogger().info("Updating Agent settings. Provider: ${newState.provider}, Model: ${newState.modelName}")

        state = newState
        newKey?.let { PasswordSafe.instance.setPassword(tokenKey, it) }
    }

    fun getApiKey(): String? = PasswordSafe.instance.getPassword(tokenKey)

    fun saveApiKey(key: String) {
        PasswordSafe.instance.setPassword(tokenKey, key)
    }

    fun applyDefaults(defaults: LlmDefaults) {
        if (state.provider.isBlank()) state.provider = defaults.provider
        if (state.modelName.isBlank()) state.modelName = defaults.modelName
        if (state.strategyId.isBlank()) state.strategyId = defaults.strategyId
        if (state.temperature == 0.0) state.temperature = defaults.temperature
        if (state.maxIterations == 0) state.maxIterations = defaults.maxIterations
        if (state.systemPrompt.isBlank()) state.systemPrompt = defaults.systemPrompt
    }

    fun getEffectiveConfig(): LlmAgentConfig {
        val apiKey = PasswordSafe.instance.getPassword(tokenKey)

        if (apiKey.isNullOrBlank()) {
            throw IllegalArgumentException("LLM API Key is missing in PasswordSafe")
        }
        return LlmAgentConfig(
            apiKey = apiKey,
            provider = state.provider,
            modelName = state.modelName,
            systemPrompt = state.systemPrompt,
            temperature = state.temperature,
            maxIterations = state.maxIterations,
            strategyId = state.strategyId,
        )
    }
}
