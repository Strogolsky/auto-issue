package com.github.strogolsky.autoissue.core.context.render

import com.github.strogolsky.autoissue.core.context.components.FileContextComponent
import com.github.strogolsky.autoissue.core.context.components.IssueInstruction
import com.github.strogolsky.autoissue.core.context.components.JiraProjectMetadata
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlainTextPromptRendererTest {
    private val renderer = PlainTextPromptRenderer()

    @Test // UC-R1
    fun should_RenderSingleInstruction_When_BuildPromptIsCalledWithOneBlock() {
        // --- TEST FLOW ---
        // 1. ARRANGE: Define target instruction.
        val targetInstruction = "Fix the NullPointerException in auth service"

        // 2. ACT: Build prompt.
        val result =
            renderer.buildPrompt {
                instruction(targetInstruction)
            }

        // 3. ASSERT: Verify output contains the instruction.
        assertTrue("Result should contain the instruction", result.contains(targetInstruction))
    }

    @Test // UC-R2
    fun should_IncludeFileNameAndCode_When_RenderingCompleteFileContext() {
        // --- TEST FLOW ---
        // 1. ARRANGE: Create full context.
        val fileContext =
            FileContextComponent(
                fileName = "AuthService.kt",
                language = "Kotlin",
                imports = listOf("import java.util.*"),
                className = "AuthService",
                classFields = listOf("private val repository: AuthRepo"),
                methodSignature = "fun login()",
                methodBody = "fun login() { ... }",
            )

        // 2. ACT: Render context.
        val result = renderer.renderComponent(fileContext)

        // 3. ASSERT: Verify all metadata is present.
        assertTrue(result.contains("AuthService.kt"))
        assertTrue(result.contains("Kotlin"))
        assertTrue(result.contains("fun login()"))
    }

    @Test // UC-R3
    fun should_OmitNullValuesAndEmptyImports_When_RenderingIncompleteFileContext() {
        // --- TEST FLOW ---
        // 1. ARRANGE: Create incomplete context.
        val incompleteContext =
            FileContextComponent(
                fileName = "Utils.kt",
                language = "Kotlin",
                imports = emptyList(),
                className = null,
                classFields = emptyList(),
                methodSignature = null,
                methodBody = "fun help() {}",
            )

        // 2. ACT: Render context.
        val result = renderer.renderComponent(incompleteContext)

        // 3. ASSERT: Verify 'null' strings and empty sections are omitted.
        assertFalse("Result should not contain 'null'", result.contains("null"))
        assertFalse("Result should not contain empty imports", result.contains("Imports:"))
        assertTrue("Result should still contain file name", result.contains("Utils.kt"))
    }

    @Test // UC-R4
    fun should_RenderProjectKeyAndLabels_When_JiraMetadataHasLabels() {
        // --- TEST FLOW ---
        // 1. ARRANGE
        val metadata =
            JiraProjectMetadata(
                projectKey = "PROJ",
                projectId = "10001",
                issueTypes = emptyList(),
                priorities = emptyList(),
                components = emptyList(),
                labels = listOf("bug", "feature"),
            )

        // 2. ACT
        val result = renderer.renderComponent(metadata)

        // 3. ASSERT
        assertTrue("Should contain project key", result.contains("PROJ"))
        assertTrue("Should contain first label", result.contains("- bug"))
        assertTrue("Should contain second label", result.contains("- feature"))
    }

    @Test // UC-R5
    fun should_RenderNoLabelsMessage_When_JiraMetadataHasNoLabels() {
        // --- TEST FLOW ---
        // 1. ARRANGE
        val metadata =
            JiraProjectMetadata(
                projectKey = "PROJ",
                projectId = "10001",
                issueTypes = emptyList(),
                priorities = emptyList(),
                components = emptyList(),
                labels = emptyList(),
            )

        // 2. ACT
        val result = renderer.renderComponent(metadata)

        // 3. ASSERT
        assertTrue("Should indicate no labels are available", result.contains("No labels available"))
    }

    @Test // UC-R6
    fun should_RenderDescription_When_ComponentIsIssueInstruction() {
        // --- TEST FLOW ---
        // 1. ARRANGE
        val instruction = IssueInstruction("Fix the login bug")

        // 2. ACT
        val result = renderer.renderComponent(instruction)

        // 3. ASSERT
        assertTrue("Should contain instruction description", result.contains("Fix the login bug"))
    }

    @Test // UC-R9
    fun should_IncludeSectionContent_When_BuildPromptHasSection() {
        // --- TEST FLOW ---
        // 1. ARRANGE & 2. ACT
        val result =
            renderer.buildPrompt {
                section("Examples", "Example content here")
            }

        // 3. ASSERT
        assertTrue("Should contain uppercased section title", result.contains("EXAMPLES"))
        assertTrue("Should contain section content", result.contains("Example content here"))
    }

    @Test // UC-R10
    fun should_RenderAllComponents_When_BuildPromptHasMultipleComponents() {
        // --- TEST FLOW ---
        // 1. ARRANGE
        val instruction = IssueInstruction("Do something")
        val fileContext =
            FileContextComponent(
                fileName = "Foo.kt",
                language = "Kotlin",
                imports = emptyList(),
                className = null,
                classFields = emptyList(),
                methodSignature = null,
                methodBody = "fun foo() {}",
            )

        // 2. ACT
        val result =
            renderer.buildPrompt {
                components(listOf(instruction, fileContext))
            }

        // 3. ASSERT
        assertTrue("Should contain instruction text", result.contains("Do something"))
        assertTrue("Should contain file name", result.contains("Foo.kt"))
    }
}
