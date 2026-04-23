package com.github.strogolsky.autoissue.ui

import com.github.strogolsky.autoissue.agent.ModelProviderResolver
import com.github.strogolsky.autoissue.settings.AgentConfigService
import com.github.strogolsky.autoissue.settings.AgentState
import com.intellij.openapi.components.service
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.dsl.builder.COLUMNS_LARGE
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent
import javax.swing.JPasswordField

class LlmSettingsConfigurable(private val project: Project) : Configurable {

    private val configService = project.service<AgentConfigService>()
    private val resolver = project.service<ModelProviderResolver>()

    private lateinit var providerComboBox: ComboBox<String>
    private lateinit var modelComboBox: ComboBox<String>
    private lateinit var apiKeyField: JPasswordField

    override fun getDisplayName() = "LLM"

    override fun createComponent(): JComponent = panel {
        row("Provider:") {
            providerComboBox = comboBox(resolver.providers().sorted()).component
            providerComboBox.addActionListener { refreshModels() }
        }
        row("Model:") {
            modelComboBox = comboBox(emptyList<String>()).columns(COLUMNS_LARGE).component
        }
        row("API Key:") {
            apiKeyField = passwordField().columns(COLUMNS_LARGE).component
        }
    }

    private fun refreshModels() {
        val provider = providerComboBox.selectedItem as? String ?: return
        val models = runCatching { resolver.modelsFor(provider) }.getOrDefault(emptyList())
        modelComboBox.removeAllItems()
        models.forEach { modelComboBox.addItem(it) }
    }

    override fun isModified(): Boolean {
        val state = configService.getState()
        val savedKey = configService.getApiKey() ?: ""
        return providerComboBox.selectedItem as? String != state.provider ||
            modelComboBox.selectedItem as? String != state.modelName ||
            String(apiKeyField.password) != savedKey
    }

    override fun apply() {
        val newState = AgentState().apply {
            val s = configService.getState()
            provider = providerComboBox.selectedItem as? String ?: s.provider
            modelName = modelComboBox.selectedItem as? String ?: s.modelName
            systemPrompt = s.systemPrompt
            temperature = s.temperature
            maxIterations = s.maxIterations
            strategyId = s.strategyId
        }
        val token = String(apiKeyField.password).trim().takeIf { it.isNotBlank() }
        configService.updateSettings(newState, token)
    }

    override fun reset() {
        val state = configService.getState()

        val providers = resolver.providers().sorted()
        providerComboBox.removeAllItems()
        providers.forEach { providerComboBox.addItem(it) }
        providerComboBox.selectedItem = state.provider

        refreshModels()
        modelComboBox.selectedItem = state.modelName

        apiKeyField.text = configService.getApiKey() ?: ""
    }
}
