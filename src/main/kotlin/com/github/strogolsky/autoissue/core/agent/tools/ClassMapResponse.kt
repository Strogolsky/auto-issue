package com.github.strogolsky.autoissue.core.agent.tools

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response from ListAllClassesTool containing a map of classes.
 *
 * Maps class names to their file paths so the AI can locate and read
 * relevant source code.
 *
 * @param classes Map of className → file path (e.g., "UserService" → "src/main/kotlin/com/app/UserService.kt")
 */
@Serializable
@SerialName("ClassMapResponse")
data class ClassMapResponse(
    val classes: Map<String, String>,
) : ToolResponse
