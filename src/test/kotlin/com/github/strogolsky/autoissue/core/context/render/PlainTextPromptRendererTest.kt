package com.github.strogolsky.autoissue.core.context.render

import com.github.strogolsky.autoissue.core.context.components.FileContextComponent
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
}
