package com.github.strogolsky.autoissue.core.agent.tools

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import com.github.strogolsky.autoissue.integration.code.CodeAnalysisService
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

/**
 * Tool for AI agent to discover all classes in the project.
 *
 * This tool provides the AI agent with a complete map of class names to their
 * file locations, allowing it to locate and read relevant code context.
 *
 * Usage by AI:
 * 1. Call listAllClasses() to get a map of class names → file paths
 * 2. Use ReadFileContentTool to read the content of relevant files
 */
@LLMDescription(
    "Returns a comprehensive map of all class names to their source file paths. " +
        "Use this tool when you need to find where any class is defined in the project. " +
        "This is the primary way to locate classes by name before examining their content.",
)
class ListAllClassesTool(private val project: Project) : AgentTool {
    private val codeAnalysisService = project.service<CodeAnalysisService>()

    /**
     * Returns a map of all class names to their source file paths.
     *
     * This helps the AI agent find classes in the codebase. After getting the class map,
     * the agent can call readFileContent with a file path to examine the code.
     *
     * @return A map response containing all classes and their locations
     */
    @Tool
    @LLMDescription(
        "Retrieves a complete name-to-path mapping of all classes in the project. " +
            "Returns: { className -> filePath } pairs for every class found. " +
            "Examples: 'JiraIssueGenerationService' -> 'src/main/kotlin/.../JiraIssueGenerationService.kt'. " +
            "After finding a class, read file to examine its implementation.",
    )
    fun listAllClasses(): ToolResponse = ClassMapResponse(classes = codeAnalysisService.listAllClasses())
}
