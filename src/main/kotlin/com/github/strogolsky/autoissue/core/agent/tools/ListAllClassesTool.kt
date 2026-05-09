package com.github.strogolsky.autoissue.core.agent.tools

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
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
@LLMDescription("Tools for getting a complete map of all classes in the project.")
class ListAllClassesTool(private val project: Project) : ToolSet {
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
        "Returns a map of all class names to their source file paths in the project. " +
            "Use this first to locate any class regardless of its file name. " +
            "Then call readFileContent with the returned path.",
    )
    fun listAllClasses(): ToolResponse = ClassMapResponse(classes = codeAnalysisService.listAllClasses())
}
