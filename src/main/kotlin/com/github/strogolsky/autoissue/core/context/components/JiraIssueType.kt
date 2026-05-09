package com.github.strogolsky.autoissue.core.context.components

import kotlinx.serialization.Serializable

/**
 * JIRA issue type definition.
 *
 * Represents an issue type available in the JIRA project.
 * Examples: Bug, Story, Task, Improvement, Subtask
 *
 * @param id The JIRA issue type ID (used when creating issues)
 * @param name Human-readable name (shown to users)
 * @param subtask Whether this is a subtask type (affects parent issue requirements)
 */
@Serializable
data class JiraIssueType(val id: String, val name: String, val subtask: Boolean)
