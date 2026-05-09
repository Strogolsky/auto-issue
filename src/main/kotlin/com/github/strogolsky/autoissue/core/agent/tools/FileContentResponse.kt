package com.github.strogolsky.autoissue.core.agent.tools

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("FileContentResponse")
data class FileContentResponse(
    val filePath: String,
    val content: String,
) : ToolResponse
