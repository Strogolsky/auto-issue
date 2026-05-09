package com.github.strogolsky.autoissue.core.output

/**
 * Complete request for creating a JIRA issue.
 *
 * This is the user-reviewed and potentially edited version of the AI-generated issue.
 * All fields are filled in (either from AI generation or user editing) and ready
 * to be sent to the JIRA REST API.
 *
 * @param title Issue summary (max 255 characters)
 * @param description Issue description in plain text (will be converted to ADF)
 * @param labels List of labels to apply
 * @param issueTypeId JIRA issue type ID (e.g., for Bug, Story, Task)
 * @param priorityId JIRA priority level ID (e.g., Highest, High, Medium, Low)
 * @param assigneeAccountId Optional JIRA user account ID for assignment
 * @param parentIssueKey Optional parent issue key (for subtasks)
 * @param dueDate Optional due date in YYYY-MM-DD format
 */
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
