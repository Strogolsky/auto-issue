package com.github.strogolsky.autoissue.core.agent.tools

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import com.github.strogolsky.autoissue.integration.code.CodeAnalysisService
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

@LLMDescription("Tools for getting a complete map of all classes in the project.")
class ListAllClassesTool(private val project: Project) : ToolSet {
    private val codeAnalysisService = project.service<CodeAnalysisService>()

    @Tool
    @LLMDescription(
        "Returns a map of all class names to their source file paths in the project. " +
            "Use this first to locate any class regardless of its file name. " +
            "Then call readFileContent with the returned path.",
    )
    fun listAllClasses(): ToolResponse = ClassMapResponse(classes = codeAnalysisService.listAllClasses())
}
