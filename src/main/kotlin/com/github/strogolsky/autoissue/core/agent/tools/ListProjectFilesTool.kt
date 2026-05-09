package com.github.strogolsky.autoissue.core.agent.tools

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import com.github.strogolsky.autoissue.integration.code.CodeAnalysisService
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

@LLMDescription("Tools for listing all source files in the project.")
class ListProjectFilesTool(private val project: Project) : ToolSet {
    private val codeAnalysisService = project.service<CodeAnalysisService>()

    @Tool
    @LLMDescription(
        "Returns a flat list of all source file paths (Kotlin and Java), excluding build directories. " +
            "Use as a last resort when listAllClasses and searchSymbol both fail to find what you need.",
    )
    fun listProjectFiles(): ToolResponse = ProjectStructureResponse(files = codeAnalysisService.listAllSourceFiles())
}
