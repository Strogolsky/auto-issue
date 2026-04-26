package com.github.strogolsky.autoissue.plugin.config

data class JiraConfig(
    val baseUrl: String,
    val username: String,
    val apiToken: String,
    val projectKey: String,
)
