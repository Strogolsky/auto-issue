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

/**
 * Manages JIRA integration configuration and credentials.
 *
 * Stores and retrieves:
 * - JIRA Base URL
 * - Username/Email
 * - API Token (securely via IDE credential store)
 * - Default project key
 *
 * This service persists settings in AutoIssue_Jira.xml and uses the IDE's
 * PasswordSafe to securely store the API token.
 */
@Service(Service.Level.APP)
@State(name = "JiraIntegrationConfiguration", storages = [Storage("AutoIssue_Jira.xml")])
class JiraConfigService : PersistentStateComponent<JiraState> {
    private var state = JiraState()
    private val tokenKey = CredentialAttributes(generateServiceName("AutoIssue", "JiraApiToken"))

    override fun getState() = state

    override fun loadState(s: JiraState) {
        state = s
    }

    /**
     * Retrieves the stored JIRA API token from IDE credential store.
     *
     * @return API token string, or null if not set
     */
    fun getApiToken(): String? = PasswordSafe.instance.getPassword(tokenKey)

    /**
     * Saves the JIRA API token to IDE credential store.
     *
     * @param token The API token to store securely
     */
    fun saveApiToken(token: String) {
        PasswordSafe.instance[tokenKey] = Credentials(state.username, token)
    }

    /**
     * Updates JIRA configuration settings and optionally updates the API token.
     *
     * @param newState The new configuration state (URL, username, project key)
     * @param newKey Optional new API token to store
     */
    fun updateSettings(
        newState: JiraState,
        newKey: String?,
    ) {
        state = newState
        newKey?.let {
            PasswordSafe.instance[tokenKey] = Credentials(state.username, it)
        }
    }

    /**
     * Checks if JIRA configuration is complete and ready for use.
     *
     * @return true if all required settings are present
     */
    fun isReady(): Boolean {
        val token = PasswordSafe.instance.getPassword(tokenKey)
        return !token.isNullOrBlank() && state.baseUrl.isNotBlank() && state.username.isNotBlank()
    }

    /**
     * Gets complete JIRA configuration for API calls.
     *
     * Validates that all required settings are present.
     *
     * @return JiraConfig with all required fields populated
     * @throws ConfigurationException if any required setting is missing
     */
    fun getEffectiveConfig(): JiraConfig {
        val token = PasswordSafe.instance.getPassword(tokenKey)

        if (token.isNullOrBlank()) {
            throw ConfigurationException("JIRA API Token is missing. Please configure it in settings.")
        }
        if (state.baseUrl.isBlank()) {
            throw ConfigurationException("JIRA Base URL is missing. Please configure it in settings.")
        }
        if (state.username.isBlank()) {
            throw ConfigurationException("JIRA Username is missing. Please configure it in settings.")
        }

        return JiraConfig(
            baseUrl = state.baseUrl,
            username = state.username,
            apiToken = token,
            projectKey = state.defaultProjectKey,
        )
    }
}
