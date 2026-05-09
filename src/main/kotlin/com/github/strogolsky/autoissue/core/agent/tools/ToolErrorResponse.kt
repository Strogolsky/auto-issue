package com.github.strogolsky.autoissue.core.agent.tools

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Error response returned by any tool when an operation fails.
 *
 * Indicates that a tool execution encountered an error condition and could not complete
 * the requested operation. The error details are provided to the AI agent for understanding
 * what went wrong and for deciding how to proceed (retry with different parameters, try
 * alternative tools, or inform the user).
 *
 * Examples of error conditions:
 * - "No symbols found matching: UserService"
 * - "File not found at path: src/main/kotlin/com/app/Missing.kt"
 * - "Cannot read binary files."
 * - "Symbol search index not available"
 *
 * @param errorDetails Human-readable error message explaining what went wrong and why
 */
@Serializable
@SerialName("ToolErrorResponse")
data class ToolErrorResponse(
    val errorDetails: String,
) : ToolResponse
