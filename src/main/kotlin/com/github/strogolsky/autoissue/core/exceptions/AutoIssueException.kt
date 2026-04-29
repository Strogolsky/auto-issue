package com.github.strogolsky.autoissue.core.exceptions

abstract class AutoIssueException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

class ConfigurationException(message: String) : AutoIssueException(message)

class JiraApiException(message: String, cause: Throwable? = null) : AutoIssueException(message, cause)

class SourceCodeUpdateException(message: String, cause: Throwable? = null) : AutoIssueException(message, cause)