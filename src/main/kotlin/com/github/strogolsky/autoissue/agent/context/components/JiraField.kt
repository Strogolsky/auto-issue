package com.github.strogolsky.autoissue.agent.context.components

import kotlinx.serialization.Serializable

@Serializable
data class JiraField(val id: String, val name: String)
