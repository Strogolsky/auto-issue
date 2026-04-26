package com.github.strogolsky.autoissue.ui.components

import com.github.strogolsky.autoissue.integration.jira.JiraApiService
import com.github.strogolsky.autoissue.plugin.config.JiraConfigService
import com.github.strogolsky.autoissue.plugin.state.JiraIntegrationState
import com.intellij.openapi.components.service
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.COLUMNS_LARGE
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPasswordField
import javax.swing.SwingUtilities
import kotlin.collections.get

class JiraSettingsConfigurable(private val project: Project) : Configurable {
    private val configService = project.service<JiraConfigService>()
    private val apiService = project.service<JiraApiService>()

    private lateinit var baseUrlField: JBTextField
    private lateinit var usernameField: JBTextField
    private lateinit var apiKeyField: JPasswordField
    private lateinit var connectionStatusLabel: JLabel
    private lateinit var projectComboBox: ComboBox<String>
    private lateinit var loadProjectsButton: JButton

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var projectKeyMap: Map<String, String> = emptyMap()

    override fun getDisplayName() = "Jira"

    override fun createComponent(): JComponent =
        panel {
            row("Base URL:") {
                baseUrlField = textField().columns(COLUMNS_LARGE).component
            }
            row("Username:") {
                usernameField = textField().columns(COLUMNS_LARGE).component
            }
            row("API Key:") {
                apiKeyField = passwordField().columns(COLUMNS_LARGE).component
            }
            row {
                button("Test Connection") { testConnection() }
                connectionStatusLabel = label("").component
            }
            separator()
            row("Default Project:") {
                projectComboBox = comboBox(emptyList<String>()).component
                loadProjectsButton = button("Load Projects") { loadProjects() }.component
            }
        }

    private fun testConnection() {
        val url = baseUrlField.text.trim()
        val user = usernameField.text.trim()
        val token = String(apiKeyField.password).trim()

        if (url.isBlank() || user.isBlank() || token.isBlank()) {
            connectionStatusLabel.text = "Fill in all fields first"
            return
        }

        connectionStatusLabel.text = "Testing…"

        scope.launch {
            val ok = apiService.testConnection(url, user, token)
            SwingUtilities.invokeLater {
                connectionStatusLabel.text = if (ok) "✓ Connected" else "✗ Connection failed"
            }
        }
    }

    private fun loadProjects() {
        val url = baseUrlField.text.trim()
        val user = usernameField.text.trim()
        val token = String(apiKeyField.password).trim()
        // Read state on EDT before entering background thread
        val currentKey = configService.getState().defaultProjectKey

        if (url.isBlank() || user.isBlank() || token.isBlank()) return

        loadProjectsButton.isEnabled = false

        scope.launch {
            val projects = apiService.getProjects(url, user, token)
            val newMap = projects.associate { "${it.key} – ${it.name}" to it.key }

            SwingUtilities.invokeLater {
                loadProjectsButton.isEnabled = true
                projectKeyMap = newMap
                projectComboBox.removeAllItems()
                newMap.keys.forEach { projectComboBox.addItem(it) }
                newMap.entries.find { it.value == currentKey }?.key?.let {
                    projectComboBox.selectedItem = it
                }
            }
        }
    }

    override fun isModified(): Boolean {
        val state = configService.getState()
        val savedToken = configService.getApiToken() ?: ""
        val selectedKey =
            projectKeyMap[projectComboBox.selectedItem as? String]
                ?: (projectComboBox.selectedItem as? String ?: "")

        return baseUrlField.text != state.baseUrl ||
            usernameField.text != state.username ||
            String(apiKeyField.password) != savedToken ||
            selectedKey != state.defaultProjectKey
    }

    override fun apply() {
        val selectedDisplay = projectComboBox.selectedItem as? String ?: ""
        val newState =
            JiraIntegrationState().apply {
                baseUrl = this@JiraSettingsConfigurable.baseUrlField.text.trim()
                username = this@JiraSettingsConfigurable.usernameField.text.trim()
                defaultProjectKey = projectKeyMap[selectedDisplay] ?: selectedDisplay
            }
        val token = String(apiKeyField.password).trim().takeIf { it.isNotBlank() }
        configService.updateSettings(newState, token)
    }

    override fun reset() {
        val state = configService.getState()
        baseUrlField.text = state.baseUrl
        usernameField.text = state.username
        apiKeyField.text = configService.getApiToken() ?: ""
        connectionStatusLabel.text = ""

        if (state.baseUrl.isNotBlank() && state.username.isNotBlank() && !configService.getApiToken().isNullOrBlank()) {
            loadProjects()
        }
    }

    override fun disposeUIResources() {
        scope.cancel()
    }
}
