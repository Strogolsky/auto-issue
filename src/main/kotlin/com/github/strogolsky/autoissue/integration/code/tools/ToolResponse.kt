package com.github.strogolsky.autoissue.integration.code.tools

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface ToolResponse

@Serializable
@SerialName("FileSearchResponse")
data class FileSearchResponse(
    val query: String,
    val matchedPaths: List<String>,
) : ToolResponse

@Serializable
@SerialName("FileContentResponse")
data class FileContentResponse(
    val filePath: String,
    val content: String,
) : ToolResponse

@Serializable
@SerialName("ToolErrorResponse")
data class ToolErrorResponse(
    val errorDetails: String,
) : ToolResponse
