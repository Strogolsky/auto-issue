package com.github.strogolsky.autoissue.core.context.providers

import com.github.strogolsky.autoissue.core.context.ContextEnvironment
import com.github.strogolsky.autoissue.core.context.components.FileContextComponent
import com.github.strogolsky.autoissue.integration.code.ClassContext
import com.github.strogolsky.autoissue.integration.code.CodeAnalysisService
import com.github.strogolsky.autoissue.integration.code.DetailedFileContext
import com.github.strogolsky.autoissue.integration.code.MethodContext
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.SmartPsiElementPointer
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class FileContextComponentProviderTest {
    private val project = mockk<Project>()
    private val pointer = mockk<SmartPsiElementPointer<PsiElement>>()
    private val env = mockk<ContextEnvironment>()
    private val codeAnalysisService = mockk<CodeAnalysisService>()

    private lateinit var provider: FileContextComponentProvider

    @Before
    fun setUp() {
        every { env.project } returns project
        every { env.pointer } returns pointer
        every { project.getService(CodeAnalysisService::class.java) } returns codeAnalysisService

        provider = FileContextComponentProvider()
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun shouldReturnComponentWhenContextExtractedSuccessfully() =
        runBlocking {
            // --- TEST FLOW ---
            // 1. ARRANGE
            val classContext =
                mockk<ClassContext> {
                    every { name } returns "UserService"
                    every { fields } returns listOf("private val repository")
                }
            val methodContext =
                mockk<MethodContext> {
                    every { signature } returns "fun getUser()"
                    every { body } returns "return repository.getUser()"
                }
            val detailedContext =
                mockk<DetailedFileContext> {
                    every { fileName } returns "UserService.kt"
                    every { language } returns "Kotlin"
                    every { imports } returns listOf("import java.util.*")
                    every { enclosingClass } returns classContext
                    every { enclosingMethod } returns methodContext
                }

            every { codeAnalysisService.extractDetailedContext(pointer) } returns detailedContext

            // 2. ACT
            val result = provider.provide(env) as? FileContextComponent

            // 3. ASSERT
            assertEquals("UserService.kt", result?.fileName)
            assertEquals("Kotlin", result?.language)
            assertEquals(1, result?.imports?.size)
            assertEquals("UserService", result?.className)
            assertEquals(1, result?.classFields?.size)
            assertEquals("fun getUser()", result?.methodSignature)
            assertEquals("return repository.getUser()", result?.methodBody)
        }

    @Test
    fun shouldReturnPartialComponentWhenClassAndMethodAreNull() =
        runBlocking {
            // --- TEST FLOW ---
            // 1. ARRANGE
            val detailedContext =
                mockk<DetailedFileContext> {
                    every { fileName } returns "script.kts"
                    every { language } returns "Kotlin"
                    every { imports } returns emptyList()
                    every { enclosingClass } returns null
                    every { enclosingMethod } returns null
                    every { surroundingText } returns "println(\"Hello\")"
                }

            every { codeAnalysisService.extractDetailedContext(pointer) } returns detailedContext

            // 2. ACT
            val result = provider.provide(env) as? FileContextComponent

            // 3. ASSERT
            assertEquals("script.kts", result?.fileName)
            assertNull(result?.className)
            assertNull(result?.methodSignature)
            assertEquals("println(\"Hello\")", result?.methodBody) // Falls back to surroundingText
        }

    @Test
    fun shouldReturnNullWhenContextExtractionFails() =
        runBlocking {
            // --- TEST FLOW ---
            // 1. ARRANGE
            every { codeAnalysisService.extractDetailedContext(pointer) } returns null

            // 2. ACT
            val result = provider.provide(env)

            // 3. ASSERT
            assertNull(result)
        }
}
