package com.github.strogolsky.autoissue.agent.context.providers

import com.github.strogolsky.autoissue.agent.context.ContextEnvironment
import com.github.strogolsky.autoissue.agent.context.components.ContextComponent
import com.github.strogolsky.autoissue.services.JiraApiService
import com.github.strogolsky.autoissue.services.JiraConfigService
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

class JiraMetadataProvider(private val project: Project) : ContextComponentProvider {
    private val jiraClient = project.service<JiraApiService>()
    private val configService = project.service<JiraConfigService>()

    override suspend fun provide(env: ContextEnvironment): ContextComponent? {
        return try {
            val currentProjectKey = configService.state.defaultProjectKey
            if (currentProjectKey.isBlank()) return null

            jiraClient.getMetadata(currentProjectKey)
        } catch (e: Exception) {
            // TODO add logs
            null
        }
    }
}
