package com.github.strogolsky.autoissue.integration.code.tools

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import com.github.strogolsky.autoissue.integration.code.CodeAnalysisService
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

@LLMDescription("Tools for locating files within the project by name.")
class SearchFilesTool(private val project: Project) : ToolSet {

    @Tool
    @LLMDescription(
        "Searches for files in the project by partial name match (case-insensitive). " +
            "Returns a list of project-relative paths. " +
            "Always call this first to get the exact path before reading a file.",
    )
    fun searchFiles(
        @LLMDescription("Part of the file name to search for, e.g. 'UserService' or 'OrderRepo'.")
        query: String,
    ): ToolResponse {
        val matches = project.service<CodeAnalysisService>().searchFilesByName(query)
        return if (matches.isEmpty()) {
            ToolErrorResponse(errorDetails = "No files found matching: $query")
        } else {
            FileSearchResponse(query = query, matchedPaths = matches)
        }
    }
}
