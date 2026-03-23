package com.github.strogolsky.autoissue.agent.output

import kotlinx.serialization.Serializable

@Serializable
data class TestTaskCandidate(
    val title: String,
    val description: String,
)
