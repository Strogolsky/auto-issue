package com.github.strogolsky.autoissue.core.agent.tools

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response from ListProjectFilesTool containing all source files in the project.
 *
 * Provides the AI agent with a complete flat list of source file paths as a fallback
 * when other symbol lookup methods (listAllClasses, searchSymbol) are insufficient.
 * Excludes build directories, generated code, and compiled artifacts.
 *
 * @param files List of all project source file paths relative to project root
 *              (e.g., ["src/main/kotlin/com/app/Service.kt", "src/main/kotlin/com/app/util/Helper.kt"])
 */
@Serializable
@SerialName("ProjectStructureResponse")
data class ProjectStructureResponse(
    val files: List<String>,
) : ToolResponse
