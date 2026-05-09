package com.github.strogolsky.autoissue.core.exceptions

/**
 * Thrown when AI-driven issue generation fails.
 *
 * Examples:
 * - LLM API call fails (network, auth, rate limit, etc.)
 * - AI model returns invalid/unparseable response
 * - Context gathering fails
 * - Strategy or agent initialization fails
 *
 * This is a user-facing error that should be shown in the notification.
 * The message should explain what went wrong and any actionable steps.
 *
 * @param message Description of what went wrong during generation
 * @param cause The underlying exception that caused the failure
 */
class IssueGenerationException(message: String, cause: Throwable? = null) : AutoIssueException(message, cause)
