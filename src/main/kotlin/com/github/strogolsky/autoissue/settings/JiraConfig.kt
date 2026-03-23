package com.github.strogolsky.autoissue.settings

data class JiraConfig(
    val baseUrl: String,
    val apiToken: String,
    val defaultProjectKey: String
)