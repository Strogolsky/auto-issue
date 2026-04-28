package com.github.strogolsky.autoissue.ui.components

import com.github.strogolsky.autoissue.integration.jira.JiraApiService
import com.github.strogolsky.autoissue.plugin.config.JiraConfigService
import com.github.strogolsky.autoissue.plugin.state.JiraState
import com.intellij.icons.AllIcons
import com.intellij.openapi.components.service
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
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

class JiraSettingsConfigurable(private val project: Project) : Configurable {
    private val configService = project.service<JiraConfigService>()
    private val apiService = project.service<JiraApiService>()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var settingsPanel: DialogPanel

    private var jiraUrl = ""
    private var jiraUser = ""
    private var jiraToken = ""
    private var jiraProjectKey: String? = null

    private val projectsModel = DefaultComboBoxModel<ProjectItem>()

    private val loadingIcon = AsyncProcessIcon("JiraTestConnection").apply {
        isVisible = false
        suspend()
    }

    override fun getDisplayName() = "Jira Integration"

    override fun createComponent(): DialogPanel {
        settingsPanel = panel {
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
                            if (currentKey == null) null
                            else {
                                (0 until projectsModel.size)
                                    .map { projectsModel.getElementAt(it) }
                                    .find { it.key == currentKey }
                            }
                        },
                        setter = { jiraProjectKey = it?.key }
                    )

                button("Load Projects") {
                    settingsPanel.apply()
                    loadProjects(jiraProjectKey)
                }
            }
        }
        return settingsPanel
    }

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

    private fun loadProjects(keyToSelect: String?) {
        if (jiraUrl.isBlank() || jiraUser.isBlank() || jiraToken.isBlank()) return

        scope.launch {
            val projects = apiService.getProjects(jiraUrl, jiraUser, jiraToken)
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

    override fun isModified(): Boolean = settingsPanel.isModified()

    override fun apply() {
        settingsPanel.apply()
        val newState = JiraState().apply {
            baseUrl = jiraUrl
            username = jiraUser
            defaultProjectKey = jiraProjectKey ?: ""
        }
        configService.updateSettings(newState, jiraToken.takeIf { it.isNotBlank() })
    }

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

    override fun disposeUIResources() {
        scope.cancel()
    }

    private data class ProjectItem(val key: String, val displayText: String)
}