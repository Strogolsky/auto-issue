package com.github.strogolsky.autoissue.agent.output

data class JiraIssueRequest(
    val title: String,
    val description: String,
    val labels: List<String>,
    val issueTypeId: String,
    val priorityId: String,
    val assigneeAccountId: String?,
    val parentIssueKey: String?,
    val dueDate: String?,
)
