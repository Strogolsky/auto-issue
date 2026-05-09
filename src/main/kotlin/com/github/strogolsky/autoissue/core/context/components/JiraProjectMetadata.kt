package com.github.strogolsky.autoissue.core.context.components

import kotlinx.serialization.Serializable

/**
 * JIRA project metadata context.
 *
 * Contains all metadata about the JIRA project needed for issue creation:
 * - Available issue types (Bug, Story, Task, etc.)
 * - Priority levels (Highest, High, Medium, Low, Lowest)
 * - Project components
 * - Available assignees
 * - Available labels
 *
 * This context is provided to the AI agent so it can:
 * - Select an appropriate issue type
 * - Suggest relevant labels
 * - Recommend a priority level
 * - Optionally assign to team members
 *
 * @param projectKey The JIRA project key (e.g., "PROJ")
 * @param projectId The JIRA project ID
 * @param issueTypes List of available issue types in the project
 * @param priorities List of available priority levels
 * @param components List of project components
 * @param assignees List of users who can be assigned issues
 * @param labels List of available labels in the project
 */
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
