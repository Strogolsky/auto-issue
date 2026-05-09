package com.github.strogolsky.autoissue.plugin.startup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PluginConfigLoaderTest {
    private fun xml(content: String) = content.trimIndent().byteInputStream()

    @Test
    fun testShouldLoadConfigFromActualResource() {
        // --- TEST FLOW ---
        // 1. ARRANGE: default PluginConfig.xml from classpath (file-based system-prompt and examples)

        // 2. ACT
        val config = PluginConfigLoader.load()

        // 3. ASSERT
        assertEquals("GOOGLE", config.llm.provider)
        assertEquals("XML", config.renderingFormat)
        assertTrue(config.masking.enabled)
    }

    @Test
    fun testShouldLoadInlineSystemPrompt_When_NoFileAttribute() {
        // --- TEST FLOW ---
        // 1. ARRANGE
        val stream =
            xml(
                """
                <plugin-config>
                  <llm>
                    <default-provider>GOOGLE</default-provider>
                    <default-strategy>jira-direct</default-strategy>
                    <temperature>0.0</temperature>
                    <max-iterations>10</max-iterations>
                    <system-prompt>Inline system prompt text</system-prompt>
                  </llm>
                  <rendering><format>MARKDOWN</format></rendering>
                </plugin-config>
                """,
            )

        // 2. ACT
        val config = PluginConfigLoader.load(stream)

        // 3. ASSERT
        assertTrue("System prompt should contain inline content", config.llm.systemPrompt.contains("Inline system prompt text"))
        assertEquals("MARKDOWN", config.renderingFormat)
    }

    @Test
    fun testShouldReturnEmptyExamples_When_ExamplesNodeAbsent() {
        // --- TEST FLOW ---
        // 1. ARRANGE: no <examples> element at all
        val stream =
            xml(
                """
                <plugin-config>
                  <llm>
                    <default-provider>GOOGLE</default-provider>
                    <default-strategy>jira-direct</default-strategy>
                    <temperature>0.0</temperature>
                    <max-iterations>10</max-iterations>
                    <system-prompt>System prompt</system-prompt>
                  </llm>
                  <rendering><format>XML</format></rendering>
                </plugin-config>
                """,
            )

        // 2. ACT
        val config = PluginConfigLoader.load(stream)

        // 3. ASSERT
        assertEquals("System prompt", config.llm.systemPrompt)
    }

    @Test
    fun testShouldReturnEmptyExamples_When_ExamplesFileAttributeIsEmpty() {
        // --- TEST FLOW ---
        // 1. ARRANGE: <examples file=""> — empty file path, no inline content
        val stream =
            xml(
                """
                <plugin-config>
                  <llm>
                    <default-provider>GOOGLE</default-provider>
                    <default-strategy>jira-direct</default-strategy>
                    <temperature>0.0</temperature>
                    <max-iterations>10</max-iterations>
                    <system-prompt>Prompt</system-prompt>
                    <examples file=""></examples>
                  </llm>
                  <rendering><format>XML</format></rendering>
                </plugin-config>
                """,
            )

        // 2. ACT
        val config = PluginConfigLoader.load(stream)

        // 3. ASSERT
        assertEquals("Prompt", config.llm.systemPrompt)
    }

    @Test
    fun testShouldDisableDevConfig_When_LocalPropertiesNodeAbsent() {
        // --- TEST FLOW ---
        // 1. ARRANGE: no <local-properties> element
        val stream =
            xml(
                """
                <plugin-config>
                  <llm>
                    <default-provider>GOOGLE</default-provider>
                    <default-strategy>jira-direct</default-strategy>
                    <temperature>0.0</temperature>
                    <max-iterations>10</max-iterations>
                    <system-prompt>Prompt</system-prompt>
                  </llm>
                  <rendering><format>XML</format></rendering>
                </plugin-config>
                """,
            )

        // 2. ACT
        val config = PluginConfigLoader.load(stream)

        // 3. ASSERT
        assertFalse("Local properties should be disabled when node is absent", config.dev.localPropertiesEnabled)
    }

    @Test
    fun testShouldUseDefaultMaskingConfig_When_MaskingNodeAbsent() {
        // --- TEST FLOW ---
        // 1. ARRANGE: no <masking> element
        val stream =
            xml(
                """
                <plugin-config>
                  <llm>
                    <default-provider>GOOGLE</default-provider>
                    <default-strategy>jira-direct</default-strategy>
                    <temperature>0.0</temperature>
                    <max-iterations>10</max-iterations>
                    <system-prompt>Prompt</system-prompt>
                  </llm>
                  <rendering><format>XML</format></rendering>
                </plugin-config>
                """,
            )

        // 2. ACT
        val config = PluginConfigLoader.load(stream)

        // 3. ASSERT
        assertTrue("Masking should be enabled by default", config.masking.enabled)
    }

    @Test
    fun testShouldDisableMasking_When_MaskingEnabledIsFalse() {
        // --- TEST FLOW ---
        // 1. ARRANGE
        val stream =
            xml(
                """
                <plugin-config>
                  <llm>
                    <default-provider>GOOGLE</default-provider>
                    <default-strategy>jira-direct</default-strategy>
                    <temperature>0.0</temperature>
                    <max-iterations>10</max-iterations>
                    <system-prompt>Prompt</system-prompt>
                  </llm>
                  <rendering><format>XML</format></rendering>
                  <masking><enabled>false</enabled></masking>
                </plugin-config>
                """,
            )

        // 2. ACT
        val config = PluginConfigLoader.load(stream)

        // 3. ASSERT
        assertFalse("Masking should be disabled", config.masking.enabled)
    }
}
