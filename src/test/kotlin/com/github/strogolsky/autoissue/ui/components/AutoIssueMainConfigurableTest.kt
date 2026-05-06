package com.github.strogolsky.autoissue.ui.components

import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ex.Settings
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import java.awt.event.ActionEvent
import javax.swing.JComponent
import javax.swing.JPanel

class AutoIssueMainConfigurableTest : BasePlatformTestCase() {
    private lateinit var configurable: AutoIssueMainConfigurable

    override fun setUp() {
        super.setUp()
        configurable = AutoIssueMainConfigurable()
    }

    override fun tearDown() {
        configurable.disposeUIResources()
        super.tearDown()
    }

    fun testShouldReturnCorrectDisplayName() {
        assertEquals("AutoIssue", configurable.displayName)
    }

    fun testShouldNotBeModified() {
        assertFalse(configurable.isModified)
    }

    fun testShouldCreateComponentWithoutErrors() {
        val component = configurable.createComponent()

        assertNotNull(component)
        assertTrue(component is JPanel)
    }

    fun testShouldNavigateToJiraSettingsWhenLinkActionTriggered() {
        verifyNavigation("com.github.strogolsky.autoissue.Jira")
    }

    fun testShouldNavigateToLLMSettingsWhenLinkActionTriggered() {
        verifyNavigation("com.github.strogolsky.autoissue.LLM")
    }

    private fun verifyNavigation(targetId: String) {
        // --- MOCK SETUP ---
        val mockComponent = mockk<JComponent>()
        val mockEvent = mockk<ActionEvent>()
        every { mockEvent.source } returns mockComponent

        val mockDataManager = mockk<DataManager>()
        val mockDataContext = mockk<DataContext>()
        val mockSettings = mockk<Settings>(relaxed = true)
        val targetConfigurable = mockk<Configurable>()

        mockkStatic(DataManager::class)
        every { DataManager.getInstance() } returns mockDataManager
        every { mockDataManager.getDataContext(mockComponent) } returns mockDataContext

        every { mockDataContext.getData(Settings.KEY) } returns mockSettings

        every { mockSettings.find(targetId) } returns targetConfigurable

        // --- TEST FLOW ---
        invokePrivateMethod("navigateTo", mockEvent, targetId)

        // --- VERIFICATION ---
        verify(exactly = 1) { mockSettings.select(targetConfigurable) }

        // --- CLEANUP ---
        unmockkStatic(DataManager::class)
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
