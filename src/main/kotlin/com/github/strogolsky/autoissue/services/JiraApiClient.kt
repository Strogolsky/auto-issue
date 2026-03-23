package com.github.strogolsky.autoissue.services

import com.github.strogolsky.autoissue.context.JiraProjectMetadata
import com.github.strogolsky.autoissue.settings.JiraConfigService
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class JiraApiClient(private val project: Project) {
    private val configService = project.service<JiraConfigService>()

    suspend fun testConnection(): Boolean {
        TODO()
    }

    suspend fun fetchProjectMetadata(projectKey: String): JiraProjectMetadata {
        TODO()
    }

    suspend fun createIssue(request: JiraIssueCreationRequest): String {
        TODO()
    }
}
