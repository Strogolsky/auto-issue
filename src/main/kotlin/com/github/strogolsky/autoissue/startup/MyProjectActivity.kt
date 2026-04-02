package com.github.strogolsky.autoissue.startup

import com.github.strogolsky.autoissue.agent.context.ContextRegistry
import com.github.strogolsky.autoissue.agent.context.providers.JiraMetadataProvider
import com.github.strogolsky.autoissue.agent.context.providers.FileContextComponentProvider
import com.github.strogolsky.autoissue.settings.AgentConfigService
import com.github.strogolsky.autoissue.settings.AgentState
import com.github.strogolsky.autoissue.services.JiraConfigService
import com.github.strogolsky.autoissue.settings.JiraIntegrationState
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

class MyProjectActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        thisLogger().info("Initializing test through the Orchestrator...")


        val geminiApiKey = System.getProperty("gemini.api.key", "")
        val jiraUrl = System.getProperty("jira.base.url", "")
        val jiraUser = System.getProperty("jira.username", "")
        val jiraToken = System.getProperty("jira.api.token", "")
        val jiraProjectKey = System.getProperty("jira.project-key", "")

        val registry = project.service<ContextRegistry>()
        registry.register(JiraMetadataProvider(project))
        registry.register(FileContextComponentProvider())

        val agentConfigService = project.service<AgentConfigService>()
        val jiraConfigService = project.service<JiraConfigService>()

        val testAgentState =
            AgentState().apply {
                provider = "GOOGLE"
                modelName = "gemini-2.5-flash"
                systemPrompt = "You are an expert developer assistant. Analyze the context and generate a Jira task."
                strategyId = "prod-jira-strategy"
                temperature = 0.0
                maxIterations = 5
            }

        val testJiraState =
            JiraIntegrationState().apply {
                baseUrl = jiraUrl
                username = jiraUser
                defaultProjectKey = jiraProjectKey
            }

        jiraConfigService.updateSettings(testJiraState, newKey = jiraToken)
        agentConfigService.updateSettings(testAgentState, newKey = geminiApiKey)

    }
}
