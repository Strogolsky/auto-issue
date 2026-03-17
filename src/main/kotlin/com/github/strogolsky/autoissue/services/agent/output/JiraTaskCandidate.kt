package com.github.strogolsky.autoissue.services.agent.output

import kotlinx.serialization.Serializable

@Serializable
data class JiraTaskCandidate(
    override val title: String,
    override val description: String,
    val issueType: String,
    val components: List<String> = emptyList(),
    val labels: List<String> = emptyList(),
    val epicLink: String? = null,
) : TaskCandidate
