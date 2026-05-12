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
@LLMDescription("Lists all source files in the project.")
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
        "Returns a flat list of all source file paths (Kotlin and Java), excluding build directories. " +
            "Use as a last resort when listAllClasses and searchSymbol both fail to find what you need.",
    )
    fun listProjectFiles(): ToolResponse = ProjectStructureResponse(files = codeAnalysisService.listAllSourceFiles())
}
