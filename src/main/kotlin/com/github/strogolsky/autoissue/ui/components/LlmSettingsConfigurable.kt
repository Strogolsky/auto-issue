package com.github.strogolsky.autoissue.ui.components

import com.github.strogolsky.autoissue.core.agent.LlmProviderRegistry
import com.github.strogolsky.autoissue.core.agent.strategy.IssueStrategyFactory
import com.github.strogolsky.autoissue.core.agent.strategy.JiraStrategyRegistry
import com.github.strogolsky.autoissue.core.input.IssueGenerationInput
import com.github.strogolsky.autoissue.core.output.JiraIssueCandidate
import com.github.strogolsky.autoissue.plugin.config.LlmAgentConfigService
import com.github.strogolsky.autoissue.plugin.state.LlmAgentState
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.COLUMNS_LARGE
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.SimpleListCellRenderer
import javax.swing.DefaultComboBoxModel

class LlmSettingsConfigurable(private val project: Project) : Configurable {
    private val configService = project.service<LlmAgentConfigService>()
    private val providerRegistry = project.service<LlmProviderRegistry>()
    private val strategyRegistry = ApplicationManager.getApplication().service<JiraStrategyRegistry>()

    private lateinit var settingsPanel: DialogPanel

    private var llmProvider: String? = ""
    private var llmStrategy: String? = ""
    private var llmToken = ""

    private val strategiesModel = DefaultComboBoxModel<IssueStrategyFactory<IssueGenerationInput, JiraIssueCandidate>>()

    override fun getDisplayName() = "LLM"

    override fun createComponent(): DialogPanel {
        settingsPanel =
            panel {
                row("Provider:") {
                    comboBox(providerRegistry.providers().sorted())
                        .bindItem(::llmProvider)
                        .applyToComponent {
                            addActionListener {
                                val selected = selectedItem as? String
                                if (selected != null && selected != llmProvider) {
                                    llmProvider = selected
                                    loadStrategiesForProvider(selected)
                                    llmStrategy = strategiesModel.getElementAt(0)?.id ?: ""
                                    settingsPanel.reset()
                                }
                            }
                        }
                }
                row("Strategy:") {
                    comboBox(strategiesModel)
                        .columns(COLUMNS_LARGE)
                        .applyToComponent {
                            renderer = SimpleListCellRenderer.create("") { it.displayName }
                        }
                        .bindItem(
                            getter = {
                                strategiesModel.allElements().find { it.id == llmStrategy }
                            },
                            setter = { factory ->
                                llmStrategy = factory?.id ?: ""
                            },
                        )
                }
                row("API Key:") {
                    passwordField()
                        .columns(COLUMNS_LARGE)
                        .bindText(::llmToken)
                }
            }
        return settingsPanel
    }

    private fun loadStrategiesForProvider(provider: String?) {
        strategiesModel.removeAllElements()
        if (provider.isNullOrBlank()) return
        strategyRegistry.strategiesFor(provider).forEach { strategiesModel.addElement(it) }
    }

    override fun isModified(): Boolean = settingsPanel.isModified()

    override fun apply() {
        settingsPanel.apply()

        val currentState = configService.getState()
        val newState =
            LlmAgentState().apply {
                provider = llmProvider ?: ""
                strategyId = llmStrategy ?: ""
                systemPrompt = currentState.systemPrompt
                temperature = currentState.temperature
                maxIterations = currentState.maxIterations
            }
        configService.updateSettings(newState, llmToken.takeIf { it.isNotBlank() })
    }

    override fun reset() {
        val state = configService.getState()

        llmProvider = state.provider
        llmToken = configService.getApiKey() ?: ""

        loadStrategiesForProvider(state.provider)

        llmStrategy = state.strategyId

        settingsPanel.reset()
    }

    private fun <T> DefaultComboBoxModel<T>.allElements(): List<T> =
        (0 until size).map { getElementAt(it) }
}
