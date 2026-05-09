package com.github.strogolsky.autoissue.core.context.providers

import com.github.strogolsky.autoissue.core.context.ContextEnvironment
import com.github.strogolsky.autoissue.core.context.components.ContextComponent
import com.github.strogolsky.autoissue.integration.jira.JiraApiService
import com.github.strogolsky.autoissue.plugin.config.JiraConfigService
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service

/**
 * Context provider that fetches JIRA project metadata.
 *
 * Retrieves issue types, priorities, assignees, labels, and other JIRA configuration
 * from the configured project. This metadata is used by the AI agent to generate
 * valid issue requests that respect the project's schema and business rules.
 *
 * Returns null gracefully if no project is configured or if metadata fetching fails,
 * allowing issue generation to proceed (though with less validation).
 */
class JiraMetadataProvider : ContextComponentProvider {
    /**
     * Fetches metadata for the configured JIRA project.
     *
     * @param env The context environment (not used, metadata is project-specific)
     * @return JiraProjectMetadata with issue types, priorities, assignees, etc.,
     *         or null if no project is configured or metadata fetch fails
     */
    override suspend fun provide(env: ContextEnvironment): ContextComponent? {
        val jiraClient = ApplicationManager.getApplication().service<JiraApiService>()
        val configService = ApplicationManager.getApplication().service<JiraConfigService>()

        return try {
            val currentProjectKey = configService.state.defaultProjectKey
            if (currentProjectKey.isBlank()) return null

            jiraClient.getMetadata(currentProjectKey)
        } catch (e: Exception) {
            // Fail gracefully if metadata fetch fails - issue generation can proceed without it
            null
        }
    }
}
