package com.github.strogolsky.autoissue.core.exceptions

/**
 * Base exception for all AutoIssue plugin errors.
 *
 * Subclasses represent different error categories:
 * - ConfigurationException: Issues with plugin or JIRA configuration
 * - IssueGenerationException: Problems during AI-driven issue generation
 * - JiraApiException: JIRA REST API communication failures
 * - SourceCodeUpdateException: Failures when updating source code
 *
 * These are user-facing exceptions that should result in helpful notification messages.
 *
 * @param message Human-readable error description for the user
 * @param cause The underlying exception that caused this error
 */
abstract class AutoIssueException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
