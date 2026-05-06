package com.github.strogolsky.autoissue.integration.code

import com.github.strogolsky.autoissue.core.exceptions.SourceCodeUpdateException
import com.intellij.openapi.components.service
import com.intellij.psi.SmartPointerManager
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class TodoUpdaterServiceTest : BasePlatformTestCase() {
    private lateinit var service: TodoUpdaterService

    override fun setUp() {
        super.setUp()
        Dispatchers.setMain(UnconfinedTestDispatcher())
        service = project.service<TodoUpdaterService>()
    }

    override fun tearDown() {
        Dispatchers.resetMain()
        super.tearDown()
    }

    // UC-C6
    fun testShouldInjectKeyWhenSingleLineTodoExists() {
        // --- TEST FLOW ---
        // 1. ARRANGE
        val code =
            """
            class Calc {
                void calculate() {
                    // TODO<caret> fix calculation bug
                }
            }
            """.trimIndent()
        val psiFile = myFixture.configureByText("Calc.java", code)
        val elementAtCaret = psiFile.findElementAt(myFixture.caretOffset)
        val pointer = SmartPointerManager.getInstance(project).createSmartPsiElementPointer(elementAtCaret!!)

        // 2. ACT
        runBlocking {
            service.appendKeyToCode(pointer, "PROJ-123")
        }

        // 3. ASSERT
        val updatedText = myFixture.file.text
        assertTrue("Key should be injected", updatedText.contains("// TODO [PROJ-123]"))
    }

    // UC-C7
    fun testShouldInjectKeyAndKeepSyntaxWhenMultilineTodoExists() {
        // --- TEST FLOW ---
        // 1. ARRANGE
        val code =
            """
            class Init {
                void start() {
                    /* TODO implement database connection <caret>*/
                }
            }
            """.trimIndent()
        val psiFile = myFixture.configureByText("Init.java", code)
        val elementAtCaret = psiFile.findElementAt(myFixture.caretOffset)
        val pointer = SmartPointerManager.getInstance(project).createSmartPsiElementPointer(elementAtCaret!!)

        // 2. ACT
        runBlocking {
            service.appendKeyToCode(pointer, "PROJ-999")
        }

        // 3. ASSERT
        val updatedText = myFixture.file.text
        assertTrue("Closing tag should be preserved", updatedText.contains("/* TODO [PROJ-999] */"))
    }

    // UC-C8
    fun testShouldThrowExceptionWhenTodoMarkerIsMissing() {
        // --- TEST FLOW ---
        // 1. ARRANGE
        val code =
            """
            class Api {
                void fetch() {
                    // FIXME:<caret> broken api
                }
            }
            """.trimIndent()
        val psiFile = myFixture.configureByText("Api.java", code)
        val elementAtCaret = psiFile.findElementAt(myFixture.caretOffset)
        val pointer = SmartPointerManager.getInstance(project).createSmartPsiElementPointer(elementAtCaret!!)

        // 2. ACT & ASSERT
        try {
            runBlocking {
                service.appendKeyToCode(pointer, "PROJ-123")
            }
            fail("Expected SourceCodeUpdateException to be thrown")
        } catch (e: SourceCodeUpdateException) {
            assertTrue(e.message!!.contains("'TODO' marker not found"))
        }
    }
}
