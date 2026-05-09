package com.github.strogolsky.autoissue.core.context.render

import com.github.strogolsky.autoissue.core.masking.ContentMasker
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class PromptRenderServiceTest {
    private val renderer = mockk<PromptRenderer>()
    private val masker = mockk<ContentMasker>()
    private val service = PromptRenderService()

    @Test
    fun testShouldThrowWhenBuildPromptCalledBeforeInitialize() {
        // --- TEST FLOW ---
        // 1. ARRANGE: service not initialized

        // 2. ACT & 3. ASSERT
        try {
            service.buildPrompt {}
            fail("Expected IllegalStateException")
        } catch (e: IllegalStateException) {
            // expected
        }
    }

    @Test
    fun testShouldDelegateBuildPromptToRendererAndApplyMasking() {
        // --- TEST FLOW ---
        // 1. ARRANGE
        every { renderer.buildPrompt(any()) } returns "raw"
        every { masker.mask("raw") } returns "masked"
        service.initialize(renderer, masker)

        // 2. ACT
        val result = service.buildPrompt {}

        // 3. ASSERT
        assertEquals("masked", result)
        verify { masker.mask("raw") }
    }

    @Test
    fun testShouldDelegateMaskToMasker() {
        // --- TEST FLOW ---
        // 1. ARRANGE
        every { masker.mask("secret text") } returns "***"
        service.initialize(renderer, masker)

        // 2. ACT
        val result = service.mask("secret text")

        // 3. ASSERT
        assertEquals("***", result)
    }
}
