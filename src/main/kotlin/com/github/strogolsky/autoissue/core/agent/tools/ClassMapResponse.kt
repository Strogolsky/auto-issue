package com.github.strogolsky.autoissue.core.agent.tools

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("ClassMapResponse")
data class ClassMapResponse(
    val classes: Map<String, String>,
) : ToolResponse
