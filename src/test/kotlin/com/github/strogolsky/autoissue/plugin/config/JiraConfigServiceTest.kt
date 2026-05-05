package com.github.strogolsky.autoissue.plugin.config

import com.github.strogolsky.autoissue.core.exceptions.ConfigurationException
import com.github.strogolsky.autoissue.plugin.state.JiraState
import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.generateServiceName
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class JiraConfigServiceTest : BasePlatformTestCase() {

    private lateinit var service: JiraConfigService
    private lateinit var tokenKey: CredentialAttributes

    override fun setUp() {
        super.setUp()
        service = JiraConfigService()
        tokenKey = CredentialAttributes(generateServiceName("AutoIssue", "JiraApiToken"))

        PasswordSafe.instance.setPassword(tokenKey, null)
    }

    override fun tearDown() {
        PasswordSafe.instance.setPassword(tokenKey, null)
        super.tearDown()
    }

    fun testShould_UpdateInternalState_When_LoadStateCalled() {
        // 1. ARRANGE
        val newState = JiraState().apply {
            baseUrl = "https://jira.test"
            username = "testuser"
            defaultProjectKey = "TEST"
        }

        // 2. ACT
        service.loadState(newState)

        // 3. ASSERT
        val state = service.state
        assertEquals("https://jira.test", state.baseUrl)
        assertEquals("testuser", state.username)
        assertEquals("TEST", state.defaultProjectKey)
    }

    fun testShould_SaveToPasswordSafe_When_UpdateSettingsWithNewToken() {
        // 1. ARRANGE
        val newState = JiraState().apply { username = "testuser" }

        // 2. ACT
        service.updateSettings(newState, "new-secret-token")

        // 3. ASSERT
        // Проверяем реальный PasswordSafe!
        val savedPassword = PasswordSafe.instance.getPassword(tokenKey)
        assertEquals("new-secret-token", savedPassword)
        assertEquals("testuser", service.state.username)
    }

    fun testShould_NotOverwritePasswordSafe_When_UpdateSettingsWithoutToken() {
        // 1. ARRANGE
        PasswordSafe.instance.setPassword(tokenKey, "old-token")
        val newState = JiraState().apply { username = "testuser" }

        // 2. ACT
        service.updateSettings(newState, null)

        // 3. ASSERT
        val savedPassword = PasswordSafe.instance.getPassword(tokenKey)
        assertEquals("old-token", savedPassword) // Убеждаемся, что он не стерся
        assertEquals("testuser", service.state.username)
    }

    fun testShould_ReturnFalse_When_FieldsAreMissing() {
        // 1. ARRANGE
        service.loadState(JiraState().apply {
            baseUrl = ""
            username = ""
        })

        // 2. ACT
        val isReady = service.isReady()

        // 3. ASSERT
        assertFalse(isReady)
    }

    fun testShould_ReturnTrue_When_SystemIsReady() {
        // 1. ARRANGE
        PasswordSafe.instance.setPassword(tokenKey, "token")
        service.loadState(JiraState().apply {
            baseUrl = "https://jira.test"
            username = "user"
        })

        // 2. ACT
        val isReady = service.isReady()

        // 3. ASSERT
        assertTrue(isReady)
    }

    fun testShould_ReturnValidDto_When_SystemIsReady() {
        // 1. ARRANGE
        PasswordSafe.instance.setPassword(tokenKey, "my-token")
        service.loadState(JiraState().apply {
            baseUrl = "https://jira"
            username = "admin"
            defaultProjectKey = "PROJ"
        })

        // 2. ACT
        val config = service.getEffectiveConfig()

        // 3. ASSERT
        assertEquals("https://jira", config.baseUrl)
        assertEquals("admin", config.username)
        assertEquals("my-token", config.apiToken)
        assertEquals("PROJ", config.projectKey)
    }

    fun testShould_ThrowException_When_TokenIsMissing() {
        // 1. ARRANGE
        service.loadState(JiraState().apply {
            baseUrl = "https://jira"
            username = "admin"
        })

        // 2. ACT & 3. ASSERT
        try {
            service.getEffectiveConfig()
            fail("Expected ConfigurationException to be thrown")
        } catch (e: ConfigurationException) {
        }
    }
}