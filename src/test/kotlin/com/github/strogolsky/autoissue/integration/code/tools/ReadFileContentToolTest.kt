package com.github.strogolsky.autoissue.integration.code.tools

import com.github.strogolsky.autoissue.core.context.render.PromptRenderService
import com.github.strogolsky.autoissue.integration.code.CodeAnalysisService
import com.intellij.openapi.project.Project
import io.mockk.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ReadFileContentToolTest {

    private val project = mockk<Project>()
    private val codeService = mockk<CodeAnalysisService>()
    private val renderService = mockk<PromptRenderService>()
    private lateinit var tool: ReadFileContentTool

    @Before
    fun setUp() {
        every { project.getService(CodeAnalysisService::class.java) } returns codeService
        every { project.getService(PromptRenderService::class.java) } returns renderService
        tool = ReadFileContentTool(project)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun should_ReturnFileContent_When_FileIsValid() {
        // --- TEST FLOW ---
        // 1. ARRANGE
        val path = "src/Main.kt"
        val rawContent = "val secret = \"12345\""
        val maskedContent = "val secret = \"****\""

        every { codeService.isBinaryFile(path) } returns false
        every { codeService.getWholeFileContent(path) } returns rawContent
        every { renderService.mask(rawContent) } returns maskedContent

        // 2. ACT
        val response = tool.readFileContent(path)

        // 3. ASSERT
        assertTrue(response is FileContentResponse)
        val success = response as FileContentResponse
        assertEquals(path, success.filePath)
        assertEquals(maskedContent, success.content)
    }

    @Test
    fun should_ReturnError_When_FileIsBinary() {
        // --- TEST FLOW ---
        // 1. ARRANGE
        val path = "image.png"
        every { codeService.isBinaryFile(path) } returns true

        // 2. ACT
        val response = tool.readFileContent(path)

        // 3. ASSERT
        assertTrue(response is ToolErrorResponse)
        assertEquals("Cannot read binary files.", (response as ToolErrorResponse).errorDetails)
        verify(exactly = 0) { codeService.getWholeFileContent(any()) }
    }

    @Test
    fun should_ReturnError_When_FileNotFound() {
        // --- TEST FLOW ---
        // 1. ARRANGE
        val path = "missing.kt"
        every { codeService.isBinaryFile(path) } returns false
        every { codeService.getWholeFileContent(path) } returns null

        // 2. ACT
        val response = tool.readFileContent(path)

        // 3. ASSERT
        assertTrue(response is ToolErrorResponse)
        assertTrue((response as ToolErrorResponse).errorDetails.contains("File not found"))
    }
}