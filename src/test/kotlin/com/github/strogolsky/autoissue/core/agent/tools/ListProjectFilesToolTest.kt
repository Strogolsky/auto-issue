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

class ListProjectFilesToolTest {
    private val project = mockk<Project>()
    private val codeService = mockk<CodeAnalysisService>()
    private lateinit var tool: ListProjectFilesTool

    @Before
    fun setUp() {
        every { project.getService(CodeAnalysisService::class.java) } returns codeService
        tool = ListProjectFilesTool(project)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun should_ReturnFileList_When_FilesExist() {
        // --- TEST FLOW ---
        // 1. ARRANGE
        val files = listOf("src/UserService.kt", "src/OrderRepo.kt")
        every { codeService.listAllSourceFiles() } returns files

        // 2. ACT
        val response = tool.listProjectFiles()

        // 3. ASSERT
        assertTrue(response is ProjectStructureResponse)
        assertEquals(files, (response as ProjectStructureResponse).files)
    }

    @Test
    fun should_ReturnEmptyList_When_NoFilesFound() {
        // --- TEST FLOW ---
        // 1. ARRANGE
        every { codeService.listAllSourceFiles() } returns emptyList()

        // 2. ACT
        val response = tool.listProjectFiles()

        // 3. ASSERT
        assertTrue(response is ProjectStructureResponse)
        assertTrue((response as ProjectStructureResponse).files.isEmpty())
    }
}
