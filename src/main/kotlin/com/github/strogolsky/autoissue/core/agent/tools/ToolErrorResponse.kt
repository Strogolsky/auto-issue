package com.github.strogolsky.autoissue.core.agent.tools

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("ToolErrorResponse")
data class ToolErrorResponse(
    val errorDetails: String,
) : ToolResponse
