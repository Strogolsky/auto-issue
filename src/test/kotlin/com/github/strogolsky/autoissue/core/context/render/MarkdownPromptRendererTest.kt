package com.github.strogolsky.autoissue.core.context.render

import com.github.strogolsky.autoissue.core.context.components.FileContextComponent
import com.github.strogolsky.autoissue.core.context.components.JiraProjectMetadata
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownPromptRendererTest {
    private val renderer = MarkdownPromptRenderer()

    @Test // UC-R7
    fun should_FormatHeadersAndCodeBlocks_When_RenderingFileContextToMarkdown() {
        // --- TEST FLOW ---
        // 1. ARRANGE: Create file context.
        val fileContext =
            FileContextComponent(
                fileName = "MarkdownGen.kt",
                language = "Kotlin",
                imports = emptyList(),
                className = "MarkdownGen",
                classFields = listOf("private val id: Int"),
                methodSignature = "fun generate()",
                methodBody = "fun generate() = Unit",
            )

        // 2. ACT: Build Markdown prompt.
        val result =
            renderer.buildPrompt {
                components(listOf(fileContext))
            }

        // 3. ASSERT: Verify Markdown syntax.
        assertTrue("Should use h2 for context section", result.contains("## Context"))
        assertTrue("Should use h3 for filename", result.contains("### MarkdownGen.kt"))
        assertTrue("Should format fields as list", result.contains("- private val id: Int"))
        assertTrue("Should start code block", result.contains("```Kotlin"))
        assertTrue("Should contain code body", result.contains("fun generate() = Unit"))
        assertTrue("Should end code block", result.contains("```"))
    }

    @Test // UC-R8
    fun should_FormatArraysAsBulletedLists_When_RenderingJiraMetadataToMarkdown() {
        // --- TEST FLOW ---
        // 1. ARRANGE: Create Jira metadata.
        val jiraMetadata =
            JiraProjectMetadata(
                projectKey = "PROJ",
                projectId = "10001",
                issueTypes = emptyList(),
                priorities = emptyList(),
                components = emptyList(),
                labels = listOf("bug", "ui"),
            )

        // 2. ACT: Build Markdown prompt.
        val result =
            renderer.buildPrompt {
                components(listOf(jiraMetadata))
            }

        // 3. ASSERT: Verify list rendering.
        assertTrue("Should contain project key", result.contains("Project Key: PROJ"))
        assertTrue("Should render first label as list item", result.contains("- bug"))
        assertTrue("Should render second label as list item", result.contains("- ui"))
    }
}
