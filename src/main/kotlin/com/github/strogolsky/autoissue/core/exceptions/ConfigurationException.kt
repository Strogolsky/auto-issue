package com.github.strogolsky.autoissue.core.exceptions

/**
 * Thrown when plugin or JIRA configuration is invalid or incomplete.
 *
 * Examples:
 * - JIRA URL is missing or malformed
 * - JIRA credentials (username/token) are not set
 * - Selected JIRA project doesn't exist
 * - LLM provider is not configured
 *
 * This is a user-facing error that should be shown in the notification
 * with a clear message about what configuration is missing.
 *
 * @param message Description of what configuration is missing or invalid
 */
class ConfigurationException(message: String) : AutoIssueException(message)
