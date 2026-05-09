package com.github.strogolsky.autoissue.integration.code

import com.intellij.openapi.components.service
import com.intellij.psi.SmartPointerManager
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class CodeAnalysisServiceTest : BasePlatformTestCase() {
    private lateinit var service: CodeAnalysisService

    override fun setUp() {
        super.setUp()
        service = project.service<CodeAnalysisService>()
    }

    fun testShouldRespectMaxResultsLimitWhenSearchFilesByNameCalled() {
        // --- TEST FLOW ---
        // 1. ARRANGE
        myFixture.addFileToProject("src/TestAuth.java", "class TestAuth {}")
        myFixture.addFileToProject("src/TestUser.java", "class TestUser {}")
        myFixture.addFileToProject("src/TestConfig.java", "class TestConfig {}")

        // 2. ACT
        val results = service.searchFilesByName("Test", maxResults = 2)

        // 3. ASSERT
        assertEquals(2, results.size)
        assertTrue(results.all { it.contains("Test") })
    }

    fun testShouldTruncateContentWhenFileExceedsSizeLimit() {
        // --- TEST FLOW ---
        // 1. ARRANGE
        val giantContent = "A".repeat(25_000)
        myFixture.addFileToProject("GiantFile.txt", giantContent)

        // 2. ACT
        val content = service.getWholeFileContent("GiantFile.txt")

        // 3. ASSERT
        assertNotNull("Content should not be null", content)
        assertTrue("Content should be truncated", content!!.length < 25_000)
        assertTrue(content.contains("CONTENT TRUNCATED DUE TO SIZE LIMIT"))
    }

    fun testShouldExtractFullContextWhenCaretIsInsideMethod() {
        // --- TEST FLOW ---
        // 1. ARRANGE
        val code =
            """
            import java.util.Date;
            import java.util.List;

            class AuthenticationManager {
                public void loginUser() {
                    // TODO: <caret>Add token validation here
                    System.out.println("Logging in");
                }
            }
            """.trimIndent()

        val psiFile = myFixture.configureByText("Auth.java", code)
        val elementAtCaret = psiFile.findElementAt(myFixture.caretOffset)
        val pointer = SmartPointerManager.getInstance(project).createSmartPsiElementPointer(elementAtCaret!!)

        // 2. ACT
        val context = service.extractDetailedContext(pointer)

        // 3. ASSERT
        assertNotNull("Context should be extracted", context)
        assertEquals("Auth.java", context!!.fileName)

        assertEquals(2, context.imports.size)
        assertTrue(context.imports.any { it.contains("java.util.Date") })

        assertEquals("AuthenticationManager", context.enclosingClass?.name)
        assertEquals("loginUser", context.enclosingMethod?.name)
        assertTrue(context.enclosingMethod!!.body.contains("System.out.println"))
    }
}
