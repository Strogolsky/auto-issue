package com.github.strogolsky.autoissue.core.exceptions

/**
 * Thrown when JIRA REST API communication fails.
 *
 * Examples:
 * - Network connection failure
 * - Invalid JIRA URL or credentials
 * - HTTP 4xx/5xx error from JIRA
 * - API response parsing fails
 * - JIRA server is unreachable
 *
 * This is a user-facing error that should guide the user to check
 * JIRA configuration (URL, credentials) and network connectivity.
 *
 * @param message Description of the API failure
 * @param cause The underlying network or parsing exception
 */
class JiraApiException(message: String, cause: Throwable? = null) : AutoIssueException(message, cause)
