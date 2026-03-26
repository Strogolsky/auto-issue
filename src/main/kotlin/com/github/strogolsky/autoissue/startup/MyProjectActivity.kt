package com.github.strogolsky.autoissue.startup

import com.github.strogolsky.autoissue.context.ContextRegistry
import com.github.strogolsky.autoissue.context.JiraMetadataProvider
import com.github.strogolsky.autoissue.context.TestEnvironment
import com.github.strogolsky.autoissue.services.JiraApiService
import com.github.strogolsky.autoissue.services.JiraTaskGenerationService
import com.github.strogolsky.autoissue.settings.AgentConfigService
import com.github.strogolsky.autoissue.settings.AgentState
import com.github.strogolsky.autoissue.settings.JiraConfigService
import com.github.strogolsky.autoissue.settings.JiraIntegrationState
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import kotlin.coroutines.cancellation.CancellationException

class MyProjectActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        thisLogger().info("Initializing test through the Orchestrator...")

        val registry = project.service<ContextRegistry>()
        registry.register(JiraMetadataProvider(project))

        val generationService = project.service<JiraTaskGenerationService>()
        val agentConfigService = project.service<AgentConfigService>()
        val jiraConfigService = project.service<JiraConfigService>()
        val jiraApiService = project.service<JiraApiService>()

        val testAgentState =
            AgentState().apply {
                provider = "GOOGLE"
                modelName = "gemini-2.5-flash"
                systemPrompt = "You are an expert developer assistant. Analyze the context and generate a Jira task."
                strategyId = "prod-jira-strategy"
                temperature = 0.0
                maxIterations = 5
            }
        agentConfigService.updateSettings(testAgentState, newKey = "")

        val testJiraState =
            JiraIntegrationState().apply {
                baseUrl = ""
                username = ""
                defaultProjectKey = "KAN"
            }

        val myJiraApiToken = ""
        jiraConfigService.updateSettings(testJiraState, newKey = myJiraApiToken)

        val testEnv =
            TestEnvironment(
                mockFileName = "Test.java",
                mockSelectedCode = "// TODO: Write instruction for printing Hello World in Java",
            )

        try {
            thisLogger().info("Executing generation service...")

            val result = generationService.generateTask("Write instruction for printing Hello World in Java", testEnv)

            val outputLog =
                """
                Success! Parsed Output:
                - Title: '${result.title}'
                - Issue Type ID: '${result.issueTypeId}'
                - Priority ID: '${result.priorityId}'
                - Components: ${result.componentIds}
                - Labels: ${result.labels}
                - Description: '${result.description}...'
                """.trimIndent()

            thisLogger().info(outputLog)
            println(outputLog)

            thisLogger().info("Sending request to Jira API...")
            val issueKey = jiraApiService.createIssue(result)

            thisLogger().info("Successfully created Jira issue: $issueKey")
            println("Successfully created Jira issue: $issueKey")
        } catch (e: CancellationException) {
            thisLogger().warn("Task was cancelled")
            throw e
        } catch (e: Exception) {
            thisLogger().error("Exception during task generation or Jira API call", e)
            e.printStackTrace()
        }
    }
}
