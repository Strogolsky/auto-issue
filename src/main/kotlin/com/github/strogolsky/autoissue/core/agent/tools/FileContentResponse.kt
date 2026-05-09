package com.github.strogolsky.autoissue.core.agent.tools

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response from ReadFileContentTool containing the content of a source file.
 *
 * Provides the AI agent with the full content of a source file for code analysis and understanding.
 * Content is automatically masked to remove sensitive information (API keys, credentials, etc.)
 * before being sent to the AI. Large files may be truncated with a note indicating the size limit.
 *
 * @param filePath Project-relative path to the file (e.g., "src/main/kotlin/com/app/Service.kt")
 * @param content The file's source code content (masked and potentially truncated)
 */
@Serializable
@SerialName("FileContentResponse")
data class FileContentResponse(
    val filePath: String,
    val content: String,
) : ToolResponse
