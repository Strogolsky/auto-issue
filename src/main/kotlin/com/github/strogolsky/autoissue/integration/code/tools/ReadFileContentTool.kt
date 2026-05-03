package com.github.strogolsky.autoissue.integration.code.tools

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import com.github.strogolsky.autoissue.core.context.render.PromptRenderService
import com.github.strogolsky.autoissue.integration.code.CodeAnalysisService
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

@LLMDescription("Tools for reading source file content.")
class ReadFileContentTool(private val project: Project) : ToolSet {
    @Tool
    @LLMDescription(
        "Reads the entire content of a source file. " +
            "Requires the exact project-relative path returned by searchFiles. " +
            "Cannot read binary files (images, jars, compiled artifacts).",
    )
    fun readFileContent(
        @LLMDescription("Project-relative path to the file as returned by searchFiles, e.g. 'src/main/kotlin/com/app/Foo.kt'.")
        filePath: String,
    ): ToolResponse {
        val service = project.service<CodeAnalysisService>()
        if (service.isBinaryFile(filePath)) {
            return ToolErrorResponse(errorDetails = "Cannot read binary files.")
        }
        val content =
            service.getWholeFileContent(filePath)
                ?: return ToolErrorResponse(errorDetails = "File not found at path: $filePath")
        val maskedContent = project.service<PromptRenderService>().mask(content)
        return FileContentResponse(filePath = filePath, content = maskedContent)
    }
}
