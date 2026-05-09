package com.github.strogolsky.autoissue.core.exceptions

abstract class AutoIssueException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
