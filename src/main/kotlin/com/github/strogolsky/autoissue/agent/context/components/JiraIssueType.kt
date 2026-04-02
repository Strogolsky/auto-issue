package com.github.strogolsky.autoissue.agent.context.components

import kotlinx.serialization.Serializable

@Serializable
data class JiraIssueType(val id: String, val name: String, val subtask: Boolean)
