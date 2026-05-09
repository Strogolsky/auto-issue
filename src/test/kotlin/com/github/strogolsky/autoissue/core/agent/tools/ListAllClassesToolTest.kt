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

class ListAllClassesToolTest {
    private val project = mockk<Project>()
    private val codeService = mockk<CodeAnalysisService>()
    private lateinit var tool: ListAllClassesTool

    @Before
    fun setUp() {
        every { project.getService(CodeAnalysisService::class.java) } returns codeService
        tool = ListAllClassesTool(project)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun should_ReturnClassMap_When_ClassesExist() {
        // --- TEST FLOW ---
        // 1. ARRANGE
        val classes = mapOf("UserService" to "src/UserService.kt", "OrderRepo" to "src/Repositories.kt")
        every { codeService.listAllClasses() } returns classes

        // 2. ACT
        val response = tool.listAllClasses()

        // 3. ASSERT
        assertTrue(response is ClassMapResponse)
        assertEquals(classes, (response as ClassMapResponse).classes)
    }

    @Test
    fun should_ReturnEmptyMap_When_NoClassesFound() {
        // --- TEST FLOW ---
        // 1. ARRANGE
        every { codeService.listAllClasses() } returns emptyMap()

        // 2. ACT
        val response = tool.listAllClasses()

        // 3. ASSERT
        assertTrue(response is ClassMapResponse)
        assertTrue((response as ClassMapResponse).classes.isEmpty())
    }
}
