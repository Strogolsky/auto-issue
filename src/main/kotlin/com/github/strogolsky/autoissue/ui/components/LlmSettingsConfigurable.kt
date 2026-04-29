package com.github.strogolsky.autoissue.ui.components

import com.github.strogolsky.autoissue.core.agent.LlmProviderRegistry
import com.github.strogolsky.autoissue.plugin.config.LlmAgentConfigService
import com.github.strogolsky.autoissue.plugin.state.LlmAgentState
import com.intellij.openapi.components.service
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.COLUMNS_LARGE
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import javax.swing.DefaultComboBoxModel

class LlmSettingsConfigurable(private val project: Project) : Configurable {
    private val configService = project.service<LlmAgentConfigService>()
    private val resolver = project.service<LlmProviderRegistry>()

    private lateinit var settingsPanel: DialogPanel

    private var llmProvider: String? = ""
    private var llmModel: String? = ""
    private var llmToken = ""

    private val modelsModel = DefaultComboBoxModel<String>()

    override fun getDisplayName() = "LLM"

    override fun createComponent(): DialogPanel {
        settingsPanel =
            panel {
                row("Provider:") {
                    comboBox(resolver.providers().sorted())
                        .bindItem(::llmProvider)
                        .applyToComponent {
                            addActionListener {
                                val selected = selectedItem as? String
                                // Обновляем список моделей только если провайдер реально изменился
                                if (selected != null && selected != llmProvider) {
                                    llmProvider = selected
                                    loadModelsForProvider(selected)

                                    // Автоматически выбираем первую модель из нового списка, чтобы поле не было пустым
                                    llmModel = if (modelsModel.size > 0) modelsModel.getElementAt(0) else ""

                                    // Заставляем UI обновиться с новыми значениями
                                    settingsPanel.reset()
                                }
                            }
                        }
                }
                row("Model:") {
                    comboBox(modelsModel)
                        .columns(COLUMNS_LARGE)
                        .bindItem(::llmModel)
                }
                row("API Key:") {
                    passwordField()
                        .columns(COLUMNS_LARGE)
                        .bindText(::llmToken)
                }
            }
        return settingsPanel
    }

    private fun loadModelsForProvider(provider: String?) {
        modelsModel.removeAllElements()
        if (provider.isNullOrBlank()) return

        val models = runCatching { resolver.modelsFor(provider) }.getOrDefault(emptyList())
        models.forEach { modelsModel.addElement(it) }
    }

    override fun isModified(): Boolean = settingsPanel.isModified()

    override fun apply() {
        settingsPanel.apply()

        val currentState = configService.getState()
        val newState =
            LlmAgentState().apply {
                provider = llmProvider ?: ""
                modelName = llmModel ?: ""
                systemPrompt = currentState.systemPrompt
                temperature = currentState.temperature
                maxIterations = currentState.maxIterations
            }
        configService.updateSettings(newState, llmToken.takeIf { it.isNotBlank() })
    }

    override fun reset() {
        val state = configService.getState()

        // 1. Сначала вытаскиваем данные из стейта
        llmProvider = state.provider
        llmToken = configService.getApiKey() ?: ""

        // 2. Строго до применения модели, заполняем ComboBox нужным списком
        loadModelsForProvider(state.provider)

        // 3. Теперь, когда список заполнен, можно безопасно установить выбранную модель
        llmModel = state.modelName

        // 4. Синхронизируем переменные с интерфейсом
        settingsPanel.reset()
    }
}
