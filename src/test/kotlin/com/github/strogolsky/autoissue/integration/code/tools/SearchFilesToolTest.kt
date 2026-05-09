package com.github.strogolsky.autoissue.integration.code.tools

import com.github.strogolsky.autoissue.integration.code.CodeAnalysisService
import com.intellij.openapi.project.Project
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SearchFilesToolTest {
    private val project = mockk<Project>()
    private val codeService = mockk<CodeAnalysisService>()
    private lateinit var tool: SearchFilesTool

    @Before
    fun setUp() {
        every { project.getService(CodeAnalysisService::class.java) } returns codeService
        tool = SearchFilesTool(project)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun should_ReturnPaths_When_FilesAreFound() {
        // --- TEST FLOW ---
        // 1. ARRANGE
        val query = "Service"
        val results = listOf("UserService.kt", "AuthService.kt")
        every { codeService.searchFilesByName(query) } returns results

        // 2. ACT
        val response = tool.searchFiles(query)

        // 3. ASSERT
        assertTrue(response is FileSearchResponse)
        val success = response as FileSearchResponse
        assertEquals(query, success.query)
        assertEquals(results, success.matchedPaths)
    }

    @Test
    fun should_ReturnError_When_NoFilesMatchQuery() {
        // --- TEST FLOW ---
        // 1. ARRANGE
        val query = "Unknown"
        every { codeService.searchFilesByName(query) } returns emptyList()

        // 2. ACT
        val response = tool.searchFiles(query)

        // 3. ASSERT
        assertTrue(response is ToolErrorResponse)
        assertTrue((response as ToolErrorResponse).errorDetails.contains("No files found"))
    }
}
