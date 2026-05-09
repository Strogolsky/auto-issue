package com.github.strogolsky.autoissue.plugin.config

/**
 * JIRA configuration for API access.
 *
 * Contains all credentials and settings needed to communicate with JIRA.
 * This is the effective config passed to API services for actual requests.
 *
 * @param baseUrl JIRA instance URL (e.g., https://company.atlassian.net)
 * @param username JIRA username or email for authentication
 * @param apiToken JIRA API token for HTTP Basic authentication
 * @param projectKey The default JIRA project key (e.g., "PROJ")
 */
data class JiraConfig(
    val baseUrl: String,
    val username: String,
    val apiToken: String,
    val projectKey: String,
)
