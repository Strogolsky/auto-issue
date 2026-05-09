package com.github.strogolsky.autoissue.core.agent.tools

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

class SearchSymbolToolTest {
    private val project = mockk<Project>()
    private val codeService = mockk<CodeAnalysisService>()
    private lateinit var tool: SearchSymbolTool

    @Before
    fun setUp() {
        every { project.getService(CodeAnalysisService::class.java) } returns codeService
        tool = SearchSymbolTool(project)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun should_ReturnResults_When_SymbolIsFound() {
        // --- TEST FLOW ---
        // 1. ARRANGE
        val query = "UserValidator"
        val results = listOf("UserValidator → src/Validators.kt")
        every { codeService.searchSymbol(query) } returns results

        // 2. ACT
        val response = tool.searchSymbol(query)

        // 3. ASSERT
        assertTrue(response is SymbolSearchResponse)
        val success = response as SymbolSearchResponse
        assertEquals(query, success.query)
        assertEquals(results, success.results)
    }

    @Test
    fun should_ReturnError_When_SymbolNotFound() {
        // --- TEST FLOW ---
        // 1. ARRANGE
        val query = "NonExistent"
        every { codeService.searchSymbol(query) } returns emptyList()

        // 2. ACT
        val response = tool.searchSymbol(query)

        // 3. ASSERT
        assertTrue(response is ToolErrorResponse)
        assertTrue((response as ToolErrorResponse).errorDetails.contains("No symbols found"))
    }
}
