package com.github.strogolsky.autoissue.ui.components

import com.github.strogolsky.autoissue.integration.jira.JiraApiService
import com.github.strogolsky.autoissue.plugin.config.JiraConfigService
import com.github.strogolsky.autoissue.plugin.state.JiraState
import com.intellij.icons.AllIcons
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.components.JBLabel
import com.intellij.ui.dsl.builder.COLUMNS_LARGE
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.AsyncProcessIcon
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.swing.DefaultComboBoxModel

/**
 * IDE settings panel for configuring JIRA Cloud integration.
 *
 * Provides UI for users to:
 * - Enter JIRA base URL, username, and API key (credentials are stored securely)
 * - Test the connection to verify credentials work
 * - Select a default project for issue creation (loaded from JIRA with "Load Projects" button)
 *
 * Uses IDE's PasswordSafe for secure credential storage. Settings are persisted in
 * JiraConfigService. The panel is async-aware and shows a loading icon during network calls.
 *
 * The panel implements IntelliJ's Configurable interface for integration with IDE Settings.
 */
class JiraSettingsConfigurable : Configurable {
    private val configService = ApplicationManager.getApplication().service<JiraConfigService>()
    private val apiService = ApplicationManager.getApplication().service<JiraApiService>()

    // Coroutine scope for async operations (connection testing, project loading)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var settingsPanel: DialogPanel

    // Configuration fields (mutable to support IDE's binding mechanism)
    private var jiraUrl = ""
    private var jiraUser = ""
    private var jiraToken = ""
    private var jiraProjectKey: String? = null

    // Combo box model containing JIRA projects loaded from the instance
    private val projectsModel = DefaultComboBoxModel<ProjectItem>()

    // Animated loading icon shown during async operations (connection test, project loading)
    private val loadingIcon =
        AsyncProcessIcon("JiraTestConnection").apply {
            isVisible = false
            suspend()
        }

    /** Returns the display name shown in IDE Settings under Plugins/AutoIssue/Jira Integration */
    override fun getDisplayName() = "Jira Integration"

    /**
     * Creates the settings panel UI with fields for JIRA credentials and project selection.
     *
     * Layout:
     * - Base URL, username, API key fields (with password masking)
     * - "Test Connection" button to verify credentials work
     * - "Load Projects" button to fetch projects from the JIRA instance
     * - Dropdown to select the default project for issue creation
     *
     * @return The DialogPanel containing all configuration UI elements
     */
    override fun createComponent(): DialogPanel {
        settingsPanel =
            panel {
                row("Base URL:") {
                    textField().columns(COLUMNS_LARGE).bindText(::jiraUrl)
                }
                row("Username:") {
                    textField().columns(COLUMNS_LARGE).bindText(::jiraUser)
                }
                row("API Key:") {
                    passwordField().columns(COLUMNS_LARGE).bindText(::jiraToken)
                }
                row {
                    val statusLabel = JBLabel("")
                    button("Test Connection") {
                        settingsPanel.apply()
                        testConnection(statusLabel)
                    }
                    cell(loadingIcon)
                    cell(statusLabel)
                }

                separator()

                row("Default Project:") {
                    comboBox(projectsModel, renderer = SimpleListCellRenderer.create("") { it?.displayText ?: "" })
                        .bindItem(
                            getter = {
                                val currentKey = jiraProjectKey
                                if (currentKey == null) {
                                    null
                                } else {
                                    (0 until projectsModel.size)
                                        .map { projectsModel.getElementAt(it) }
                                        .find { it.key == currentKey }
                                }
                            },
                            setter = { jiraProjectKey = it?.key },
                        )

                    button("Load Projects") {
                        settingsPanel.apply()
                        loadProjects(jiraProjectKey)
                    }
                }
            }
        return settingsPanel
    }

    /**
     * Tests the JIRA connection with the current credentials.
     *
     * Validates that all required fields are filled, then makes an async call to JiraApiService
     * to verify the credentials work. Updates the UI with a loading icon and status message.
     *
     * @param statusLabel Label to update with test result (success/failure)
     */
    private fun testConnection(statusLabel: JBLabel) {
        if (jiraUrl.isBlank() || jiraUser.isBlank() || jiraToken.isBlank()) {
            statusLabel.icon = AllIcons.General.Warning
            statusLabel.text = "Fill in all fields first"
            return
        }

        statusLabel.icon = null
        statusLabel.text = "Testing..."
        loadingIcon.isVisible = true
        loadingIcon.resume()

        scope.launch {
            val ok = apiService.testConnection(jiraUrl, jiraUser, jiraToken)
            withContext(Dispatchers.Main) {
                loadingIcon.suspend()
                loadingIcon.isVisible = false
                if (ok) {
                    statusLabel.icon = AllIcons.General.InspectionsOK
                    statusLabel.text = "Connection successful"
                } else {
                    statusLabel.icon = AllIcons.General.Error
                    statusLabel.text = "Connection failed"
                }
            }
        }
    }

    /**
     * Fetches the list of accessible JIRA projects from the configured instance.
     *
     * Makes an async API call to retrieve projects, then updates the combo box model.
     * If a preferred project key is provided, it will be selected; otherwise the first
     * project is selected.
     *
     * @param keyToSelect Optional project key to pre-select in the dropdown
     */
    private fun loadProjects(keyToSelect: String?) {
        if (jiraUrl.isBlank() || jiraUser.isBlank() || jiraToken.isBlank()) return

        scope.launch {
            val projects =
                apiService.getProjects(jiraUrl, jiraUser, jiraToken)
                    .map { ProjectItem(it.key, "${it.key} - ${it.name}") }

            withContext(Dispatchers.Main) {
                projectsModel.removeAllElements()
                projects.forEach { projectsModel.addElement(it) }

                val itemToSelect = projects.find { it.key == keyToSelect }
                if (itemToSelect != null) {
                    projectsModel.selectedItem = itemToSelect
                } else if (projects.isNotEmpty()) {
                    projectsModel.selectedItem = projects.first()
                }

                settingsPanel.reset()
            }
        }
    }

    /** Checks if any settings have been modified since last apply() */
    override fun isModified(): Boolean = settingsPanel.isModified()

    /**
     * Persists the user's settings changes to the IDE's persistent state.
     * Saves both non-sensitive fields (URL, username, project) and the API token securely.
     */
    override fun apply() {
        settingsPanel.apply()
        val newState =
            JiraState().apply {
                baseUrl = jiraUrl
                username = jiraUser
                defaultProjectKey = jiraProjectKey ?: ""
            }
        configService.updateSettings(newState, jiraToken.takeIf { it.isNotBlank() })
    }

    /** Reloads all fields from persistent state, discarding any unsaved changes */
    override fun reset() {
        val state = configService.getState()
        jiraUrl = state.baseUrl
        jiraUser = state.username
        jiraToken = configService.getApiToken() ?: ""
        jiraProjectKey = state.defaultProjectKey

        settingsPanel.reset()

        if (jiraUrl.isNotBlank() && jiraUser.isNotBlank() && jiraToken.isNotBlank()) {
            loadProjects(jiraProjectKey)
        }
    }

    /** Cleans up resources: cancels any pending async operations */
    override fun disposeUIResources() {
        scope.cancel()
    }

    /** Data class for displaying project options in the combo box */
    private data class ProjectItem(val key: String, val displayText: String)
}
