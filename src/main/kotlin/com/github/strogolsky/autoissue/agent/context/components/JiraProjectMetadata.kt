package com.github.strogolsky.autoissue.agent.context.components

import kotlinx.serialization.Serializable

@Serializable
data class JiraProjectMetadata(
    val projectKey: String,
    val projectId: String,
    val issueTypes: List<JiraIssueType>,
    val priorities: List<JiraField>,
    val components: List<JiraField>,
    val assignees: List<JiraField> = emptyList(),
    val labels: List<String> = emptyList(),
) : ContextComponent
