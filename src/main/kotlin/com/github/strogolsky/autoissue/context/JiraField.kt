package com.github.strogolsky.autoissue.context

import kotlinx.serialization.Serializable

@Serializable
data class JiraField(val id: String, val name: String)
