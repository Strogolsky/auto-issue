package com.github.strogolsky.autoissue.core.context.components

import kotlinx.serialization.Serializable

/**
 * JIRA field option (priority, assignee, component, etc.).
 *
 * Generic structure for representing JIRA field values that have
 * both an ID and human-readable name.
 *
 * Examples:
 * - Priority: id="10001", name="Highest"
 * - Assignee: id="5a123abc456def789", name="John Doe"
 * - Component: id="10100", name="Backend"
 *
 * @param id The JIRA field value ID (used when creating/updating issues)
 * @param name Human-readable name (shown in UI)
 */
@Serializable
data class JiraField(val id: String, val name: String)
