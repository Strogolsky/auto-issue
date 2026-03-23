package com.github.strogolsky.autoissue.settings

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.generateServiceName
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@Service(Service.Level.PROJECT)
@State(name = "JiraIntegrationConfiguration", storages = [Storage("AutoIssue_Jira.xml")])
class JiraConfigService : PersistentStateComponent<JiraIntegrationState> {
    private var state = JiraIntegrationState()
    private val tokenKey = CredentialAttributes(generateServiceName("AutoIssue", "JiraApiToken"))

    override fun getState() = state

    override fun loadState(s: JiraIntegrationState) {
        state = s
    }

    fun updateSettings(
        newState: JiraIntegrationState,
        newKey: String?,
    ) {
        state = newState
        newKey?.let { PasswordSafe.instance.setPassword(tokenKey, it) }
    }

    fun getEffectiveConfig(): JiraConfig {
        val token = PasswordSafe.instance.getPassword(tokenKey)
        require(!token.isNullOrBlank()) { "Jira API Token is missing." }
        require(state.baseUrl.isNotBlank()) { "Jira Base URL is missing." }
        return JiraConfig(state.baseUrl, token, state.defaultProjectKey)
    }
}
