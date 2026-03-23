package com.github.strogolsky.autoissue.settings

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.generateServiceName
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.State

@Service(Service.Level.PROJECT)
@State(
    name = "JiraAgentConfiguration",
    storages = [Storage("AutoIssuePlugin.xml")]
)
class AgentConfigService : PersistentStateComponent<AgentState> {
    private var state = AgentState()
    private val tokenKey = CredentialAttributes(generateServiceName("AutoIssue", "LlmApiKey"))

    override fun getState() = state
    override fun loadState(s: AgentState) { state = s }

    fun updateSettings(newState: AgentState, newKey: String?) {
        state = newState
        newKey?.let { PasswordSafe.instance.setPassword(tokenKey, it) }
    }

    fun getEffectiveConfig(): AgentConfig? {
        val apiKey = PasswordSafe.instance.getPassword(tokenKey)
        require(!apiKey.isNullOrBlank()) { "LLM API Key is missing." }
        return AgentConfig(apiKey, state.provider, state.modelName, state.systemPrompt, state.temperature, state.maxIterations, state.strategyId)
    }
}