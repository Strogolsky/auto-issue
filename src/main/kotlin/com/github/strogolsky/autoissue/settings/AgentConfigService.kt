package com.github.strogolsky.autoissue.settings

import com.github.strogolsky.autoissue.config.LlmDefaults
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
class AgentConfigService : PersistentStateComponent<AgentState> {
    private var state = AgentState()
    private val tokenKey = CredentialAttributes(generateServiceName("AutoIssue", "LlmApiKey"))

    override fun getState() = state

    override fun loadState(s: AgentState) {
        state = s
    }

    fun updateSettings(
        newState: AgentState,
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
        if (defaults.provider.isNotBlank()) state.provider = defaults.provider
        if (defaults.modelName.isNotBlank()) state.modelName = defaults.modelName
        if (defaults.strategyId.isNotBlank()) state.strategyId = defaults.strategyId
        if (defaults.temperature != 0.0) state.temperature = defaults.temperature
        if (defaults.maxIterations != 0) state.maxIterations = defaults.maxIterations
        if (defaults.systemPrompt.isNotBlank()) state.systemPrompt = defaults.systemPrompt
    }

    fun getEffectiveConfig(): AgentConfig? {
        val apiKey = PasswordSafe.instance.getPassword(tokenKey)

        if (apiKey.isNullOrBlank()) {
            thisLogger().warn("Failed to retrieve Agent configuration: LLM API Key is missing in PasswordSafe.")
            return null
        }
        return AgentConfig(
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
