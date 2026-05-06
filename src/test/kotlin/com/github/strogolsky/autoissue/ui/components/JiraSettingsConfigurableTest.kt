package com.github.strogolsky.autoissue.ui.components

import com.github.strogolsky.autoissue.integration.jira.JiraApiService
import com.github.strogolsky.autoissue.plugin.config.JiraConfigService
import com.github.strogolsky.autoissue.plugin.state.JiraState
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.testFramework.replaceService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class JiraSettingsConfigurableTest : BasePlatformTestCase() {
    private lateinit var configurable: JiraSettingsConfigurable
    private lateinit var realConfigService: JiraConfigService
    private lateinit var apiServiceMock: JiraApiService
    private val testDispatcher = UnconfinedTestDispatcher()

    override fun setUp() {
        super.setUp()
        Dispatchers.setMain(testDispatcher)

        realConfigService = ApplicationManager.getApplication().service<JiraConfigService>()
        realConfigService.loadState(JiraState())

        apiServiceMock = mockk(relaxed = true)
        ApplicationManager.getApplication().replaceService(
            JiraApiService::class.java,
            apiServiceMock,
            testRootDisposable,
        )

        configurable = JiraSettingsConfigurable()
        configurable.createComponent()
    }

    override fun tearDown() {
        Dispatchers.resetMain()
        configurable.disposeUIResources()
        super.tearDown()
    }

    fun testShouldLoadDataIntoFieldsWhenResetCalled() {
        // --- TEST FLOW ---
        val savedState =
            JiraState().apply {
                baseUrl = "https://jira.custom.com"
                username = "admin"
                defaultProjectKey = "PROJ"
            }
        realConfigService.loadState(savedState)

        configurable.reset()
        configurable.apply()

        val currentState = realConfigService.getState()
        assertEquals("https://jira.custom.com", currentState.baseUrl)
        assertEquals("admin", currentState.username)
    }

    fun testShouldSaveNewSettingsWhenApplyCalled() {
        // --- TEST FLOW ---
        setPrivateField("jiraUrl", "https://new-jira.com")
        setPrivateField("jiraUser", "new-user")
        setPrivateField("jiraToken", "new-token")

        val panel = getPrivateField("settingsPanel") as com.intellij.openapi.ui.DialogPanel
        panel.reset()

        configurable.apply()

        val savedState = realConfigService.getState()
        assertEquals("https://new-jira.com", savedState.baseUrl)
        assertEquals("new-user", savedState.username)
        assertEquals("new-token", realConfigService.getApiToken())
    }

    fun testShouldShowWarningWhenTestConnectionClickedWithEmptyFields() {
        // --- TEST FLOW ---
        setPrivateField("jiraUrl", "")
        setPrivateField("jiraUser", "")
        setPrivateField("jiraToken", "")

        val panel = getPrivateField("settingsPanel") as com.intellij.openapi.ui.DialogPanel
        panel.reset()

        val statusLabel = com.intellij.ui.components.JBLabel()
        invokePrivateMethod("testConnection", statusLabel)

        assertEquals("Fill in all fields first", statusLabel.text)
        coVerify(exactly = 0) { apiServiceMock.testConnection(any(), any(), any()) }
    }

    fun testShouldShowSuccessStatusWhenConnectionIsSuccessful() {
        // --- TEST FLOW ---
        coEvery { apiServiceMock.testConnection(any(), any(), any()) } returns true

        setPrivateField("jiraUrl", "https://jira.com")
        setPrivateField("jiraUser", "user")
        setPrivateField("jiraToken", "token")

        val panel = getPrivateField("settingsPanel") as com.intellij.openapi.ui.DialogPanel
        panel.reset()

        val statusLabel = com.intellij.ui.components.JBLabel()
        invokePrivateMethod("testConnection", statusLabel)

        val start = System.currentTimeMillis()
        while (statusLabel.text != "Connection successful" && System.currentTimeMillis() - start < 5000) {
            com.intellij.util.ui.UIUtil.dispatchAllInvocationEvents()
            Thread.sleep(10)
        }

        assertEquals("Connection successful", statusLabel.text)
    }

    private fun setPrivateField(
        fieldName: String,
        value: Any?,
    ) {
        val field = configurable.javaClass.getDeclaredField(fieldName)
        field.isAccessible = true
        field.set(configurable, value)
    }

    private fun getPrivateField(fieldName: String): Any? {
        val field = configurable.javaClass.getDeclaredField(fieldName)
        field.isAccessible = true
        return field.get(configurable)
    }

    private fun invokePrivateMethod(
        methodName: String,
        vararg args: Any?,
    ) {
        val method = configurable.javaClass.getDeclaredMethods().find { it.name == methodName }
        method?.let {
            it.isAccessible = true
            it.invoke(configurable, *args)
        }
    }
}
