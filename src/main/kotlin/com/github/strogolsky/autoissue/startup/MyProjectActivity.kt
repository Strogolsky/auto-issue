package com.github.strogolsky.autoissue.startup

import ai.koog.agents.core.agent.AIAgent
import ai.koog.prompt.executor.clients.google.GoogleModels
import ai.koog.prompt.executor.llms.all.simpleGoogleAIExecutor
import com.github.strogolsky.autoissue.services.agent.KoogIssueGenerationAgent
import com.github.strogolsky.autoissue.services.agent.input.IssueGenerationInput
import com.github.strogolsky.autoissue.services.agent.input.TaskInstruction
import com.github.strogolsky.autoissue.services.agent.input.TestRenderer
import com.github.strogolsky.autoissue.services.agent.output.TestTaskCandidate
import com.github.strogolsky.autoissue.services.agent.strategy.TestIssueStrategyFactory
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import kotlin.coroutines.cancellation.CancellationException

class MyProjectActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        println("Start Activity")
        runTestAgent()
    }

    private suspend fun runTestAgent() {
        thisLogger().info("Initializing test agent components...")

        val renderer = TestRenderer()
        val instruction = TaskInstruction("Write instruction for printing Hello World in Java")

        val input =
            IssueGenerationInput(
                instruction = instruction,
                contextComponents = emptyList(),
                renderer = renderer,
            )

        val apiKey = ""
        val systemPrompt =
            "\"You are a developer assistant. Return ONLY a valid JSON object with 'title' and" +
                    " 'description' fields based on the user instruction. Do not include markdown formatting.\""

        val strategy = TestIssueStrategyFactory().createStrategy()

        @OptIn(kotlin.time.ExperimentalTime::class)
        val rawKoogAgent =
            AIAgent(
                promptExecutor = simpleGoogleAIExecutor(apiKey),
                llmModel = GoogleModels.Gemini2_5Flash,
                strategy = strategy,
                systemPrompt = systemPrompt,
                temperature = 0.0,
                maxIterations = 5,
            )

        val agentWrapper = KoogIssueGenerationAgent(rawKoogAgent)

        try {
            thisLogger().info("Executing agent with prompt:\n${input.toPrompt()}")

            val result: TestTaskCandidate? = agentWrapper.generate(input)

            if (result != null) {
                thisLogger().info("Success! Parsed Output -> Title: '${result.title}', Description: '${result.description}'")
                println("Success! Parsed Output -> Title: '${result.title}', Description: '${result.description}'")
            } else {
                thisLogger().warn("Agent returned null (Execution failed or parsing error).")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            thisLogger().error("Exception during agent execution", e)
        }
    }
}
