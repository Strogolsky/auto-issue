package com.github.strogolsky.autoissue.settings

data class JiraConfig(
    val baseUrl: String,
    val username: String,
    val apiToken: String,
    val projectKey: String,
)
