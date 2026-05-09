package com.github.strogolsky.autoissue.core.agent.tools

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("FileSearchResponse")
data class FileSearchResponse(
    val query: String,
    val matchedPaths: List<String>,
) : ToolResponse
