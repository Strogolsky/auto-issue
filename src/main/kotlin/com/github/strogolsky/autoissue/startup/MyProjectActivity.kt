package com.github.strogolsky.autoissue.startup


import com.github.strogolsky.autoissue.context.TestEnvironment
import com.github.strogolsky.autoissue.services.JiraTaskGenerationService
import com.github.strogolsky.autoissue.settings.AgentConfigService
import com.github.strogolsky.autoissue.settings.AgentState
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import kotlin.coroutines.cancellation.CancellationException

class MyProjectActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        thisLogger().info("Initializing test through the Orchestrator...")

        val generationService = project.service<JiraTaskGenerationService>()
        val agentConfigService = project.service<AgentConfigService>()

        val testState = AgentState().apply {
            provider = "GOOGLE"
            modelName = "gemini-2.5-flash"
            systemPrompt = "You are an expert developer assistant. Analyze the context and generate a Jira task."
            strategyId = "prod-jira-strategy"
            temperature = 0.0
            maxIterations = 5
        }

        agentConfigService.updateSettings(testState, newKey = "")

        val testEnv = TestEnvironment(
            mockFileName = "Test.java",
            mockSelectedCode = "// TODO: Write instruction for printing Hello World in Java"
        )

        try {
            thisLogger().info("Executing generation service...")

            val result = generationService.generateTask("Write instruction for printing Hello World in Java", testEnv)

            thisLogger().info("Success! Parsed Output -> Title: '${result.title}', Description: '${result.description}'")
            println("Success! Parsed Output -> Title: '${result.title}', Description: '${result.description}'")

        } catch (e: CancellationException) {
            thisLogger().warn("Task was cancelled")
            throw e
        } catch (e: Exception) {
            thisLogger().error("Exception during task generation", e)
        }
    }


}
