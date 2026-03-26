package com.github.strogolsky.autoissue.context

import kotlinx.serialization.Serializable

@Serializable
data class JiraProjectMetadata(
    val projectKey: String,
    val projectId: String,
    val issueTypes: List<JiraIssueType>,
    val priorities: List<JiraField>,
    val components: List<JiraField>,
) : ContextComponent
