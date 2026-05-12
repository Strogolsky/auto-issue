package com.github.strogolsky.autoissue.core.agent.tools

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import com.github.strogolsky.autoissue.integration.code.CodeAnalysisService
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

/**
 * Tool for AI agent to list all project source files.
 *
 * Provides a complete list of source files in the project, excluding build
 * and generated directories. Use when other symbol lookup methods fail.
 */
@LLMDescription(
    "Provides a complete flat list of all source code files in the project. " +
        "Includes only .kt and .java files from actual source directories. " +
        "Use this when you need to browse all available files or when class-based lookup doesn't work.",
)
class ListProjectFilesTool(private val project: Project) : AgentTool {
    private val codeAnalysisService = project.service<CodeAnalysisService>()

    /**
     * Returns a list of all source files in the project.
     *
     * Lists all .java and .kotlin files, excluding:
     * - build/ directories
     * - generated/ directories
     * - .gradle/ directories
     * - out/ directories
     *
     * @return Project structure response with all source file paths
     */
    @Tool
    @LLMDescription(
        "Returns all project source files: {file path list}. " +
            "Includes: Kotlin (.kt) and Java (.java) files from src/, test/, and similar directories. " +
            "Excludes: build/, .gradle/, generated/, out/ directories and all compiled artifacts. " +
            "When to use: " +
            "1. Exploring project structure or modules " +
            "2. When listAllClasses doesn't have the specific class you're looking for " +
            "3. Finding related files in the same package/module",
    )
    fun listProjectFiles(): ToolResponse = ProjectStructureResponse(files = codeAnalysisService.listAllSourceFiles())
}
