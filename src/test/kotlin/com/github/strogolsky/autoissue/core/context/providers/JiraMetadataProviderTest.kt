package com.github.strogolsky.autoissue.core.context.providers

import com.github.strogolsky.autoissue.core.context.ContextEnvironment
import com.github.strogolsky.autoissue.core.context.components.JiraProjectMetadata
import com.github.strogolsky.autoissue.integration.jira.JiraApiService
import com.github.strogolsky.autoissue.plugin.config.JiraConfigService
import com.github.strogolsky.autoissue.plugin.state.JiraState
import com.intellij.openapi.application.Application
import com.intellij.openapi.application.ApplicationManager
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class JiraMetadataProviderTest {

    private val application = mockk<Application>()
    private val jiraApiService = mockk<JiraApiService>()
    private val configService = mockk<JiraConfigService>()
    private val pluginState = mockk<JiraState>()
    private val env = mockk<ContextEnvironment>()

    private lateinit var provider: JiraMetadataProvider

    @Before
    fun setUp() {
        mockkStatic(ApplicationManager::class)
        every { ApplicationManager.getApplication() } returns application

        every { application.getService(JiraApiService::class.java) } returns jiraApiService
        every { application.getService(JiraConfigService::class.java) } returns configService
        every { configService.state } returns pluginState

        provider = JiraMetadataProvider()
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun should_ReturnMetadata_When_ProjectKeyIsValid() = runBlocking {
        // --- TEST FLOW ---
        // 1. ARRANGE
        val expectedMetadata = mockk<JiraProjectMetadata>()
        every { pluginState.defaultProjectKey } returns "PROJ"
        coEvery { jiraApiService.getMetadata("PROJ") } returns expectedMetadata

        // 2. ACT
        val result = provider.provide(env)

        // 3. ASSERT
        assertEquals(expectedMetadata, result)
        coVerify(exactly = 1) { jiraApiService.getMetadata("PROJ") }
    }

    @Test
    fun should_ReturnNull_When_ProjectKeyIsBlank() = runBlocking {
        // --- TEST FLOW ---
        // 1. ARRANGE
        every { pluginState.defaultProjectKey } returns "   "

        // 2. ACT
        val result = provider.provide(env)

        // 3. ASSERT
        assertNull(result)
        coVerify(exactly = 0) { jiraApiService.getMetadata(any()) }
    }

    @Test
    fun should_ReturnNull_When_ApiThrowsException() = runBlocking {
        // --- TEST FLOW ---
        // 1. ARRANGE
        every { pluginState.defaultProjectKey } returns "PROJ"
        coEvery { jiraApiService.getMetadata("PROJ") } throws RuntimeException("Network Error")

        // 2. ACT
        val result = provider.provide(env)

        // 3. ASSERT
        assertNull(result)
    }
}