package com.github.strogolsky.autoissue.ui.action

import com.intellij.openapi.project.DumbService
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.testFramework.replaceService
import io.mockk.every
import io.mockk.mockk

class TodoLineMarkerProviderTest : BasePlatformTestCase() {

    private lateinit var provider: TodoLineMarkerProvider

    override fun setUp() {
        super.setUp()
        provider = TodoLineMarkerProvider()
    }

    // UC-L1
    fun test_should_ReturnMarkerInfo_When_CommentContainsTodo() {
        // --- TEST FLOW ---
        // 1. ARRANGE
        val code = """
            class Test {
                // TODO:<caret> Need to fix authentication
                void auth() {}
            }
        """.trimIndent()
        val psiFile = myFixture.configureByText("Test.java", code)
        val elementAtCaret = psiFile.findElementAt(myFixture.caretOffset)

        // 2. ACT
        val markerInfo = provider.getLineMarkerInfo(elementAtCaret!!)

        // 3. ASSERT
        assertNotNull("Marker info should not be null", markerInfo)
        assertEquals("Create JIRA issue", markerInfo?.lineMarkerTooltip)
    }

    // UC-L2
    fun test_should_ReturnNull_When_ElementIsNotAComment() {
        // --- TEST FLOW ---
        // 1. ARRANGE
        val code = """
            class Test {
                String fakeTodo = "TODO:<caret> this is a string";
            }
        """.trimIndent()
        val psiFile = myFixture.configureByText("Test.java", code)
        val elementAtCaret = psiFile.findElementAt(myFixture.caretOffset)

        // 2. ACT
        val markerInfo = provider.getLineMarkerInfo(elementAtCaret!!)

        // 3. ASSERT
        assertNull(markerInfo)
    }

    // UC-L3
    fun test_should_ReturnNull_When_CommentDoesNotContainTodo() {
        // --- TEST FLOW ---
        // 1. ARRANGE
        val code = """
            class Test {
                // Just a regular comment explaining<caret> code
                void method() {}
            }
        """.trimIndent()
        val psiFile = myFixture.configureByText("Test.java", code)
        val elementAtCaret = psiFile.findElementAt(myFixture.caretOffset)

        // 2. ACT
        val markerInfo = provider.getLineMarkerInfo(elementAtCaret!!)

        // 3. ASSERT
        assertNull(markerInfo)
    }

    // UC-L4
    fun test_should_ReturnNull_When_CommentAlreadyHasJiraId() {
        // --- TEST FLOW ---
        // 1. ARRANGE
        val code = """
            class Test {
                // TODO [PROJ-123]: Fix<caret> database connection
                void connect() {}
            }
        """.trimIndent()
        val psiFile = myFixture.configureByText("Test.java", code)
        val elementAtCaret = psiFile.findElementAt(myFixture.caretOffset)

        // 2. ACT
        val markerInfo = provider.getLineMarkerInfo(elementAtCaret!!)

        // 3. ASSERT
        assertNull(markerInfo)
    }

    // UC-L5
    fun test_should_ReturnNull_When_DumbModeIsActive() {
        // --- TEST FLOW ---
        // 1. ARRANGE
        val code = """
            class Test {
                // TODO: Refactor<caret> this
            }
        """.trimIndent()
        val psiFile = myFixture.configureByText("Test.java", code)
        val elementAtCaret = psiFile.findElementAt(myFixture.caretOffset)

        val dumbServiceMock = mockk<DumbService>(relaxed = true)
        every { dumbServiceMock.isDumb } returns true
        project.replaceService(DumbService::class.java, dumbServiceMock, testRootDisposable)

        // 2. ACT
        val markerInfo = provider.getLineMarkerInfo(elementAtCaret!!)

        // 3. ASSERT
        assertNull(markerInfo)
    }
}