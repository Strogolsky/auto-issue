package com.github.strogolsky.autoissue.core.agent

import ai.koog.agents.core.agent.AIAgent
import com.github.strogolsky.autoissue.core.exceptions.IssueGenerationException
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest

class KoogAgentAdapterTest : BasePlatformTestCase() {
    private lateinit var agent: AIAgent<String, String>
    private lateinit var adapter: KoogAgentAdapter<String, String>

    override fun setUp() {
        super.setUp()
        agent = mockk()
        adapter = KoogAgentAdapter(agent)
    }

    fun testShouldReturnResultWhenAgentSucceeds() =
        runTest {
            // --- TEST FLOW ---
            // 1. ARRANGE
            coEvery { agent.run("input") } returns "result"

            // 2. ACT
            val result = adapter.generate("input")

            // 3. ASSERT
            assertEquals("result", result)
        }

    fun testShouldThrowIssueGenerationExceptionWhenAgentFails() =
        runTest {
            // --- TEST FLOW ---
            // 1. ARRANGE
            val cause = RuntimeException("API timeout")
            coEvery { agent.run(any()) } throws cause

            // 2. ACT & 3. ASSERT
            try {
                adapter.generate("input")
                fail("Expected IssueGenerationException to be thrown")
            } catch (e: IssueGenerationException) {
                assertSame(cause, e.cause)
            }
        }
}
