package com.github.strogolsky.autoissue.plugin.config

/**
 * Summary information about a JIRA project.
 *
 * Used when displaying a list of available projects during configuration.
 * Contains just the essential information: key and name.
 *
 * @param key The JIRA project key (e.g., "PROJ")
 * @param name The project name (e.g., "My Project")
 */
data class JiraProjectSummary(val key: String, val name: String)
