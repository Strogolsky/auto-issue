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
        val fileInfo = service.getWholeFileContent("GiantFile.txt")

        // 3. ASSERT
        assertNotNull("FileInfo should not be null", fileInfo)
        assertTrue("Content should be truncated to maxChars", fileInfo!!.content.length == 20_000)
        assertTrue("truncated flag should be true", fileInfo.truncated)
    }

    fun testShouldReturnAllClassesWithFilePaths() {
        // --- TEST FLOW ---
        // 1. ARRANGE
        myFixture.addFileToProject("src/main/UserService.java", "class UserService {}")
        myFixture.addFileToProject("src/main/Validators.java", "class UserValidator {} class OrderValidator {}")
        myFixture.addFileToProject("src/main/PaymentService.kt", "class PaymentService")

        // 2. ACT
        val result = service.listAllClasses()

        // 3. ASSERT
        assertTrue(result.containsKey("UserService"))
        assertTrue(result.containsKey("UserValidator"))
        assertTrue(result.containsKey("OrderValidator"))
        assertTrue(result.containsKey("PaymentService"))
        assertTrue(result["UserValidator"]!!.contains("Validators.java"))
    }

    fun testShouldReturnAllSourceFilePaths() {
        // --- TEST FLOW ---
        // 1. ARRANGE
        myFixture.addFileToProject("src/main/UserService.java", "class UserService {}")
        myFixture.addFileToProject("src/main/OrderRepo.java", "class OrderRepo {}")

        // 2. ACT
        val result = service.listAllSourceFiles()

        // 3. ASSERT
        assertTrue(result.any { it.contains("UserService.java") })
        assertTrue(result.any { it.contains("OrderRepo.java") })
        assertEquals(result.sorted(), result)
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
        val info = service.extractDetailedFileInfo(pointer)

        // 3. ASSERT
        assertNotNull("DetailedFileInfo should be extracted", info)
        assertEquals("Auth.java", info!!.fileName)

        assertEquals(2, info.imports.size)
        assertTrue(info.imports.any { it.contains("java.util.Date") })

        assertEquals("AuthenticationManager", info.enclosingClass?.name)
        assertEquals("loginUser", info.enclosingMethod?.name)
        assertTrue(info.enclosingMethod!!.body.contains("System.out.println"))
    }

    fun testShouldExtractFullContextWhenCaretIsInsideKotlinMethod() {
        // --- TEST FLOW ---
        // 1. ARRANGE
        val code =
            """
            import java.util.Date
            import java.util.List

            class AuthenticationManager {
                fun loginUser() {
                    // TODO: <caret>Add token validation here
                    println("Logging in")
                }
            }
            """.trimIndent()

        val psiFile = myFixture.configureByText("Auth.kt", code)
        val elementAtCaret = psiFile.findElementAt(myFixture.caretOffset)
        val pointer = SmartPointerManager.getInstance(project).createSmartPsiElementPointer(elementAtCaret!!)

        // 2. ACT
        val info = service.extractDetailedFileInfo(pointer)

        // 3. ASSERT
        assertNotNull("DetailedFileInfo should be extracted for Kotlin", info)
        assertEquals("Auth.kt", info!!.fileName)
        assertEquals("kotlin", info.language)
        assertEquals(2, info.imports.size)
        assertTrue(info.imports.any { it.contains("java.util.Date") })
        assertEquals("AuthenticationManager", info.enclosingClass?.name)
        assertEquals("loginUser", info.enclosingMethod?.name)
        assertTrue(info.enclosingMethod!!.body.contains("println"))
    }

    fun testShouldReturnKotlinFilesWhenListAllSourceFilesCalledWithDefaultExtensions() {
        // --- TEST FLOW ---
        // 1. ARRANGE
        myFixture.addFileToProject("src/UserService.kt", "class UserService")

        // 2. ACT
        val result = service.listAllSourceFiles()

        // 3. ASSERT
        assertTrue("Kotlin files should be included by default", result.any { it.contains("UserService.kt") })
    }
}
