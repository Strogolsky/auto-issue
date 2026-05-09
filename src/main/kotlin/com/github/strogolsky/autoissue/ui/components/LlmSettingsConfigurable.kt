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
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.dsl.builder.COLUMNS_LARGE
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import javax.swing.DefaultComboBoxModel

/**
 * IDE settings panel for configuring LLM (Large Language Model) provider and strategy.
 *
 * Allows users to:
 * - Select which LLM provider to use (e.g., OpenAI, Google, Claude)
 * - Choose a specific generation strategy for that provider
 * - Enter the provider's API key (stored securely)
 *
 * Dynamically loads available strategies when the provider selection changes.
 * Settings are persisted in LlmAgentConfigService.
 *
 * The panel implements IntelliJ's Configurable interface for integration with IDE Settings.
 */
class LlmSettingsConfigurable : Configurable {
    private val configService = ApplicationManager.getApplication().service<LlmAgentConfigService>()
    private val providerRegistry = ApplicationManager.getApplication().service<LlmProviderRegistry>()
    private val strategyRegistry = ApplicationManager.getApplication().service<JiraStrategyRegistry>()

    private lateinit var settingsPanel: DialogPanel

    // Configuration fields (mutable for IDE binding)
    private var llmProvider: String? = ""
    private var llmStrategy: String? = ""
    private var llmToken = ""

    // Combo box model containing strategies available for the selected LLM provider
    private val strategiesModel = DefaultComboBoxModel<IssueStrategyFactory<IssueGenerationInput, JiraIssueCandidate>>()

    /** Returns the display name shown in IDE Settings under Plugins/AutoIssue/LLM */
    override fun getDisplayName() = "LLM"

    /**
     * Creates the settings panel UI for LLM configuration.
     *
     * Layout:
     * - Dropdown to select LLM provider (providers are discovered via extension points)
     * - Dropdown to select generation strategy for the selected provider (populated dynamically)
     * - Password field for the provider's API key
     *
     * When the provider selection changes, the strategy dropdown is repopulated with
     * strategies specific to that provider.
     *
     * @return The DialogPanel containing provider, strategy, and API key UI elements
     */
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

    /**
     * Loads all available strategies for the given LLM provider.
     * Clears the existing strategies model and repopulates it with strategies
     * specific to the provider (e.g., different reasoning or prompt strategies).
     *
     * @param provider The LLM provider identifier
     */
    private fun loadStrategiesForProvider(provider: String?) {
        strategiesModel.removeAllElements()
        if (provider.isNullOrBlank()) return
        strategyRegistry.strategiesFor(provider).forEach { strategiesModel.addElement(it) }
    }

    /** Checks if any settings have been modified since last apply() */
    override fun isModified(): Boolean = settingsPanel.isModified()

    /**
     * Persists the user's settings changes to the IDE's persistent state.
     * Saves the provider name, strategy ID, and API key securely.
     * Preserves other config values (system prompt, temperature, max iterations).
     */
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

    /** Reloads all fields from persistent state, discarding any unsaved changes */
    override fun reset() {
        val state = configService.getState()

        llmProvider = state.provider
        llmToken = configService.getApiKey() ?: ""

        loadStrategiesForProvider(state.provider)

        llmStrategy = state.strategyId

        settingsPanel.reset()
    }

    /** Helper to extract all elements from a combo box model as a list */
    private fun <T> DefaultComboBoxModel<T>.allElements(): List<T> = (0 until size).map { getElementAt(it) }
}
