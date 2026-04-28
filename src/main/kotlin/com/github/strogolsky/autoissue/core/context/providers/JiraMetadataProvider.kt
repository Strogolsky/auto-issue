package com.github.strogolsky.autoissue.core.context.providers

import com.github.strogolsky.autoissue.core.context.ContextEnvironment
import com.github.strogolsky.autoissue.core.context.components.ContextComponent
import com.github.strogolsky.autoissue.integration.jira.JiraApiService
import com.github.strogolsky.autoissue.plugin.config.JiraConfigService
import com.intellij.openapi.components.service

class JiraMetadataProvider : ContextComponentProvider {
    override suspend fun provide(env: ContextEnvironment): ContextComponent? {
        val jiraClient = env.project.service<JiraApiService>()
        val configService = env.project.service<JiraConfigService>()

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
