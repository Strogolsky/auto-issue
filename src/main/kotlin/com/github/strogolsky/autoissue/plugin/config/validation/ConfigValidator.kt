package com.github.strogolsky.autoissue.plugin.config.validation

/**
 * Validates a specific configuration subsystem.
 *
 * Implementations check specific configuration areas (JIRA, LLM) and provide
 * user-friendly error messages when configuration is incomplete.
 */
interface ConfigValidator {
    /** Human-readable name of this configuration area */
    val name: String

    /** IDE configurable ID for opening settings dialog */
    val configurableId: String

    /**
     * Checks if this configuration is ready for use.
     *
     * @return true if all required settings are present
     */
    fun isReady(): Boolean

    /**
     * Gets a user-friendly error message describing what's missing.
     *
     * @return Error message to show to the user
     */
    fun getErrorMessage(): String
}
