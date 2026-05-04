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

    // UC-C2
    fun test_should_RespectMaxResultsLimit_When_SearchFilesByNameCalled() {
        // --- TEST FLOW ---
        // 1. ARRANGE: Create virtual files in the project memory.
        myFixture.addFileToProject("src/TestAuth.java", "class TestAuth {}")
        myFixture.addFileToProject("src/TestUser.java", "class TestUser {}")
        myFixture.addFileToProject("src/TestConfig.java", "class TestConfig {}")

        // 2. ACT: Search for files containing "Test", limited to 2 results.
        val results = service.searchFilesByName("Test", maxResults = 2)

        // 3. ASSERT: Verify exactly 2 files are returned.
        assertEquals(2, results.size)
        assertTrue(results.all { it.contains("Test") })
    }

    // UC-C3
    fun test_should_TruncateContent_When_FileExceedsSizeLimit() {
        // --- TEST FLOW ---
        // 1. ARRANGE: Create a string of 25,000 characters (exceeding the 20,000 limit).
        val giantContent = "A".repeat(25_000)
        myFixture.addFileToProject("GiantFile.txt", giantContent)

        // 2. ACT: Read the file content.
        val content = service.getWholeFileContent("GiantFile.txt")

        // 3. ASSERT: Verify the text is truncated and contains the truncation notice.
        assertNotNull("Content should not be null", content)
        assertTrue("Content should be truncated", content!!.length < 25_000)
        assertTrue(content.contains("CONTENT TRUNCATED DUE TO SIZE LIMIT"))
    }

    // UC-C4
    fun test_should_ExtractFullContext_When_CaretIsInsideMethod() {
        // --- TEST FLOW ---
        // 1. ARRANGE: Source code with imports, class, method, and a cursor (<caret>).
        val code = """
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

        // 2. ACT: Extract the detailed context using the PSI pointer.
        val context = service.extractDetailedContext(pointer)

        // 3. ASSERT: Verify that the parser correctly traversed up the AST to find all components.
        assertNotNull("Context should be extracted", context)
        assertEquals("Auth.java", context!!.fileName)

        assertEquals(2, context.imports.size)
        assertTrue(context.imports.any { it.contains("java.util.Date") })

        assertEquals("AuthenticationManager", context.enclosingClass?.name)
        assertEquals("loginUser", context.enclosingMethod?.name)
        assertTrue(context.enclosingMethod!!.body.contains("System.out.println"))
    }
}