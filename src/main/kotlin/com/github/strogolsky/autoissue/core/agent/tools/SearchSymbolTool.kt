package com.github.strogolsky.autoissue.core.agent.tools

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import com.github.strogolsky.autoissue.integration.code.CodeAnalysisService
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

/**
 * Tool for AI agent to search for symbols (classes, functions) by name.
 *
 * Uses IntelliJ's symbol index for fast lookups. Useful when the exact class name
 * is unknown or when more refined search is needed.
 */
@LLMDescription("Tools for searching symbols by name across the project.")
class SearchSymbolTool(private val project: Project) : ToolSet {
    private val codeAnalysisService = project.service<CodeAnalysisService>()

    /**
     * Searches for a symbol by name or partial name.
     *
     * Performs case-insensitive partial name matching. Returns the full class name
     * and file path for each match.
     *
     * @param query Symbol name or partial name (e.g., 'UserValidator', 'Validator')
     * @return Search results with matching symbols and their locations, or error if none found
     */
    @Tool
    @LLMDescription(
        "Searches for classes and functions by partial name match using the IntelliJ symbol index. " +
            "Returns matching symbol names with their file paths. " +
            "Use this when listAllClasses does not have the symbol you need.",
    )
    fun searchSymbol(
        @LLMDescription("Name or partial name of the class or function to find, e.g. 'UserValidator'.")
        query: String,
    ): ToolResponse {
        val results = codeAnalysisService.searchSymbol(query)
        return if (results.isEmpty()) {
            ToolErrorResponse(errorDetails = "No symbols found matching: $query")
        } else {
            SymbolSearchResponse(query = query, results = results)
        }
    }
}
