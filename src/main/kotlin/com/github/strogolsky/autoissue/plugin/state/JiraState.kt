package com.github.strogolsky.autoissue.plugin.state

/**
 * Persistent JIRA configuration state.
 *
 * This class is persisted to disk by the IDE's state persistence system.
 * It stores non-sensitive JIRA settings (the API token is stored separately
 * in the password safe).
 *
 * @property baseUrl The JIRA instance URL
 * @property username JIRA username or email
 * @property defaultProjectKey The default project key for issue creation
 */
class JiraState {
    var baseUrl: String = ""
    var username: String = ""
    var defaultProjectKey: String = ""
}
