package com.github.strogolsky.autoissue.core.agent.tools

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import com.github.strogolsky.autoissue.integration.code.CodeAnalysisService
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

@LLMDescription("Tools for searching symbols by name across the project.")
class SearchSymbolTool(private val project: Project) : ToolSet {
    private val codeAnalysisService = project.service<CodeAnalysisService>()

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
