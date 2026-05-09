package com.github.strogolsky.autoissue.plugin.config.validation

import com.github.strogolsky.autoissue.plugin.config.JiraConfigService
import com.github.strogolsky.autoissue.plugin.config.LlmAgentConfigService
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConfigHealthCheckerTest {
    private val jiraService = mockk<JiraConfigService>()
    private val llmService = mockk<LlmAgentConfigService>()

    @Test
    fun testShouldReturnTrueWhenJiraServiceIsReady() {
        // --- TEST FLOW ---
        // 1. ARRANGE
        val validator = JiraConfigValidator(jiraService)
        every { jiraService.isReady() } returns true

        // 2. ACT & 3. ASSERT
        assertTrue(validator.isReady())
    }

    @Test
    fun testShouldReturnFalseWhenJiraServiceIsNotReady() {
        // --- TEST FLOW ---
        // 1. ARRANGE
        val validator = JiraConfigValidator(jiraService)
        every { jiraService.isReady() } returns false

        // 2. ACT & 3. ASSERT
        assertFalse(validator.isReady())
    }

    @Test
    fun testShouldReturnCorrectMessageForJiraValidator() {
        // --- TEST FLOW ---
        // 1. ARRANGE
        val validator = JiraConfigValidator(jiraService)

        // 2. ACT & 3. ASSERT
        assertEquals("JIRA Base URL or credentials are missing. Please configure JIRA settings.", validator.getErrorMessage())
    }

    @Test
    fun testShouldReturnTrueWhenLlmServiceIsReady() {
        // --- TEST FLOW ---
        // 1. ARRANGE
        val validator = LlmConfigValidator(llmService)
        every { llmService.isReady() } returns true

        // 2. ACT & 3. ASSERT
        assertTrue(validator.isReady())
    }

    @Test
    fun testShouldReturnFalseWhenLlmServiceIsNotReady() {
        // --- TEST FLOW ---
        // 1. ARRANGE
        val validator = LlmConfigValidator(llmService)
        every { llmService.isReady() } returns false

        // 2. ACT & 3. ASSERT
        assertFalse(validator.isReady())
    }

    @Test
    fun testShouldReturnCorrectMessageForLlmValidator() {
        // --- TEST FLOW ---
        // 1. ARRANGE
        val validator = LlmConfigValidator(llmService)

        // 2. ACT & 3. ASSERT
        assertEquals("LLM API key is missing. Please configure LLM settings.", validator.getErrorMessage())
    }
}
