package com.github.strogolsky.autoissue.core.exceptions

/**
 * Thrown when updating source code with the generated JIRA issue key fails.
 *
 * Examples:
 * - Target TODO line was deleted before update could occur
 * - Source file is no longer accessible
 * - TODO marker pattern not found
 * - File is read-only or locked
 *
 * Even if the JIRA issue was created successfully, the user needs to know
 * that the source code wasn't updated with the issue key.
 *
 * @param message Description of what went wrong during source code update
 * @param cause The underlying exception, if any
 */
class SourceCodeUpdateException(message: String, cause: Throwable? = null) : AutoIssueException(message, cause)
