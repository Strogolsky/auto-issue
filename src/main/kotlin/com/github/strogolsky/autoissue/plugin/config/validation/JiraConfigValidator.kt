package com.github.strogolsky.autoissue.plugin.config.validation

import com.github.strogolsky.autoissue.plugin.config.JiraConfigService

/**
 * Validates JIRA configuration.
 *
 * Checks that JIRA URL and credentials are present.
 */
class JiraConfigValidator(private val jiraService: JiraConfigService) : ConfigValidator {
    override val name = "JIRA"
    override val configurableId = "com.github.strogolsky.autoissue.Jira"

    override fun isReady(): Boolean = jiraService.isReady()

    override fun getErrorMessage(): String = "JIRA Base URL or credentials are missing. Please configure JIRA settings."
}
