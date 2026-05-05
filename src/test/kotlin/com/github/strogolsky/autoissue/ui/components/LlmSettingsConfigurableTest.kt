package com.github.strogolsky.autoissue.ui.components

import com.github.strogolsky.autoissue.core.agent.LlmProviderRegistry
import com.github.strogolsky.autoissue.core.agent.strategy.JiraStrategyRegistry
import com.github.strogolsky.autoissue.core.agent.strategy.IssueStrategyFactory
import com.github.strogolsky.autoissue.core.input.IssueGenerationInput
import com.github.strogolsky.autoissue.core.output.JiraIssueCandidate
import com.github.strogolsky.autoissue.plugin.config.LlmAgentConfigService
import com.github.strogolsky.autoissue.plugin.state.LlmAgentState
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.testFramework.replaceService
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import javax.swing.DefaultComboBoxModel

@OptIn(ExperimentalCoroutinesApi::class)
class LlmSettingsConfigurableTest : BasePlatformTestCase() {

    private lateinit var configurable: LlmSettingsConfigurable
    private lateinit var realConfigService: LlmAgentConfigService
    private lateinit var providerRegistryMock: LlmProviderRegistry
    private lateinit var strategyRegistryMock: JiraStrategyRegistry
    private val testDispatcher = UnconfinedTestDispatcher()

    override fun setUp() {
        super.setUp()
        Dispatchers.setMain(testDispatcher)

        realConfigService = ApplicationManager.getApplication().service<LlmAgentConfigService>()
        realConfigService.loadState(LlmAgentState())

        providerRegistryMock = mockk(relaxed = true)
        strategyRegistryMock = mockk(relaxed = true)

        every { providerRegistryMock.providers() } returns setOf("openai", "anthropic")

        val mockFactory = mockk<IssueStrategyFactory<IssueGenerationInput, JiraIssueCandidate>>(relaxed = true)
        every { mockFactory.id } returns "chain-of-thought"
        every { mockFactory.displayName } returns "CoT"

        val mockFactoryDirect = mockk<IssueStrategyFactory<IssueGenerationInput, JiraIssueCandidate>>(relaxed = true)
        every { mockFactoryDirect.id } returns "direct"
        every { mockFactoryDirect.displayName } returns "Direct"

        every { strategyRegistryMock.strategiesFor("openai") } returns listOf(mockFactory)
        every { strategyRegistryMock.strategiesFor("anthropic") } returns listOf(mockFactoryDirect)

        ApplicationManager.getApplication().replaceService(LlmProviderRegistry::class.java, providerRegistryMock, testRootDisposable)
        ApplicationManager.getApplication().replaceService(JiraStrategyRegistry::class.java, strategyRegistryMock, testRootDisposable)

        configurable = LlmSettingsConfigurable()
        configurable.createComponent()
    }

    override fun tearDown() {
        Dispatchers.resetMain()
        super.tearDown()
    }

    fun test_should_LoadDataIntoFields_When_ResetCalled() {
        // --- TEST FLOW ---
        val savedState = LlmAgentState().apply {
            provider = "openai"
            strategyId = "chain-of-thought"
        }
        realConfigService.loadState(savedState)

        configurable.reset()

        val panel = getPrivateField("settingsPanel") as com.intellij.openapi.ui.DialogPanel
        panel.reset()

        configurable.apply()

        val currentState = realConfigService.getState()
        assertEquals("openai", currentState.provider)
        assertEquals("chain-of-thought", currentState.strategyId)
    }

    // UC-U3
    fun test_should_SaveNewSettings_When_ApplyCalled() {
        // --- TEST FLOW ---
        setPrivateField("llmProvider", "anthropic")
        setPrivateField("llmStrategy", "direct")
        setPrivateField("llmToken", "sk-token")

        val panel = getPrivateField("settingsPanel") as com.intellij.openapi.ui.DialogPanel
        panel.reset()

        configurable.apply()

        val savedState = realConfigService.getState()
        assertEquals("anthropic", savedState.provider)
        assertEquals("direct", savedState.strategyId)
        assertEquals("sk-token", realConfigService.getApiKey())
    }

    fun test_should_UpdateStrategiesModel_When_ProviderChanged() {
        // --- TEST FLOW ---
        val mockFactory = mockk<IssueStrategyFactory<IssueGenerationInput, JiraIssueCandidate>>()
        every { mockFactory.id } returns "new-strategy"
        every { mockFactory.displayName } returns "New Strategy"

        every { strategyRegistryMock.strategiesFor("google") } returns listOf(mockFactory)

        invokePrivateMethod("loadStrategiesForProvider", "google")

        val model = getPrivateField("strategiesModel") as DefaultComboBoxModel<*>
        assertEquals(1, model.size)

        val firstElement = model.getElementAt(0) as IssueStrategyFactory<*, *>
        assertEquals("new-strategy", firstElement.id)
    }

    private fun setPrivateField(fieldName: String, value: Any?) {
        val field = configurable.javaClass.getDeclaredField(fieldName)
        field.isAccessible = true
        field.set(configurable, value)
    }

    private fun getPrivateField(fieldName: String): Any? {
        val field = configurable.javaClass.getDeclaredField(fieldName)
        field.isAccessible = true
        return field.get(configurable)
    }

    private fun invokePrivateMethod(methodName: String, vararg args: Any?) {
        val method = configurable.javaClass.getDeclaredMethods().find { it.name == methodName }
        method?.let {
            it.isAccessible = true
            it.invoke(configurable, *args)
        }
    }
}