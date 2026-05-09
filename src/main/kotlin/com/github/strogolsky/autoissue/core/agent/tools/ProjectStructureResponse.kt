package com.github.strogolsky.autoissue.core.agent.tools

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("ProjectStructureResponse")
data class ProjectStructureResponse(
    val files: List<String>,
) : ToolResponse
