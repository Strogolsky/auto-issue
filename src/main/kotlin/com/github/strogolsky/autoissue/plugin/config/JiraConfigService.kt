package com.github.strogolsky.autoissue.plugin.config

import com.github.strogolsky.autoissue.core.exceptions.ConfigurationException
import com.github.strogolsky.autoissue.plugin.state.JiraState
import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.credentialStore.generateServiceName
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@Service(Service.Level.APP)
@State(name = "JiraIntegrationConfiguration", storages = [Storage("AutoIssue_Jira.xml")])
class JiraConfigService : PersistentStateComponent<JiraState> {
    private var state = JiraState()
    private val tokenKey = CredentialAttributes(generateServiceName("AutoIssue", "JiraApiToken"))

    override fun getState() = state

    override fun loadState(s: JiraState) {
        state = s
    }

    fun getApiToken(): String? = PasswordSafe.instance.getPassword(tokenKey)

    fun saveApiToken(token: String) {
        PasswordSafe.instance.set(tokenKey, Credentials(state.username, token))
    }

    fun updateSettings(
        newState: JiraState,
        newKey: String?,
    ) {
        state = newState
        newKey?.let {
            PasswordSafe.instance.set(tokenKey, Credentials(state.username, it))
        }
    }

    fun isReady(): Boolean {
        val token = PasswordSafe.instance.getPassword(tokenKey)
        return !token.isNullOrBlank() && state.baseUrl.isNotBlank() && state.username.isNotBlank()
    }

    fun getEffectiveConfig(): JiraConfig {
        val token = PasswordSafe.instance.getPassword(tokenKey)

        if (token.isNullOrBlank()) {
            throw ConfigurationException("Jira API Token is missing in PasswordSafe.")
        }
        if (state.baseUrl.isBlank()) {
            throw ConfigurationException("Jira Base URL is missing.")
        }
        if (state.username.isBlank()) {
            throw ConfigurationException("Jira Username is missing.")
        }

        return JiraConfig(
            baseUrl = state.baseUrl,
            username = state.username,
            apiToken = token,
            projectKey = state.defaultProjectKey,
        )
    }
}
